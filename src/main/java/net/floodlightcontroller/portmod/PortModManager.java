package net.floodlightcontroller.portmod;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.portmod.web.PortModWebRoutable;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.storage.IStorageSourceService;

import org.projectfloodlight.openflow.protocol.*;
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

	// Database constant values
    private static final String TABLE_NAME = "controller_portmodhistory";
    private static final String PORTMOD_ID = "id";
    private static final String DPID = "dpid";
    private static final String PORT = "port";
    private static final String CONFIG = "config";
    private static final String COLUMNS[] = {PORTMOD_ID, DPID, PORT, CONFIG};

    private static final long REQUEST_TIMEOUT_MSEC = 1000;
	
	// Module service dependencies
	private IOFSwitchService switchService;
	private IRestApiService restService;
	private IStorageSourceService storageService;

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
		dependencies.add(IStorageSourceService.class);

        return dependencies;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {

		// Load the implementations for our module dependencies
		this.switchService = context.getServiceImpl(IOFSwitchService.class);
		this.restService = context.getServiceImpl(IRestApiService.class);
		this.storageService = context.getServiceImpl(IStorageSourceService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		
		// Add our REST call to the REST services
		this.restService.addRestletRoutable(new PortModWebRoutable());

		// Create our database table for current and old port modifications
        this.storageService.createTable(TABLE_NAME, null);
        this.storageService.setTablePrimaryKeyName(TABLE_NAME, PORTMOD_ID);

		// Tell the world we have loaded
		LOG.info("Loaded module: Port modifications");
	}

    @Override
    public OFPortMod createPortMod(DatapathId dpid, OFPort port, OFPortConfig config) throws PortModException {

	    // Check we can work with our arguments
        if (dpid == null || port == null || config == null) {
            String err = "Port modification cannot be created with uninitialized data: dpid=" + dpid + "port=" + port +
                         "config=" + config;

            LOG.error(err);
            throw new PortModException(err);
        }

        // If the switch does not have a reference we can't create the message
	    IOFSwitch sw = this.switchService.getSwitch(dpid);
	    if (sw == null) {
	        LOG.error("Could not find DPID " + dpid.toString());
	        throw new PortModException("Could not find DPID " + dpid.toString());
        }

        // Create the port modification message
        OFPortMod portMod = sw.getOFFactory().buildPortMod().setPortNo(port)
                                                            .setConfig(Collections.singleton(config))
                                                            .setMask(Collections.singleton(config))
                                                            .setHwAddr(sw.getPort(port).getHwAddr())
                                                            .build();

        // Add the port modification into the history for tracking
        Map<String, Object> row = new HashMap<String, Object>();
        row.put(PORTMOD_ID, "id");
        row.put(DPID, dpid);
        row.put(PORT, port);
        row.put(CONFIG, config);
        this.storageService.insertRow(TABLE_NAME, row);

	    // Send the message
        if (!sw.write(portMod)) {
            String err = "Could not send message " + config.toString() + " to port " + port.toString() +
                         " on switch " + dpid.toString();

            LOG.error(err);
            throw new PortModException(err);
        }

        LOG.info("Port successfully modified with: " + portMod.toString());
	    return portMod;
    }

    @Override
    public Set<OFPortConfig> retrievePortMods(DatapathId dpid, OFPort port) throws PortModException {

        // Check we can work with our arguments
        if (dpid == null || port == null) {
            String err = "Port description cannot be retrieve with uninitialized data: dpid=" + dpid + "port=" + port;
            LOG.error(err);
            throw new PortModException(err);
        }

        // If the switch does not have a reference we can't create the message
        IOFSwitch sw = this.switchService.getSwitch(dpid);
        if (sw == null) {
            LOG.error("Could not find DPID " + dpid.toString());
            throw new PortModException("Could not find DPID " + dpid.toString());
        }

        // TODO: This is only supported for OF >= 1.3?
        OFPortDescStatsRequest request = sw.getOFFactory().buildPortDescStatsRequest().build();

        // Send the message to the switch
        Future<List<OFPortDescStatsReply>> response = sw.writeStatsRequest(request);
        Set<OFPortConfig> configs = new HashSet<OFPortConfig>();

        // Extract the message
        try {
            LOG.info("Sending port description request for port " + port.toString() + " on switch " + dpid.toString());
            List<OFPortDescStatsReply> replies = response.get(REQUEST_TIMEOUT_MSEC, TimeUnit.MILLISECONDS);

            // Add each set into our own
            for (OFPortDescStatsReply reply : replies) {
                for (OFPortDesc portDesc : reply.getEntries()) {
                    // TODO: Refactor
                    if (portDesc.getPortNo() == port) {
                        configs.addAll(portDesc.getConfig());
                    }
                }
            }

        } catch (TimeoutException e) {
            // If we timeout make a special message for it
            String err = "Timed out waiting for response for port " + port.toString() + " on switch " + dpid.toString();
            LOG.error(err);
            throw new PortModException(err, e);

        } catch (Exception e) {
            // Otherwise not much we can do so just wrap and rethrow
            String err = "Unexpected error waiting for response for port " + port.toString() + " on switch " +
                         dpid.toString() + ": " + e.getMessage();

            LOG.error(err);
            throw new PortModException(err, e);
        }

        // Return our compiled list of configurations on the port
        LOG.info("Found the following configs for port " + port.toString() + " on switch " + dpid.toString() + ": " +
                 configs.toString());

	    return configs;
    }
}
