package net.floodlightcontroller.statistics;

import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.ListenableFuture;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.NodeFlowTuple;
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.statistics.web.SwitchStatisticsWebRoutable;
import net.floodlightcontroller.storage.CompoundPredicate;
import net.floodlightcontroller.storage.IResultSet;
import net.floodlightcontroller.storage.IStorageSourceService;
import net.floodlightcontroller.storage.OperatorPredicate;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.ver13.OFMeterSerializerVer13;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFGroup;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Thread.State;
import java.sql.ResultSet;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class StatisticsCollector implements IFloodlightModule, IStatisticsService {
	private static final Logger log = LoggerFactory.getLogger(StatisticsCollector.class);

	private static IOFSwitchService switchService;
	private static IThreadPoolService threadPoolService;
	private static IRestApiService restApiService;
	private static IStorageSourceService storageService;

	private static boolean isEnabled = false;
	private static boolean debug = true;
	
	private static int portStatsInterval = 2; /* could be set by REST API, so not final */
	private static int flowStatsInterval = 2; 
	private static ScheduledFuture<?> portStatsCollector;
	private static ScheduledFuture<?> flowStatsCollector;

	private static final long BITS_PER_BYTE = 8;
	private static final long MILLIS_PER_SEC = 1000;
	
	private static final String INTERVAL_PORT_STATS_STR = "collectionIntervalPortStatsSeconds";
	private static final String ENABLED_STR = "enable";
	
	private static final HashMap<NodePortTuple, SwitchPortBandwidth> portStats = new HashMap<NodePortTuple, SwitchPortBandwidth>();
	
	private static final HashMap<NodeFlowTuple, FlowBandwidth> flowStats = new HashMap<NodeFlowTuple, FlowBandwidth>();
	
	private static final HashMap<NodePortTuple, SwitchPortBandwidth> tentativePortStats = new HashMap<NodePortTuple, SwitchPortBandwidth>();
	
	
	//variables for creating/updating the FLowTables
	private static final String TABLE_NAME = "FlowStatistics";
    private static final String DPID = "dpid";
    private static final String ETHSRC = "ethsrc";
    private static final String ETHDST = "ethdst";
    private static final String ETHTYPE = "ethtype";
    private static final String DURATION = "duration";
    private static final String BYTECOUNT = "bytecount";
    private static final String SPEED = "speed";
    private static final String INPORT = "port";
    private static final String COLUMNS[] = {DPID, ETHSRC, ETHDST, ETHTYPE, DURATION, BYTECOUNT, SPEED, INPORT};
    private static final long REQUEST_TIMEOUT_MSEC = 1000;
    
    
    
    private static final String Port_TABLE_NAME = "PortStatistics";
    private static final String Port_DPID = "dpid";
    private static final String Port_SPEED = "speed";
    private static final String Port_ID = "portid";
    private static final String Port_COLUMNS[] = {Port_DPID,Port_SPEED,Port_ID};
    

  	
	/**
	 * Run periodically to collect all port statistics. This only collects
	 * bandwidth stats right now, but it could be expanded to record other
	 * information as well. The difference between the most recent and the
	 * current RX/TX bytes is used to determine the "elapsed" bytes. A 
	 * timestamp is saved each time stats results are saved to compute the
	 * bits per second over the elapsed time. There isn't a better way to
	 * compute the precise bandwidth unless the switch were to include a
	 * timestamp in the stats reply message, which would be nice but isn't
	 * likely to happen. It would be even better if the switch recorded 
	 * bandwidth and reported bandwidth directly.
	 * 
	 * Stats are not reported unless at least two iterations have occurred
	 * for a single switch's reply. This must happen to compare the byte 
	 * counts and to get an elapsed time.
	 * 
	 * @author Ryan Izard, ryan.izard@bigswitch.com, rizard@g.clemson.edu
	 *
	 */
	protected class PortStatsCollector implements Runnable {

		@Override
		public void run() {
			Map<DatapathId, List<OFStatsReply>> replies = getSwitchStatistics(switchService.getAllSwitchDpids(), OFStatsType.PORT);
			for (Entry<DatapathId, List<OFStatsReply>> e : replies.entrySet()) {
				for (OFStatsReply r : e.getValue()) {
					OFPortStatsReply psr = (OFPortStatsReply) r;
					for (OFPortStatsEntry pse : psr.getEntries()) {
						NodePortTuple npt = new NodePortTuple(e.getKey(), pse.getPortNo());
						SwitchPortBandwidth spb;
						if (portStats.containsKey(npt) || tentativePortStats.containsKey(npt)) {
							if (portStats.containsKey(npt)) { /* update */
								spb = portStats.get(npt);
							} else if (tentativePortStats.containsKey(npt)) { /* finish */
								spb = tentativePortStats.get(npt);
								tentativePortStats.remove(npt);
							} else {
								log.error("Inconsistent state between tentative and official port stats lists.");
								return;
							}

							/* Get counted bytes over the elapsed period. Check for counter overflow. */
							U64 rxBytesCounted;
							U64 txBytesCounted;
							if (spb.getPriorByteValueRx().compareTo(pse.getRxBytes()) > 0) { /* overflow */
								U64 upper = U64.NO_MASK.subtract(spb.getPriorByteValueRx());
								U64 lower = pse.getRxBytes();
								rxBytesCounted = upper.add(lower);
							} else {
								rxBytesCounted = pse.getRxBytes().subtract(spb.getPriorByteValueRx());
							}
							if (spb.getPriorByteValueTx().compareTo(pse.getTxBytes()) > 0) { /* overflow */
								U64 upper = U64.NO_MASK.subtract(spb.getPriorByteValueTx());
								U64 lower = pse.getTxBytes();
								txBytesCounted = upper.add(lower);
							} else {
								txBytesCounted = pse.getTxBytes().subtract(spb.getPriorByteValueTx());
							}
							long speed = getSpeed(npt);
							long timeDifSec = (System.currentTimeMillis() - spb.getUpdateTime()) / MILLIS_PER_SEC;
							portStats.put(npt, SwitchPortBandwidth.of(npt.getNodeId(), npt.getPortId(), 
									U64.ofRaw(speed),
									U64.ofRaw((rxBytesCounted.getValue() * BITS_PER_BYTE) / timeDifSec), 
									U64.ofRaw((txBytesCounted.getValue() * BITS_PER_BYTE) / timeDifSec), 
									pse.getRxBytes(), pse.getTxBytes())
									);
							
							//going to put the table push in here
							Map<String, Object> newPortRow = new HashMap<>();
							newPortRow.put(Port_DPID, npt.getNodeId());
							newPortRow.put(Port_ID, npt.getPortId());
							newPortRow.put(Port_SPEED, speed);
							
							 storageService.insertRow(Port_TABLE_NAME, newPortRow);
							
							
						} else { /* initialize */
							tentativePortStats.put(npt, SwitchPortBandwidth.of(npt.getNodeId(), npt.getPortId(), U64.ZERO, U64.ZERO, U64.ZERO, pse.getRxBytes(), pse.getTxBytes()));
						}//
					}
				}
			}
		}

		protected long getSpeed(NodePortTuple npt) {
			IOFSwitch sw = switchService.getSwitch(npt.getNodeId());
			long speed = 0;

			if(sw == null) return speed; /* could have disconnected; we'll assume zero-speed then */
			if(sw.getPort(npt.getPortId()) == null) return speed;

			/* getCurrSpeed() should handle different OpenFlow Version */
			OFVersion detectedVersion = sw.getOFFactory().getVersion();
			switch(detectedVersion){
				case OF_10:
					log.debug("Port speed statistics not supported in OpenFlow 1.0");
					break;

				case OF_11:
				case OF_12:
				case OF_13:
					speed = sw.getPort(npt.getPortId()).getCurrSpeed();
					break;

				case OF_14:
				case OF_15:
					for(OFPortDescProp p : sw.getPort(npt.getPortId()).getProperties()){
						if( p.getType() == 0 ){ /* OpenFlow 1.4 and OpenFlow 1.5 will return zero */
							speed = ((OFPortDescPropEthernet) p).getCurrSpeed();
						}
					}
					break;

				default:
					break;
			}

			return speed;

		}

	}
	
	
	/*
	 * 	Added by Charlie Hardwick-Kelly Carleton University 
	 * 	The purpose of this class is to track per flow metrics from within the network  
	 * 	one key advantage of the flow metrics is that they contain a duration time stamp 
	 * 	this eliminates the error involved from the control plane and allows us to compute the bandwidth of 
	 * 	a particular flow with greater accuracy
	 * 
	 *  	Steps 
	 * 		1. issue a meter stats request to all switches in parallel 
	 * 		2. Analyze the variables 
	 * 		3. if valid calculate relevant bandwidth 
	 * 		4. store information in hash 
	 * 		5. send information to the gui.
	 * 
	 */
	protected class FlowStatsCollector implements Runnable {

		@Override
		public void run() {
			
			Map<DatapathId, List<OFStatsReply>> flowReplies = getSwitchStatistics(switchService.getAllSwitchDpids(), OFStatsType.FLOW);
			for (Entry<DatapathId, List<OFStatsReply>> e : flowReplies.entrySet()) {
				for (OFStatsReply r : e.getValue()) {

					OFFlowStatsReply fsr = (OFFlowStatsReply) r; 
					for ( OFFlowStatsEntry fse : fsr.getEntries()) {
						//extract the necessary information from the existing stats and 
						DatapathId sw = e.getKey();
						Match match = fse.getMatch(); 						
						OFPort in_port = match.get(MatchField.IN_PORT);
						U64 bytesCount = fse.getByteCount(); //take in the number of bytes  
						long dur = fse.getDurationSec(); //take in the duration 
						
							
						
						//PUll all the necessary information from new stat

						MacAddress eth_src = match.get(MatchField.ETH_SRC);
						MacAddress eth_dst = match.get(MatchField.ETH_DST);
						EthType eth_type = match.get(MatchField.ETH_TYPE);
						
						
						// now I need to set up the comparisons 
						NodeFlowTuple nft = new NodeFlowTuple(sw,match) ;
						if(flowStats.containsKey(nft)){	
							//Retrieve the previous stat response information from the hash
							FlowBandwidth stat = flowStats.get(nft);
							
							//Calculate the bytes difference between current and previous collections
							// convert from bytes to bits 
							long byteDiff = (bytesCount.getValue() - stat.getBytes().getValue())*BITS_PER_BYTE;
							
							//Calculate the difference in time between the current and previous stat response
							long timediff = dur - stat.getDuration();
							long speed;
							// Avoid divide by zero error
							if(byteDiff!=0){							
								speed= byteDiff/timediff;
							}else {
								speed= 0;
							}
							try {
								if(!(speed < 0)) {		
									
									// if we have a valid flow we want to update the hash and then send the information to the GUI
									FlowBandwidth test = FlowBandwidth.of(sw, match, dur, bytesCount, speed,in_port);
									flowStats.put(nft,test);		
									
									//The SOCKET send to the gui should be implemented here 

									if(debug) {
										log.info("sw: "+ sw+ " speed(bps): "+ speed  + " bytes:" + bytesCount.getValue() + " Duration (s): " +dur + " In_Port: " + in_port + " Eth_Type: " + eth_type) ;
									}	
									
									 //Build the new row to go into the table with everything except speed
									 Map<String, Object> newRow = new HashMap<>();
									 newRow.put(DPID, sw);
									 newRow.put(ETHSRC, eth_src);
									 newRow.put(ETHDST, eth_dst);
									 newRow.put(ETHTYPE, eth_type);
									 newRow.put(DURATION, dur);
									 newRow.put(BYTECOUNT, bytesCount);
									 newRow.put(INPORT, in_port);
									 newRow.put(SPEED, speed);
									 storageService.insertRow(TABLE_NAME, newRow);
									
									
									
								}else {
									// if we are getting a negative speed a flow has restarted in the network, we do not want to 
									// send these values or print them out so just insert them into the hash default speed to 0
									FlowBandwidth test = FlowBandwidth.of(sw, match, dur, bytesCount, 0, in_port);
									flowStats.put(nft,test);
								}
							}catch(IllegalArgumentException exc) {
								System.out.println("Bad flow bandwidth" + exc);
							}
						}else {
						
							//if there is no existing flow stat insert with default 0 speed
							flowStats.put(nft, FlowBandwidth.of(sw, match, dur, bytesCount, 0,in_port)); 					
						}
					}
					if(debug) {
						log.info("###########################################################################################################################################################################################");
					}
				}	
			}	
			
		}

	}

	/**
	 * Single thread for collecting switch statistics and
	 * containing the reply.
	 * 
	 * @author Ryan Izard, ryan.izard@bigswitch.com, rizard@g.clemson.edu
	 *
	 */
	private class GetStatisticsThread extends Thread {
		private List<OFStatsReply> statsReply;
		private DatapathId switchId;
		private OFStatsType statType;

		public GetStatisticsThread(DatapathId switchId, OFStatsType statType) {
			this.switchId = switchId;
			this.statType = statType;
			this.statsReply = null;
		}

		public List<OFStatsReply> getStatisticsReply() {
			return statsReply;
		}

		public DatapathId getSwitchId() {
			return switchId;
		}

		@Override
		public void run() {
			statsReply = getSwitchStatistics(switchId, statType);
		}
	}
	
	/*
	 * IFloodlightModule implementation
	 */
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IStatisticsService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m =
				new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(IStatisticsService.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IOFSwitchService.class);
		l.add(IThreadPoolService.class);
		l.add(IRestApiService.class);
		l.add(IStorageSourceService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		switchService = context.getServiceImpl(IOFSwitchService.class);
		threadPoolService = context.getServiceImpl(IThreadPoolService.class);
		restApiService = context.getServiceImpl(IRestApiService.class);
		storageService = context.getServiceImpl(IStorageSourceService.class);

		Map<String, String> config = context.getConfigParams(this);
		if (config.containsKey(ENABLED_STR)) {
			try {
				isEnabled = Boolean.parseBoolean(config.get(ENABLED_STR).trim());
			} catch (Exception e) {
				log.error("Could not parse '{}'. Using default of {}", ENABLED_STR, isEnabled);
			}
		}
		log.info("Statistics collection {}", isEnabled ? "enabled" : "disabled");

		if (config.containsKey(INTERVAL_PORT_STATS_STR)) {
			try {
				//portStatsInterval = Integer.parseInt(config.get(INTERVAL_PORT_STATS_STR).trim());
			} catch (Exception e) {
				log.error("Could not parse '{}'. Using default of {}", INTERVAL_PORT_STATS_STR, portStatsInterval);
			}
		}
		log.info("Port statistics collection interval set to {}s", portStatsInterval);
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		restApiService.addRestletRoutable(new SwitchStatisticsWebRoutable());
		if (isEnabled) {
			startStatisticsCollection();
		}
	}

	/*
	 * IStatisticsService implementation
	 */
	
	@Override
	public SwitchPortBandwidth getBandwidthConsumption(DatapathId dpid, OFPort p) {
		return portStats.get(new NodePortTuple(dpid, p));
	}
	

	@Override
	public Map<NodePortTuple, SwitchPortBandwidth> getBandwidthConsumption() {
		return Collections.unmodifiableMap(portStats);
	}

	@Override
	public synchronized void collectStatistics(boolean collect) {
		if (collect && !isEnabled) {
			startStatisticsCollection();
			isEnabled = true;
		} else if (!collect && isEnabled) {
			stopStatisticsCollection();
			isEnabled = false;
		} 
		/* otherwise, state is not changing; no-op */
	}
	
	/*
	 * Helper functions
	 */
	
	/**
	 * Start all stats threads.
	 */
	private void startStatisticsCollection() {
		
		// create the table for storing flow data 
		storageService.createTable(TABLE_NAME, null);
		storageService.setTablePrimaryKeyName(TABLE_NAME,DPID);
		storageService.deleteMatchingRows(TABLE_NAME, null);
		
		
		//create table for port stats
		storageService.createTable(Port_TABLE_NAME, null);
		storageService.setTablePrimaryKeyName(Port_TABLE_NAME,Port_DPID);
		storageService.deleteMatchingRows(Port_TABLE_NAME, null);
		
		
		portStatsCollector = threadPoolService.getScheduledExecutor().scheduleAtFixedRate(new PortStatsCollector(), portStatsInterval, portStatsInterval, TimeUnit.SECONDS);
		flowStatsCollector = threadPoolService.getScheduledExecutor().scheduleAtFixedRate(new FlowStatsCollector(), flowStatsInterval, flowStatsInterval, TimeUnit.SECONDS);
		tentativePortStats.clear(); /* must clear out, otherwise might have huge BW result if present and wait a long time before re-enabling stats */
		log.warn("Statistics collection thread(s) started");
	}
	
	/**
	 * Stop all stats threads.
	 */
	private void stopStatisticsCollection() {
		if (!portStatsCollector.cancel(false)) {
			log.error("Could not cancel port stats thread");
		} else {
			log.warn("Statistics collection thread(s) stopped");
		}
		if (!flowStatsCollector.cancel(false)) {
			log.error("Could not cancel flow stats thread");
		} else {
			log.warn("Statistics collection thread(s) stopped");
		}
		
		
	}

	/**
	 * Retrieve the statistics from all switches in parallel.
	 * @param dpids
	 * @param statsType
	 * @return
	 */
	private Map<DatapathId, List<OFStatsReply>> getSwitchStatistics(Set<DatapathId> dpids, OFStatsType statsType) {
		HashMap<DatapathId, List<OFStatsReply>> model = new HashMap<DatapathId, List<OFStatsReply>>();

		List<GetStatisticsThread> activeThreads = new ArrayList<GetStatisticsThread>(dpids.size());
		List<GetStatisticsThread> pendingRemovalThreads = new ArrayList<GetStatisticsThread>();
		GetStatisticsThread t;
		for (DatapathId d : dpids) {
			t = new GetStatisticsThread(d, statsType);
			activeThreads.add(t);
			t.start();
		}

		/* Join all the threads after the timeout. Set a hard timeout
		 * of 12 seconds for the threads to finish. If the thread has not
		 * finished the switch has not replied yet and therefore we won't
		 * add the switch's stats to the reply.
		 */
		for (int iSleepCycles = 0; iSleepCycles < portStatsInterval; iSleepCycles++) {
			for (GetStatisticsThread curThread : activeThreads) {
				if (curThread.getState() == State.TERMINATED) {
					model.put(curThread.getSwitchId(), curThread.getStatisticsReply());
					pendingRemovalThreads.add(curThread);
				}
			}

			/* remove the threads that have completed the queries to the switches */
			for (GetStatisticsThread curThread : pendingRemovalThreads) {
				activeThreads.remove(curThread);
			}
			
			/* clear the list so we don't try to double remove them */
			pendingRemovalThreads.clear();

			/* if we are done finish early */
			if (activeThreads.isEmpty()) {
				break;
			}

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				log.error("Interrupted while waiting for statistics", e);
			}
		}

		return model;
	}

	/**
	 * Get statistics from a switch.
	 * @param switchId
	 * @param statsType
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected List<OFStatsReply> getSwitchStatistics(DatapathId switchId, OFStatsType statsType) {
		IOFSwitch sw = switchService.getSwitch(switchId);
		ListenableFuture<?> future;
		List<OFStatsReply> values = null;
		Match match;
		if (sw != null) {
			OFStatsRequest<?> req = null;
			switch (statsType) {
			case FLOW:
				match = sw.getOFFactory().buildMatch().build();
				req = sw.getOFFactory().buildFlowStatsRequest()
						.setMatch(match)
						.setOutPort(OFPort.ANY)
						.setTableId(TableId.ALL)
						.setOutGroup(OFGroup.ANY)
						.build();
				break;
				
			case FLOW_LIGHTWEIGHT:
				
				match = sw.getOFFactory().buildMatch().build();
				req =  sw.getOFFactory().buildFlowLightweightStatsRequest()
						.setMatch(match)
						.setOutPort(OFPort.ANY) 
						.setTableId(TableId.ALL)
						.build();
				break; 
				
			case FLOW_MONITOR:
				
				match = sw.getOFFactory().buildMatch().build();
				req = sw.getOFFactory().buildFlowMonitorRequest().build();
				
				
				break;
				
			case AGGREGATE:
				match = sw.getOFFactory().buildMatch().build();
				req = sw.getOFFactory().buildAggregateStatsRequest()
						.setMatch(match)
						.setOutPort(OFPort.ANY)
						.setTableId(TableId.ALL)
						.build();
				break;
			case PORT:
				req = sw.getOFFactory().buildPortStatsRequest()
				.setPortNo(OFPort.ANY)
				.build();
				break;
			case QUEUE:
				req = sw.getOFFactory().buildQueueStatsRequest()
				.setPortNo(OFPort.ANY)
				.setQueueId(UnsignedLong.MAX_VALUE.longValue())
				.build();
				break;
			case DESC:
				req = sw.getOFFactory().buildDescStatsRequest()
				.build();
				break;
			case GROUP:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) > 0) {
					req = sw.getOFFactory().buildGroupStatsRequest()				
							.build();
				}
				break;

			case METER:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_13) >= 0) {
					req = sw.getOFFactory().buildMeterStatsRequest()
							.setMeterId(OFMeterSerializerVer13.ALL_VAL)
							.build();
				}
				break;

			case GROUP_DESC:			
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) > 0) {
					req = sw.getOFFactory().buildGroupDescStatsRequest()			
							.build();
				}
				break;

			case GROUP_FEATURES:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) > 0) {
					req = sw.getOFFactory().buildGroupFeaturesStatsRequest()
							.build();
				}
				break;

			case METER_CONFIG:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_13) >= 0) {
					req = sw.getOFFactory().buildMeterConfigStatsRequest()
							.build();
				}
				break;

			case METER_FEATURES:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_13) >= 0) {
					req = sw.getOFFactory().buildMeterFeaturesStatsRequest()
							.build();
				}
				break;

			case TABLE:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) > 0) {
					req = sw.getOFFactory().buildTableStatsRequest()
							.build();
				}
				break;

			case TABLE_FEATURES:	
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) > 0) {
					req = sw.getOFFactory().buildTableFeaturesStatsRequest()
							.build();		
				}
				break;
			case PORT_DESC:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_13) >= 0) {
					req = sw.getOFFactory().buildPortDescStatsRequest()
							.build();
				}
				break;
			case EXPERIMENTER:		
			default:
				log.error("Stats Request Type {} not implemented yet", statsType.name());
				break;
			}

			try {
				if (req != null) {
					future = sw.writeStatsRequest(req); 
					values = (List<OFStatsReply>) future.get(portStatsInterval / 2, TimeUnit.SECONDS);
				}
			} catch (Exception e) {
				log.error("Failure retrieving statistics from switch {}. {}", sw, e);
			}
		}
		return values;
	}
}