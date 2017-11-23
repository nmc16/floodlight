package net.floodlightcontroller.portmod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.linkdiscovery.Link;
import net.floodlightcontroller.portmod.web.PortModWebRoutable;
import net.floodlightcontroller.restserver.IRestApiService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortMod implements IFloodlightModule {

	// Class logger
	private static final Logger LOG = LoggerFactory.getLogger(PortMod.class);
	
	// Module service dependencies
	private IOFSwitchService switchService;
	private IRestApiService restService;
	
	// Create list tracking downed links, should go into some sort of data storage class
	// TODO: Should move, might have to move this to link discovery?
	private ArrayList<Link> downLinks;
	
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
		/**
		 * Returns the dependencies we need loaded for the module.
		 * 
		 * Need the IOFSwitchService in order to construct the port modification
		 * messages we are sending to the switches. Need the rest service because
		 * we are taking REST requests for link modifications.
		 * 
		 * @return Collection of floodlight service dependencies.
		 */

		Collection<Class<? extends IFloodlightService>> dependencies = new ArrayList<Class<? extends IFloodlightService>>();

		dependencies.add(IOFSwitchService.class);
		dependencies.add(IRestApiService.class);
        return dependencies;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		/**
		 * Initializes our data structures using the context providing the dependency
		 * implementations.
		 * 
		 * @param context - Context that holds the module dependencies
		 * @throws FloodlightModuleException - Thrown if module can't be loaded
		 */
		
		// Initialize the downed links storage service
		this.downLinks = new ArrayList<Link>();
		
		// Load the implementations for our module dependencies
		this.switchService = context.getServiceImpl(IOFSwitchService.class);
		this.restService = context.getServiceImpl(IRestApiService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		
		// Add our REST call to the REST services
		this.restService.addRestletRoutable(new PortModWebRoutable());
		
		// Tell the world we have loaded
		LOG.info("Loaded module: Port modifications");
	}

}
