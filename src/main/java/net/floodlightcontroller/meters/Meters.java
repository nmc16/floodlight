package net.floodlightcontroller.meters;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModCommand;
import org.projectfloodlight.openflow.protocol.OFInstructionType;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMeterMod;
import org.projectfloodlight.openflow.protocol.OFMeterModCommand;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFMeterFlags;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionMeter;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.meterband.OFMeterBand;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.TableId;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

import net.floodlightcontroller.core.IFloodlightProviderService;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.Set;
import net.floodlightcontroller.staticentry.IStaticEntryPusherService;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/*
 * The Goal of this class is to initialize meters when a flow is added to a switch to attain  
 * per flow metrics throughout our network for the purpose of perflow network monitoring.   
 * this is done by first detecting a flow_MOD using the openflow protocol and the floodlight controler 
 * using  the detected from a flow mod  
 * 
 * 
 * 	THINGS TO THINK ABOUT
 * 		- cannot add idle timeouts to the METER_MOD command 
 * 		
 */


public class Meters implements IOFMessageListener, IFloodlightModule {

	protected IFloodlightProviderService floodlightProvider;
	protected Set<Long> Metersin;
	protected static Logger logger;
	protected int myflag = 1;
	protected int meterCounter = 1; 
	protected IStaticEntryPusherService staticFlowEntryPusher;
	protected int meterIdCounter = 1;
	protected int factoryFlag =0; 
	
	
	@Override
	public String getName() {
	    return Meters.class.getSimpleName();
	}

	
	
	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
	    Collection<Class<? extends IFloodlightService>> l =
	        new ArrayList<Class<? extends IFloodlightService>>();
	    l.add(IFloodlightProviderService.class);
	    return l;
	}
	
	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
	    floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
	    Metersin = new ConcurrentSkipListSet<Long>();
	    logger = LoggerFactory.getLogger(Meters.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) {
		floodlightProvider.addOFMessageListener(OFType.FLOW_MOD, this);
		
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
			
		switch(msg.getType()) {

			/*
			 * We look at the FLOW_MOD instead of flow add for 2 reasons 
			 * 	1.  We can make use of the complex matching that is already done by the controller
			 *  2.  the FLOW_MOD sent by the controller already contains a summary of all the 
			 *   	information from the flow that is needed for creating the meter
			 */
		
			case FLOW_MOD: 	
				// pull all the variables needed out of the flow mod 
				OFFlowMod pi = (OFFlowMod) msg; 
				Match testM = pi.getMatch();
				List<OFInstruction> inst = pi.getInstructions();			
				OFFlowModCommand type = pi.getCommand();
				TableId table = pi.getTableId();
				boolean corAdd = false; 			
				if(type == OFFlowModCommand.ADD) {
					EthType ethtype = testM.get(MatchField.ETH_TYPE);
					EthType ethtest = EthType.of(0x800);						
					
					// Need to make sure it is an action add not a meter add
					if(inst.get(0) != null) {
						OFInstruction testinst = inst.get(0);
						OFInstructionType curtype = testinst.getType();	
						OFInstructionType testval = OFInstructionType.valueOf("APPLY_ACTIONS");
						if(curtype.equals(testval)) {
							corAdd = true; 
						}
					}
					
					// need to check that the switch has the right factory 
					if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_13) >= 0) {
						factoryFlag =1;
					}
					
					
					// check that the Ethernet type add type are correct before trying to add the meter 					
					if(ethtype == ethtest && corAdd && myflag == 1 && factoryFlag ==1) {	
						pi.getTableId();
						try { 
							addMeter(testM,meterIdCounter,sw,inst, table);
						} catch (InterruptedException e) {
							//logger.info("We had a problem :" + pi.toString());
							e.printStackTrace();
						}
						meterIdCounter ++;
					}
				
				}
			break;
			default:
			break;
		}
        return Command.CONTINUE;
        
    }
	
	
	/*
	 * This function created to clean up the meter add process 
	 * takes in the information pulled from the flow mod and 
	 * creates a meter to monitor that flow 
	 */
	public void addMeter(Match match, long meterID, IOFSwitch sw, List<OFInstruction> instr, TableId table) throws InterruptedException {
		// need a list to hold all the built commands 
		List<OFMeterBand> bands = new ArrayList<OFMeterBand>();
		// First build the meter  
		OFFactory meterFactory = OFFactories.getFactory(OFVersion.OF_13); 
		OFMeterMod.Builder meterModBuilder = meterFactory.buildMeterMod().setMeterId(meterID).setCommand(OFMeterModCommand.ADD);		
		OFMeterBand.Builder meterband = meterFactory.meterBands().buildExperimenter();
		OFMeterBand band = meterband.build();
		bands.add(band);
        Set<OFMeterFlags> mflags = new HashSet<OFMeterFlags>();
        mflags.add(OFMeterFlags.KBPS) ;
		meterModBuilder.setMeters(bands).setFlags(mflags).build();
		sw.write(meterModBuilder.build());
		//add the flow to the meter 
		List<OFInstruction> flowinstructions2 = new ArrayList<OFInstruction>();
		OFInstructionMeter metertouse = meterFactory.instructions().buildMeter().setMeterId(meterID).build();		
		flowinstructions2.add(metertouse);
		flowinstructions2.addAll(instr);	
		OFFlowAdd flowAdd = meterFactory.buildFlowAdd().setInstructions(flowinstructions2).setMatch(match).setTableId(table).setPriority(1).build();
		sw.write(flowAdd);
	}
}