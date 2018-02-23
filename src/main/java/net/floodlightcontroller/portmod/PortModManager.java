package net.floodlightcontroller.portmod;

import java.util.*;
import java.util.concurrent.Callable;
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
import net.floodlightcontroller.storage.*;

import net.floodlightcontroller.threadpool.IThreadPoolService;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module provides ability to create and send port modifications to ports on switches.
 *
 * TODO: Seems like it only can work for version 1.3 or greater, need to investigate
 * TODO: Seems like some don't work with each other, i.e. NO_RECV and NO_FLOOD...
 *
 * @author nicolas.mccallum@carleton.ca
 */
public class PortModManager implements IFloodlightModule, IPortModService {

	// Class logger
	private static final Logger LOG = LoggerFactory.getLogger(PortModManager.class);

	// Database constant values
    private static final String TABLE_NAME = "controller_portmodhistory";
    private static final String DPID = "dpid";
    private static final String PORT = "port";
    private static final String PORTMOD = "portmod";
    private static final String TIME = "time";
    private static final String COLUMNS[] = {DPID, PORT, PORTMOD, TIME};

    private static final long REQUEST_TIMEOUT_MSEC = 1000;
	
	// Module service dependencies
	private IOFSwitchService switchService;
	private IRestApiService restService;
	private IStorageSourceService storageService;
	private IThreadPoolService threadPoolService;

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> services = new ArrayList<>();

		// Add ourselves as a service we provide
        services.add(IPortModService.class);
        return services;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> impls = new HashMap<>();

		// Add ourselves as our own implementation
        impls.put(IPortModService.class, this);
		return impls;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {

	    // Create list for the dependencies
		Collection<Class<? extends IFloodlightService>> dependencies = new ArrayList<>();

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
		this.threadPoolService = context.getServiceImpl(IThreadPoolService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		
		// Add our REST call to the REST services
		this.restService.addRestletRoutable(new PortModWebRoutable());

		// Create our database table for current and old port modifications
        this.storageService.createTable(TABLE_NAME, null);

		// Tell the world we have loaded
		LOG.info("Loaded module: Port modifications");
	}

	@Override
    public Future<OFPortMod> createPortModAsync(DatapathId dpid, OFPort port, OFPortConfig config, boolean enable) {
        // Create the callable we will submit to the thread executor
        PortModCallable callable = new PortModCallable(this, dpid, port, config, enable);

        // Submit to the executor and return the result
        return this.threadPoolService.getScheduledExecutor().schedule(callable, 0, TimeUnit.MICROSECONDS);
    }

	@Override
    public OFPortMod createPortMod(DatapathId dpid, OFPort port, OFPortConfig config, boolean enable)
            throws PortModException {

	    Map<OFPortConfig, Boolean> configs = new HashMap<>();
	    configs.put(config, enable);

	    return createPortMod(dpid, port, configs);
    }

    @Override
    public OFPortMod createPortMod(DatapathId dpid, OFPort port, Map<OFPortConfig, Boolean> configs)
            throws PortModException {

	    // Check we can work with our arguments
        if (dpid == null || port == null || configs == null) {
            String err = "Port modification cannot be created with uninitialized data: dpid=" + dpid + "port=" + port +
                         "config=" + configs;

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
                                                            .setConfig(getEnabledConfigs(configs))
                                                            .setMask(configs.keySet())
                                                            .setHwAddr(sw.getPort(port).getHwAddr())
                                                            .build();

	    // Send the message
        if (!sw.write(portMod)) {
            String err = "Could not send message " + configs.toString() + " to port " + port.toString() +
                         " on switch " + dpid.toString();

            LOG.error(err);
            throw new PortModException(err);
        }

        // Add the port modification into the history for tracking
        Map<String, Object> row = new HashMap<>();
        row.put(DPID, dpid);
        row.put(PORT, port);
        row.put(PORTMOD, portMod);
        row.put(TIME, Calendar.getInstance().getTime());
        this.storageService.insertRow(TABLE_NAME, row);

        LOG.info("Port successfully modified with: " + portMod.toString());
	    return portMod;
    }

    @Override
    public Set<OFPortConfig> retrievePortMods(DatapathId dpid, OFPort port) throws PortModException {

        // Check we can work with our arguments
        if (dpid == null || port == null) {
            String err = "Port description cannot be retrieved with uninitialized data: dpid=" + dpid + "port=" + port;
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
        Set<OFPortConfig> configs = new HashSet<>();

        // Extract the message
        try {
            LOG.info("Sending port description request for port " + port.toString() + " on switch " + dpid.toString());
            List<OFPortDescStatsReply> replies = response.get(REQUEST_TIMEOUT_MSEC, TimeUnit.MILLISECONDS);

            // Add each set into our own
            for (OFPortDescStatsReply reply : replies) {
                for (OFPortDesc portDesc : reply.getEntries()) {
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

    @Override
    public List<OFPortMod> getHistory(DatapathId dpid, OFPort port) throws PortModException {
        return getHistory(dpid, port, null, null);
    }

    @Override
    public List<OFPortMod> getHistory(DatapathId dpid, OFPort port, Date startTime, Date endTime)
            throws PortModException {

	    // Make sure we can work with our parameters
        if (dpid == null || port == null) {
            String err = "Port mod history cannot be retrieved with uninitialized data: dpid=" + dpid + "port=" + port;
            LOG.error(err);
            throw new PortModException(err);
        }

        // Create predicates that limit search results from the parameters passed in
        ArrayList<OperatorPredicate> predicates = new ArrayList<>();
        predicates.add(new OperatorPredicate(DPID, OperatorPredicate.Operator.EQ, dpid));
        predicates.add(new OperatorPredicate(PORT, OperatorPredicate.Operator.EQ, port));

        // Only add the time constraints if they are non-null
        if (startTime != null) {
            predicates.add(new OperatorPredicate(TIME, OperatorPredicate.Operator.GTE, startTime));
        }

        if (endTime != null) {
            predicates.add(new OperatorPredicate(TIME, OperatorPredicate.Operator.LTE, endTime));
        }

        // Now we have to trick the constructor by using an array instead of variable arguments
        OperatorPredicate arrPredicates[] = new OperatorPredicate[predicates.size()];
        arrPredicates = predicates.toArray(arrPredicates);
        CompoundPredicate query = new CompoundPredicate(CompoundPredicate.Operator.AND, false, arrPredicates);

        // Execute the query on our table
        IResultSet resultSet = this.storageService.executeQuery(TABLE_NAME, COLUMNS, query, null);

        // Return all of the found port modifications
        ArrayList<OFPortMod> portMods = new ArrayList<>();
        while (resultSet.next()) {
            portMods.add((OFPortMod) resultSet.getRow().get(PORTMOD));
        }
        resultSet.close();

        LOG.info("Found " + portMods.size() + " port modifications on port " + port.toString() + " on switch " +
                 dpid.toString() + " between start time " + ((startTime != null) ? startTime.toString() : "N/A") +
                 " and end time " + ((endTime != null) ? endTime.toString() : "N/A"));
        return portMods;
    }

    /**
     * Searches the configurations to be applied for enabled flags and returns the set of configurations
     * that are enabled.
     *
     * @param configs Original map of configurations and their respective enabled flags
     * @return Set of configurations that are enabled
     */
    private Set<OFPortConfig> getEnabledConfigs(Map<OFPortConfig, Boolean> configs) {
	    // Extract the configurations to be applied
	    Set<OFPortConfig> enabledConfigs = new HashSet<>(configs.keySet());

	    // Strip away the configurations that are being disabled
        for (Map.Entry<OFPortConfig, Boolean> entry : configs.entrySet()) {
            if (!entry.getValue()) {
                enabledConfigs.remove(entry.getKey());
            }
        }

        return enabledConfigs;
    }

    /**
     * Callable implementation for the asynchronous calls to create port modifications.
     *
     * @see PortModManager#createPortModAsync(DatapathId, OFPort, OFPortConfig, boolean)
     */
    private class PortModCallable implements Callable<OFPortMod> {

        private IPortModService portModService;
        private DatapathId dpid;
        private OFPort port;
        private OFPortConfig config;
        private boolean enable;

        public PortModCallable(IPortModService portModService, DatapathId dpid, OFPort port,
                               OFPortConfig config, boolean enable) {
            this.portModService = portModService;
            this.dpid = dpid;
            this.port = port;
            this.config = config;
            this.enable = enable;
        }

        @Override
        public OFPortMod call() throws Exception {
            return this.portModService.createPortMod(this.dpid, this.port, this.config, this.enable);
        }
    }
}
