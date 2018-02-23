package net.floodlightcontroller.routing.web;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.core.web.SwitchResourceBase;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.devicemanager.internal.Device;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.linkdiscovery.Link;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 *
 */
public class PathFlowResource extends SwitchResourceBase {

    /**
     *
     */
    private enum PathDirection {
        FORWARD,
        BACKWARD
    }

    private static final Logger LOG = LoggerFactory.getLogger(PathFlowResource.class);

    /**
     *
     * @return
     */
    @Get("json")
    public List<String> retrieveFlowPath() {
        // Get the request parameters for the request
        String dpid = (String) getRequestAttributes().get("src-dpid");
        String ethDst = (String) getRequestAttributes().get("eth-dst");
        String ethSrc = (String) getRequestAttributes().get("eth-src");
        String ethType = (String) getRequestAttributes().get("eth-type");

        // Get the switch reference from the switch service
        IOFSwitchService switchService = (IOFSwitchService) getContext()
                                                            .getAttributes()
                                                            .get(IOFSwitchService.class.getCanonicalName());

        IThreadPoolService threadPoolService = (IThreadPoolService) getContext()
                                                                    .getAttributes()
                                                                    .get(IThreadPoolService.class.getCanonicalName());

        // Build the match object that we are going to use to find the flow route
        IOFSwitch sw = switchService.getSwitch(DatapathId.of(dpid));
        Match match = sw.getOFFactory()
                        .buildMatch()
                        .setExact(MatchField.ETH_DST, MacAddress.of(ethDst))
                        .setExact(MatchField.ETH_SRC, MacAddress.of(ethSrc))
                        .setExact(MatchField.ETH_TYPE, EthType.of(Integer.decode(ethType)))
                        .build();

        LOG.info("Finding matches for flow: " + match.toString());
        FlowPathFinder forward = new FlowPathFinder(sw, match, PathDirection.FORWARD);
        FlowPathFinder backward = new FlowPathFinder(sw, match, PathDirection.BACKWARD);

        Future<List<String>> pathForward =
                threadPoolService.getScheduledExecutor().schedule(forward, 0, TimeUnit.MICROSECONDS);
        Future<List<String>> pathBackward =
                threadPoolService.getScheduledExecutor().schedule(backward, 0, TimeUnit.MICROSECONDS);

        List<String> path = new ArrayList<>();
        try {
            path.addAll(pathBackward.get(2, TimeUnit.SECONDS));

            // Remove the last index or else we will have two of the same switches
            // due to the starting point always being added
            path.remove(path.size() - 1);

            path.addAll(pathForward.get(2, TimeUnit.SECONDS));

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Could not get path due to error: " + e.getLocalizedMessage());
            this.setStatus(Status.SERVER_ERROR_INTERNAL);
            return Collections.emptyList();
        }


        return path;
    }

    /**
     *
     */
    private class PathFinderException extends Exception {

        public PathFinderException(String s) {
            super(s);
        }

        public PathFinderException(String s, Throwable throwable) {
            super(s, throwable);
        }
    }

    /**
     *
     */
    private class FlowPathFinder implements Callable<List<String>> {

        private PathDirection direction;
        private IOFSwitch rootSwitch;
        private Match match;
        private ILinkDiscoveryService linkService;
        private IOFSwitchService switchService;
        private IDeviceService deviceManager;
        private Map<NodePortTuple, Set<Link>> portLinks;

        /**
         *
         * @param rootSwitch
         * @param match
         * @param direction
         */
        public FlowPathFinder(IOFSwitch rootSwitch, Match match, PathDirection direction) {
            this.direction = direction;
            this.rootSwitch = rootSwitch;
            this.match = match;

            this.linkService = (ILinkDiscoveryService) getContext()
                                                       .getAttributes()
                                                       .get(ILinkDiscoveryService.class.getCanonicalName());

            this.switchService = (IOFSwitchService) getContext()
                                                    .getAttributes()
                                                    .get(IOFSwitchService.class.getCanonicalName());

            this.deviceManager = (IDeviceService) getContext()
                                                  .getAttributes()
                                                  .get(IDeviceService.class.getCanonicalName());

            this.portLinks = this.linkService.getPortLinks();
        }

        @Override
        public List<String> call() throws Exception {
            // Create a list to keep track of the path we found
            List<String> path = new ArrayList<>();

            // Get the root switch and add it to the list
            IOFSwitch curSwitch = this.rootSwitch;
            path.add(this.rootSwitch.getId().toString());

            // Loop until we hit a host
            while (curSwitch != null) {
                LOG.info("Current path going " + this.direction.toString() + ": " + path.toString());

                DatapathId next = getNextHop(curSwitch);
                curSwitch = switchService.getSwitch(next);

                // We may need to convert a DPID to a MAC address
                String id;
                if (curSwitch != null) {
                    // We are looking at a switch so add the DPID
                    id = next.toString();

                } else {
                    // We are looking at a host so convert it to a MAC address
                    id = MacAddress.of(next).toString();
                }

                // Going backwards means we want to add to the front of the list
                if (this.direction == PathDirection.FORWARD) {
                    path.add(id);
                } else {
                    path.add(0, id);
                }
            }

            // We must have hit our host so we can return the list
            return path;
        }

        /**
         *
         * @param curSwitch
         * @return
         * @throws PathFinderException
         */
        private DatapathId getNextHop(IOFSwitch curSwitch) throws PathFinderException {
            return (this.direction == PathDirection.FORWARD) ?
                    getNextHopForwards(curSwitch) : getNextHopBackwards(curSwitch);
        }

        /**
         *
         * @param curSwitch
         * @return
         * @throws PathFinderException
         */
        private DatapathId getNextHopBackwards(IOFSwitch curSwitch) throws PathFinderException {
            // Get the flow entry on the current switch
            OFFlowStatsEntry flowEntry = getMatchingFlow(curSwitch);

            NodePortTuple npt = new NodePortTuple(curSwitch.getId(), flowEntry.getMatch().get(MatchField.IN_PORT));
            Link link = getLinkOnPort(npt);

            // The link must be an access point to the destination host if it is null
            if (link == null) {
                return getHopToHost(curSwitch);
            }

            // Otherwise we can get the next switch ID from the link directly
            return link.getSrc();
        }

        /**
         *
         * @param curSwitch
         * @return
         * @throws PathFinderException
         */
        private DatapathId getNextHopForwards(IOFSwitch curSwitch) throws PathFinderException {
            // Get the flow entry on the current switch
            OFFlowStatsEntry flowEntry = getMatchingFlow(curSwitch);

            // For now we are going to assume that there is only going to be one action that we care about
            // TODO: so may have to re-visit this if the flow can go to two different switches
            OFPort outputPort = OFPort.ZERO;
            for (OFInstruction instruction : flowEntry.getInstructions()) {
                if (instruction.getType() == OFInstructionType.APPLY_ACTIONS) {
                    List<OFAction> actions = ((OFInstructionApplyActions) instruction).getActions();

                    if (actions != null) {
                        outputPort = ((OFActionOutput) actions.get(0)).getPort();
                    }
                }
            }

            // If we didn't find the output port from the action list then we can't continue
            if (outputPort.equals(OFPort.ZERO)) {
                String msg = "Could not find the output action from switch: " + curSwitch.getId().toString();
                LOG.error(msg);
                throw new PathFinderException(msg);
            }

            NodePortTuple npt = new NodePortTuple(curSwitch.getId(), outputPort);
            Link link = getLinkOnPort(npt);

            // The link must be an access point to the destination host if it is null
            if (link == null) {
                return getHopToHost(curSwitch);
            }

            // Otherwise we can get the next switch ID from the link directly
            return link.getDst();
        }

        /**
         *
         * @param curSwitch
         * @return
         * @throws PathFinderException
         */
        private DatapathId getHopToHost(IOFSwitch curSwitch) throws PathFinderException {
            MacAddress ethAddress = (this.direction == PathDirection.FORWARD) ?
                    this.match.get(MatchField.ETH_DST) : this.match.get(MatchField.ETH_SRC);

            @SuppressWarnings("unchecked")
            Iterator<Device> iterator = (Iterator<Device>) deviceManager.queryDevices(ethAddress,
                                                                                      null,
                                                                                      IPv4Address.NONE,
                                                                                      IPv6Address.NONE,
                                                                                      DatapathId.NONE,
                                                                                      OFPort.ZERO);

            // Make sure there is at least one device in the list
            if (!iterator.hasNext()) {
                LOG.error("Couldn't find host with MAC address: " + ethAddress.toString());
                throw new PathFinderException("Couldn't find host with MAC address: " + ethAddress.toString());
            }

            // We can probably safely assume that there is always going to only be one
            Device device = iterator.next();
            for (SwitchPort switchPort : device.getAttachmentPoints()) {
                if (switchPort.getNodeId() == curSwitch.getId()) {
                    // We found the connection so we can send back the final hop
                    return DatapathId.of(device.getMACAddress());
                }
            }

            // Otherwise we were supposed to find an attachment to the end but we didn't
            String msg = "Couldn't find any attachment between switch " + curSwitch.getId() +
                         " and host " + ethAddress.toString();

            LOG.error(msg);
            throw new PathFinderException(msg);
        }

        /**
         *
         * @param curSwitch
         * @return
         * @throws PathFinderException
         */
        private OFFlowStatsEntry getMatchingFlow(IOFSwitch curSwitch) throws PathFinderException {
            // Query the switch for the flow statistics
            List<OFStatsReply> replies = getSwitchStatistics(curSwitch.getId(), OFStatsType.FLOW);

            // Odds are that there will only be one reply but loop just in case
            for (OFStatsReply reply : replies) {
                // If it is not a flow stat we don't care about it
                if (reply.getStatsType() != OFStatsType.FLOW) {
                    continue;
                }

                // Loop over all the flow entries to find the matching match
                for (OFFlowStatsEntry entry : ((OFFlowStatsReply) reply).getEntries()) {
                    if (isMatch(this.match, entry.getMatch())) {
                        LOG.info("Match found for switch (" + curSwitch.getId() + "): " + entry.getMatch().toString());
                        LOG.info("Flow: " + entry.toString());
                        return entry;
                    }
                }
            }

            // It's pretty catastrophic if we can't continue to follow the flow so
            LOG.error("Could not find matching flow entry on switch: " + curSwitch.getId());
            throw new PathFinderException("Could not find matching flow entry on switch: " + curSwitch.getId());

        }

        /**
         * Gets the corresponding link that comes out of the switch port.
         *
         * Returns null if there were no links coming from that port. This usually means that the next link
         * is actually an access point to a host.
         *
         * @param npt
         * @return
         * @throws PathFinderException
         */
        private Link getLinkOnPort(NodePortTuple npt) throws PathFinderException {
            // If the link was not found that usually means that the remaining link is external
            Set<Link> links = portLinks.get(npt);
            if (links == null) {
                // Return null alert that the link is probably an access point
                LOG.info("Found no internal link for port: " + npt.toString());
                return null;
            }

            // Otherwise we have to extract the link because we may store both directions
            for (Link link : links) {
                if ((link.getSrc() == npt.getNodeId() && this.direction == PathDirection.FORWARD) ||
                        (link.getDst() == npt.getNodeId() && this.direction == PathDirection.BACKWARD)) {
                    LOG.info("Found link on port: " + link.toString());
                    return link;
                }
            }

            // If there were links but not the one we were looking for we have a problem
            LOG.error("Could not find next hop from port: " + npt.toString());
            throw new PathFinderException("Could not find next hop from port: " + npt.toString());
        }

        /**
         * Special equals implementation because realistically MACs are good enough identifiers without
         * requiring the user to provide IPs as well.
         *
         * @param m1
         * @param m2
         * @return
         */
        private boolean isMatch(Match m1, Match m2) {
            return m1.get(MatchField.ETH_SRC).equals(m2.get(MatchField.ETH_SRC)) &&
                   m1.get(MatchField.ETH_DST).equals(m2.get(MatchField.ETH_DST)) &&
                   m1.get(MatchField.ETH_TYPE).equals(m2.get(MatchField.ETH_TYPE));
        }
    }
}
