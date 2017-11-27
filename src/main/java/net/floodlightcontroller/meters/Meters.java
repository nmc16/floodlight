package net.floodlightcontroller.meters;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMatchBmap;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMeterBandStats;
import org.projectfloodlight.openflow.protocol.OFMeterMod;
import org.projectfloodlight.openflow.protocol.OFMeterModCommand;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFMeterFlags;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionMeter;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.meterband.OFMeterBand;
import org.projectfloodlight.openflow.protocol.meterband.OFMeterBandDrop;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.projectfloodlight.openflow.types.OFPort;
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

import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.staticentry.IStaticEntryPusherService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class Meters implements IOFMessageListener, IFloodlightModule {

	
	protected IFloodlightProviderService floodlightProvider;
	protected Set<Long> Metersin;
	protected static Logger logger;
	protected int myflag = 0;
	protected int meterCounter = 1; 
	protected IStaticEntryPusherService staticFlowEntryPusher;
	
	
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
	    floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	//	floodlightProvider.addOFMessageListener(OFType.TABLE_MOD, this);
		//floodlightProvider.addOFMessageListener(OFType.FLOW_MOD, this);
		
		
	}

	@Override
	   public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
			
			switch(msg.getType()) {
			
			/*case TABLE_MOD: 
				logger.info("TABLE MOD******************************" + msg.toString()) ;
				*/
		
			case FLOW_MOD:

				
				logger.info("FLOW MOD" )  ;
				//Ethernet ethr = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
				
				
				if(myflag ==0) {
					logger.info("Were going to try and get a meter going for our switch table 0x0" )  ;
					myflag =1 ;
					OFFactory meterFactory = OFFactories.getFactory(OFVersion.OF_13);
					OFMeterMod.Builder meterModBuilder = meterFactory.buildMeterMod().setMeterId(1).setCommand(OFMeterModCommand.ADD);
					OFMeterBand.Builder meterband = meterFactory.meterBands().buildExperimenter();
					OFMeterBand band = meterband.build();
					List<OFMeterBand> bands = new ArrayList<OFMeterBand>();
		            bands.add(band);
		            Set<OFMeterFlags> mflags = new HashSet<OFMeterFlags>();
		            mflags.add(OFMeterFlags.KBPS) ;
					meterModBuilder.setMeters(bands).setFlags(mflags).build();
					sw.write(meterModBuilder.build());
					
					// now try to add the flow to the meter 
					
					
					List<OFInstruction> instructions = new ArrayList<OFInstruction>();
					OFInstructionMeter meter = meterFactory.instructions().buildMeter().setMeterId(1).build();
					
					//OFInstructionApplyActions output = meterFactory.actions().buildOutput().setPort(OFPort.of(2)).setMaxLen(0xffFFffFF).build();

					instructions.add(meter);
					//instructions.add(Collections.singletonList((OFAction) output));
	
					//instructions.add(meter);
					OFFlowAdd flowAdd = meterFactory.buildFlowAdd().setInstructions(instructions).build();
					myflag ++; 
					
					
					
					// at this point we have added the meter now we  need to assign somthing to it 
					
					
					List<OFInstruction> flowinstructions = new ArrayList<OFInstruction>();
					
					
					OFInstructionMeter metertouse = meterFactory.instructions().buildMeter().setMeterId(1).build();
					//meterFactory.instructions().bui
					
					
					
					
					
					flowinstructions.add(metertouse);
					
					/* Flow will send matched packets to meter ID 1 and then possibly output on port 2 */
					OFFlowAdd flowAdd2 = meterFactory.buildFlowAdd()
					    /* set anything else you need, e.g. match */
					    .setInstructions(flowinstructions)
					    .build();
					
					
				}
				
				
				
		case PACKET_IN:		
				
				OFFactory myFactory = OFFactories.getFactory(OFVersion.OF_13);
						
				
				if(myflag ==0) {
					// add a meter 
					logger.info("Were going to try and get a meter going for our switch table 0x0" )  ;
					myflag =1 ;
					OFFactory meterFactory = OFFactories.getFactory(OFVersion.OF_13);
					OFMeterMod.Builder meterModBuilder = meterFactory.buildMeterMod().setMeterId(1).setCommand(OFMeterModCommand.ADD);
					OFMeterBand.Builder meterband = meterFactory.meterBands().buildExperimenter();
					OFMeterBand band = meterband.build();
					List<OFMeterBand> bands = new ArrayList<OFMeterBand>();
		            bands.add(band);
		            Set<OFMeterFlags> mflags = new HashSet<OFMeterFlags>();
		            mflags.add(OFMeterFlags.KBPS) ;
					meterModBuilder.setMeters(bands).setFlags(mflags).build();
					sw.write(meterModBuilder.build());
					// add a flow to said meter;
					OFInstructions instructions = myFactory.instructions();
					ArrayList<OFAction> actionList = new ArrayList<OFAction>();
					OFActions actions = myFactory.actions();
					OFOxms oxms = myFactory.oxms();
					List<OFInstruction> flowinstructions2 = new ArrayList<OFInstruction>();
					Match mymatch = myFactory.buildMatch().setMasked(MatchField.IPV4_SRC, IPv4AddressWithMask.of("10.0.0.1")).setMasked(MatchField.IPV4_DST, IPv4AddressWithMask.of("10.0.0.2")).build() ;
					OFInstructionMeter metertouse = myFactory.instructions().buildMeter().setMeterId(1).build();
					OFActionOutput output = meterFactory.actions().buildOutput().setPort(OFPort.of(2)).setMaxLen(0xffFFffFF).build();
					actionList.add(output);
					OFInstructionApplyActions applyActions = myFactory.instructions().buildApplyActions().setActions(actionList).build();
					flowinstructions2.add(metertouse);
					flowinstructions2.add(applyActions);
						
					OFFlowAdd flowAdd = myFactory.buildFlowAdd().setInstructions(flowinstructions2).setTableId(TableId.of(0)).build();
					sw.write(flowAdd);
			
				}
				
				
				
				
				
			/*case PACKET_IN:
				
				//logger.info("Packet in" ) ;
				// if this is our first try lets give it a shot
				
				logger.info("Trying to disect the packet" )  ;
				//Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
				
				
				logger.info("Here are the entris on the switch" )  ;
				System.out.println(staticFlowEntryPusher.getEntries(sw.getId()));
				
				
				
				if(myflag ==0) {
					
					logger.info("Were going to try and get a meter going for our switch table 0x0" )  ;
					myflag =1 ;
					OFFactory meterFactory = OFFactories.getFactory(OFVersion.OF_13);
					OFMeterMod.Builder meterModBuilder = meterFactory.buildMeterMod().setMeterId(1).setCommand(OFMeterModCommand.ADD);
					OFMeterBand.Builder meterband = meterFactory.meterBands().buildExperimenter();
					OFMeterBand band = meterband.build();
					List<OFMeterBand> bands = new ArrayList<OFMeterBand>();
		            bands.add(band);
		            Set<OFMeterFlags> mflags = new HashSet<OFMeterFlags>();
		            mflags.add(OFMeterFlags.KBPS) ;
					meterModBuilder.setMeters(bands).setFlags(mflags).build();
					sw.write(meterModBuilder.build());
					
					// now try to add the flow to the meter 
					
					
					List<OFInstruction> instructions = new ArrayList<OFInstruction>();
					OFInstructionMeter meter = meterFactory.instructions().buildMeter().setMeterId(1).build();
					
					//OFInstructionApplyActions output = meterFactory.actions().buildOutput().setPort(OFPort.of(2)).setMaxLen(0xffFFffFF).build();

					instructions.add(meter);
					//instructions.add(Collections.singletonList((OFAction) output));
	
					//instructions.add(meter);
					OFFlowAdd flowAdd = meterFactory.buildFlowAdd().setInstructions(instructions).build();
					myflag ++; 
					
				
					
					
					
					// at this point we have added the meter now we  need to assign somthing to it 
					
					
					List<OFInstruction> flowinstructions = new ArrayList<OFInstruction>();
					
					
					OFInstructionMeter metertouse = meterFactory.instructions().buildMeter().setMeterId(1).build();
					//meterFactory.instructions().bui
				//OFInstructionApplyActions output = meterFactory.actions().buildOutput().setPort(OFPort.of(2)).setMaxLen(0xffFFffFF).build();
					
					
					
					
					flowinstructions.add(metertouse);
					
					 Flow will send matched packets to meter ID 1 and then possibly output on port 2 
					OFFlowAdd flowAdd2 = meterFactory.buildFlowAdd()
					     set anything else you need, e.g. match 
					    .setInstructions(flowinstructions)
					    .build();

					
				}
				// OFPacketIn pi = (OFPacketIn) msg;
				// OFMatchBmap match = new OFMatchBmap();
				

					//logger.info("Here is the payload of the packet in : ");
					//System.out.println("The eth is " + eth.getEtherType() );
					
				//	 if (eth.getEtherType() == EthType.IPv4) {
			//	             We got an IPv4 packet; get the payload from Ethernet 
				   //        IPv4 ipv4 = (IPv4) eth.getPayload();
				    //        logger.info("IPV444444444 source: "+ ipv4.getDestinationAddress() + " The Dest is: "+ ipv4.getDestinationAddress());
				             
				            
				            
				           //  More to come here 
				    // } else if (eth.getEtherType() == EthType.ARP) {
				         //    We got an ARP packet; get the payload from Ethernet 
				    //        ARP arp = (ARP) eth.getPayload();
				           // logger.info("ARRRRRP");
				       //  More to come here 
				  
				    // } else {
			
		//		     }
					 
					
					
					*/
					
	
			
			}
	        return Command.CONTINUE;
	        
	    }
	
	
	


}
