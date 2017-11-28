package net.floodlightcontroller.portmod;

import java.util.*;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.linkdiscovery.Link;
import net.floodlightcontroller.portmod.web.PortModWebRoutable;
import net.floodlightcontroller.restserver.IRestApiService;

import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortMod;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module provides ability to create and send port modifications to ports on switches.
 *
 * @author nicolas.mccallum@carleton.ca
 */
public class PortModManager implements IFloodlightModule, IPortModService {

	// Class logger
	private static final Logger LOG = LoggerFactory.getLogger(PortModManager.class);
	
	// Module service dependencies
	private IOFSwitchService switchService;
	private IRestApiService restService;
	
	// Create list tracking downed links, should go into some sort of data storage class
	// TODO: Should move, might have to move this to link discovery?
	private ArrayList<Link> downLinks;

	// Keep track of currently applied port modifications
    // TODO: Need to move it to memory storage table
    private HashMap<OFPort, ArrayList<OFPortConfig>> currentMods;

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> services = new ArrayList<Class<? extends IFloodlightService>>();

		// Add ourselves as a service we provide
        services.add(IPortModService.class);
        return services;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> impls =
                new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();

		// Add ourselves as our own implementation
        impls.put(IPortModService.class, this);
		return impls;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {

	    // Create list for the dependencies
		Collection<Class<? extends IFloodlightService>> dependencies = new ArrayList<Class<? extends IFloodlightService>>();

		// Add our dependencies on the switch service and REST API
		dependencies.add(IOFSwitchService.class);
		dependencies.add(IRestApiService.class);
        return dependencies;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {

		// Initialize the downed links storage service
		this.downLinks = new ArrayList<Link>();
		this.currentMods = new HashMap<OFPort, ArrayList<OFPortConfig>>();
		
		// Load the implementations for our module dependencies
		this.switchService = context.getServiceImpl(IOFSwitchService.class);
		this.restService = context.getServiceImpl(IRestApiService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		
		// Add our REST call to the REST services
		this.restService.addRestletRoutable(new PortModWebRoutable());
		
		// Tell the world we have loaded
		LOG.info("Loaded module: Port modifications");
	}

    @Override
    public OFPortMod createPortMod(DatapathId dpid, OFPort port, OFPortConfig config) throws PortModException {

	    // Check if port modification already exists on the port. If it does we can skip it, otherwise we add it



        // If the switch does not have a reference we can't create the message
	    IOFSwitch sw = this.switchService.getSwitch(dpid);
	    if (sw == null) {
	        throw new PortModException("Could not find DPID " + dpid.toString());
        }

        // Create the port modification message
        OFPortMod portMod = sw.getOFFactory().buildPortMod().setPortNo(port)
                                                            .setConfig(Collections.singleton(config))
                                                            .setMask(Collections.singleton(config))
                                                            .setHwAddr(sw.getPort(port)
                                                            .getHwAddr())
                                                            .build();

	    // Send the message
        if (!sw.write(portMod)) {
            throw new PortModException("Could not send message " + config.toString() + " to port " + port.toString() +
                                       " on switch " + dpid.toString());
        }

	    return portMod;
    }

    @Override
    public Collection<OFPortMod> retrievePortMods(OFPortDesc port) {
        return null;
    }
}
