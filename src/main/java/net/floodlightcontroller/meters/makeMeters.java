package net.floodlightcontroller.meters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.DatapathId;


import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.internal.OFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class makeMeters implements IOFSwitchListener, IFloodlightModule {

	
	protected IFloodlightProviderService floodlightProvider;
	protected Set<Long> macAddresses;
	protected static Logger logger;
	IOFSwitchService switchService;
	
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
	    macAddresses = new ConcurrentSkipListSet<Long>();
	    logger = LoggerFactory.getLogger(makeMeters.class);
	    logger.info("*** I HAVE STARTED THE SWITCH **");
	}

	@Override
	public void startUp(FloodlightModuleContext context) {
	    //floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		switchService.addOFSwitchListener(this);
	}

	@Override
	public void switchAdded(DatapathId switchId) {
		// going to add meter stuff in here
		System.out.println("****ADDING A SWITCH");
		IOFSwitch mySwitch = switchService.getSwitch(switchId);
		System.out.println(mySwitch);		
	}
	
	@Override
	public void switchRemoved(DatapathId switchId) {
		// going to add meter stuff in here
		System.out.println("****ADDING A SWITCH");
		IOFSwitch mySwitch = switchService.getSwitch(switchId);
		System.out.println(mySwitch);	

	}

	@Override
	public void switchActivated(DatapathId switchId) {
		// going to add meter stuff in here
		System.out.println("****ADDING A SWITCH");
		IOFSwitch mySwitch = switchService.getSwitch(switchId);
		System.out.println(mySwitch);	
	}

	@Override
	public void switchPortChanged(DatapathId switchId, OFPortDesc port, PortChangeType type) {
		// going to add meter stuff in here
		System.out.println("****ADDING A SWITCH");
		IOFSwitch mySwitch = switchService.getSwitch(switchId);
		System.out.println(mySwitch);	
	}

	@Override
	public void switchChanged(DatapathId switchId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void switchDeactivated(DatapathId switchId) {
		// TODO Auto-generated method stub

	}

}