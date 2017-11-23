package net.floodlightcontroller.meters;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMeterBandStats;
import org.projectfloodlight.openflow.protocol.OFMeterMod;
import org.projectfloodlight.openflow.protocol.OFMeterModCommand;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFMeterFlags;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionMeter;
import org.projectfloodlight.openflow.protocol.meterband.OFMeterBand;
import org.projectfloodlight.openflow.protocol.meterband.OFMeterBandDrop;
import org.projectfloodlight.openflow.types.DatapathId;

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
import net.floodlightcontroller.packet.Ethernet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class Meters implements IOFMessageListener, IFloodlightModule {

	
	protected IFloodlightProviderService floodlightProvider;
	protected Set<Long> Metersin;
	protected static Logger logger;
	protected int myflag = 0;
  
	
	
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
	    //floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		floodlightProvider.addOFMessageListener(OFType.FLOW_MOD, this);
	}

	@Override
	   public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
			
			if(myflag ==0) {
				
				logger.info("Were going to try and get a meter going for our switch table 0x0" )  ;
				myflag =1 ;
				OFFactory meterFactory = OFFactories.getFactory(OFVersion.OF_13);
				OFMeterMod.Builder meterModBuilder = meterFactory.buildMeterMod().setMeterId(1).setCommand(OFMeterModCommand.ADD);
				OFMeterBand.Builder meterband = meterFactory.meterBands().buildDrop().setRate(1000);
				OFMeterBand band = meterband.build();
				List<OFMeterBand> bands = new ArrayList<OFMeterBand>();
	            bands.add(band);
	            Set<OFMeterFlags> mflags = new HashSet<OFMeterFlags>();
	            mflags.add(OFMeterFlags.KBPS) ;
				meterModBuilder.setMeters(bands).setFlags(mflags).build();
				sw.write(meterModBuilder.build());
				
				//List<OFInstruction> instructions = new ArrayList<OFInstruction>();
				
				
				//OFInstructionMeter meter = meterFactory.instructions().buildMeter().setMeterId(1).build();
				//instructions.add(meter);
				//OFFlowAdd flowAdd = meterFactory.buildFlowAdd();
				
			}
			
		
			
		
		
			/*System.out.println("I AM TRYING TO ADD A METER1");
			OFFactory meterFactory = OFFactories.getFactory(OFVersion.OF_13);
			System.out.println("I AM TRYING TO ADD A METER2");
			
			OFMeterMod.Builder meterModBuilder = meterFactory.buildMeterMod().setMeterId(1).setCommand(null);
			System.out.println("I AM TRYING TO ADD A METER3");
			OFMeterBandDrop.Builder bandBuilder = meterFactory.meterBands().buildDrop().setRate(10000);
			System.out.println("I AM TRYING TO ADD A METER4");
			OFMeterBand band = bandBuilder.build();
			System.out.println("I AM TRYING TO ADD A METER5");
			List<OFMeterBand> bands = new ArrayList<OFMeterBand>();
			System.out.println("I AM TRYING TO ADD A METER6");
			   Create meter modification message 
	        meterModBuilder.setMeters(bands).build();
			System.out.println("I AM TRYING TO ADD A METER7");
			sw.write(meterModBuilder.build());
			System.out.println("I AM TRYING TO ADD A METER8");*/
	        return Command.CONTINUE;
	    }
	
	
	


}
