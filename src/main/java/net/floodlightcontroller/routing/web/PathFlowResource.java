package net.floodlightcontroller.routing.web;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.core.web.AllSwitchStatisticsResource;
import net.floodlightcontroller.core.web.CoreWebRoutable;
import net.floodlightcontroller.core.web.StatsReply;
import net.floodlightcontroller.core.web.SwitchResourceBase;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.internal.Device;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.linkdiscovery.Link;
import net.floodlightcontroller.linkdiscovery.internal.LinkInfo;
import net.floodlightcontroller.linkdiscovery.web.LinkWithType;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;

public class PathFlowResource extends SwitchResourceBase {

    private enum PathDirection {
        FORWARD,
        BACKWARD
    }

    private static final Logger LOG = LoggerFactory.getLogger(PathFlowResource.class);

    @Get("json")
    public String[] retrieveFlowPath() {
        // Get the request parameters for the request
        String dpid = (String) getRequestAttributes().get("src-dpid");
        String ethDst = (String) getRequestAttributes().get("eth-dst");
        String ethSrc = (String) getRequestAttributes().get("eth-src");
        String ethType = (String) getRequestAttributes().get("eth-type");

        // Get the switch reference from the switch service
        IOFSwitchService switchService = (IOFSwitchService) getContext()
                                                            .getAttributes()
                                                            .get(IOFSwitchService.class.getCanonicalName());

        ILinkDiscoveryService linkService = (ILinkDiscoveryService) getContext()
                                                                    .getAttributes()
                                                                    .get(ILinkDiscoveryService.class.getCanonicalName());

        IDeviceService deviceManager = (IDeviceService) getContext()
                                                        .getAttributes()
                                                        .get(IDeviceService.class.getCanonicalName());

        List<OFStatsReply> replies = getSwitchStatistics(dpid, OFStatsType.FLOW);
        IOFSwitch sw = switchService.getSwitch(DatapathId.of(dpid));

        // Build the match object that we are going to use to find the flow route
        Match match = sw.getOFFactory()
                        .buildMatch()
                        .setExact(MatchField.ETH_DST, MacAddress.of(ethDst))
                        .setExact(MatchField.ETH_SRC, MacAddress.of(ethSrc))
                        .setExact(MatchField.ETH_TYPE, EthType.of(Integer.parseInt(ethType, 16)))
                        .build();

        LOG.info("Finding matches for flow: " + match.toString());

        // I think there should only be one really but lets loop over it just in case
        Match rootMatch = null;
        List<OFAction> rootActions = null;
        for (OFStatsReply reply : replies) {
            // If it is not a flow stat we don't care about it
            if (reply.getStatsType() != OFStatsType.FLOW) {
                continue;
            }

            // Cast to the the flow stats reply to get the matches
            OFFlowStatsReply flowReply = (OFFlowStatsReply) reply;

            // Loop over all the flow entries to find the matching match
            for (OFFlowStatsEntry entry : flowReply.getEntries()) {
                if (match.get(MatchField.ETH_SRC).equals(entry.getMatch().get(MatchField.ETH_SRC)) &&
                        match.get(MatchField.ETH_DST).equals(entry.getMatch().get(MatchField.ETH_DST)) &&
                        match.get(MatchField.ETH_TYPE).equals(entry.getMatch().get(MatchField.ETH_TYPE))) {

                    rootMatch = entry.getMatch();
                    rootActions = entry.getActions();
                    LOG.info("Match found: " + entry.getMatch().toString());
                    break;
                }
            }
        }

        // Ensure that we found a match otherwise return invalid request
        if (rootMatch == null) {
            this.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            LOG.error("Client requested match field was not found on switch: " + dpid);
            return new String[0];
        }

        @SuppressWarnings("unchecked")
        Iterator<Device> srcIterator = (Iterator<Device>) deviceManager.queryDevices(MacAddress.of(ethSrc),
                                                                                     null,
                                                                                     IPv4Address.NONE,
                                                                                     IPv6Address.NONE,
                                                                                     DatapathId.NONE,
                                                                                     OFPort.ZERO);

        if (!srcIterator.hasNext()) {
            LOG.error("Couldn't find host with MAC address: " + ethSrc);
            return new String[0];
        }

        List<Device> srcDevices = new ArrayList<>();
        srcIterator.forEachRemaining(srcDevices::add);
        LOG.error("Source Devices: " + srcDevices.toString());

        // Otherwise now that we have the root match we can send threads going both ways along the flow
        // route to find the entire route
        NodePortTuple npt = new NodePortTuple(DatapathId.of(dpid), rootMatch.get(MatchField.IN_PORT));
        LOG.info("NPT: " + npt.toString());
        Map<NodePortTuple, Set<Link>> portLinks = linkService.getPortLinks();
        //LOG.info(portLinks.toString());
        LOG.info(portLinks.get(npt).toString());

        // Find the link for us ensure that it is the only link
        ArrayList<Link> linkSet = new ArrayList<>(portLinks.get(npt));
        assert linkSet.size() == 1;
        LOG.info("Found link: " + linkSet.get(0).toString());

        return new String[0];
    }

    private class PathFinderException extends Exception {

        public PathFinderException(String s) {
            super(s);
        }

        public PathFinderException(String s, Throwable throwable) {
            super(s, throwable);
        }
    }

    private class FlowPathFinder implements Callable<List<String>> {

        private PathDirection direction;
        private IOFSwitch sw;
        private Match match;
        private List<OFAction> actions;
        private ILinkDiscoveryService linkService;
        private IOFSwitchService switchService;
        private Map<NodePortTuple, Set<Link>> portLinks;

        public FlowPathFinder(IOFSwitch sw, Match match, List<OFAction> actions, PathDirection direction) {
            this.direction = direction;
            this.sw = sw;
            this.match = match;
            this.actions = actions;

            this.linkService = (ILinkDiscoveryService) getContext()
                                                       .getAttributes()
                                                       .get(ILinkDiscoveryService.class.getCanonicalName());

            this.switchService = (IOFSwitchService) getContext()
                                                    .getAttributes()
                                                    .get(IOFSwitchService.class.getCanonicalName());

            this.portLinks = this.linkService.getPortLinks();
        }

        @Override
        public List<String> call() throws Exception {
            return (this.direction == PathDirection.FORWARD) ? findPathToSrc() : findPathToDst();
        }

        private List<String> findPathToSrc() {
            NodePortTuple npt = new NodePortTuple(this.sw.getId(), this.match.get(MatchField.IN_PORT));
            //

            return null;
        }

        private IOFSwitch getNextHop(IOFSwitch curSwitch) throws PathFinderException {
            return (this.direction == PathDirection.FORWARD) ?
                    getNextHopForwards(curSwitch) : getNextHopBackwards();
        }

        private IOFSwitch getNextHopBackwards() throws PathFinderException {
            return null;
        }

        private IOFSwitch getNextHopForwards(IOFSwitch curSwitch) throws PathFinderException {
            // Get the flow entry on the current switch
            OFFlowStatsEntry flowEntry = getMatchingFlow(curSwitch);

            // For now we are going to assume that there is only going to be one action that we care about
            // TODO: so may have to re-visit this if the flow can go to two different switches
            OFPort outputPort = OFPort.ZERO;
            for (OFAction action : flowEntry.getActions()) {
                if (action.getType() == OFActionType.OUTPUT) {
                    outputPort = ((OFActionOutput) action).getPort();
                }
            }

            // If we didn't find the output port from the action list then we can't continue
            if (outputPort.equals(OFPort.ZERO)) {
                String msg = "Could not find the output action from switch: " + curSwitch.getId().toString();
                LOG.error(msg);
                throw new PathFinderException(msg);
            }

            return null;
        }

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
                        LOG.info("Match found for switch (" + curSwitch.getId() + ": " + entry.getMatch().toString());
                        return entry;
                    }
                }
            }

            // It's pretty catastrophic if we can't continue to follow the flow so
            throw new PathFinderException("Could not find matching flow entry on switch: " + curSwitch.getId());

        }

        private Link getLink(NodePortTuple npt) {
            Set<Link> links = portLinks.get(npt);

            // If the link was not found that usually means that the remaining link is external
            if (links == null) {
                // TODO



                return null;
            }

            // Otherwise we have to extract the link because we may store both directions
            Link hop = null;
            for (Link link : links) {
                if (link.getSrc() == this.sw.getId()) {
                    hop = link;
                }
            }

            if (hop == null) {
                // TODO
                String msg = "Could not find next hop link from action";
                LOG.error(msg);
                throw new RuntimeException(msg);
            }

            return hop;
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

        private List<String> findPathToDst() {

            // For now we are going to assume that there is only going to be one action that we care about
            // TODO: so may have to re-visit this if the flow can go to two different switches
            OFPort outputPort = OFPort.ZERO;
            for (OFAction action : actions) {
                if (action.getType() == OFActionType.OUTPUT) {
                    outputPort = ((OFActionOutput) action).getPort();
                }
            }

            // If we didn't find the output port from the action list then we can't continue
            if (outputPort.equals(OFPort.ZERO)) {
                // TODO
                String msg = "Could not find the output action from switch: " + this.sw.getId().toString();
                LOG.error(msg);
                throw new RuntimeException(msg);
            }

            // Find the link that corresponds to the output action
            NodePortTuple npt = new NodePortTuple(this.sw.getId(), outputPort);
            Set<Link> links = portLinks.get(npt);

            // If the link was not found that usually means that the remaining link is external
            if (links == null) {
                // TODO
            } else {
                // Otherwise we have to extract the link because we may store both directions
                Link hop = null;
                for (Link link : links) {
                    if (link.getSrc() == this.sw.getId()) {
                        hop = link;
                    }
                }

                if (hop == null) {
                    // TODO
                    String msg = "Could not find next hop link from action";
                    LOG.error(msg);
                    throw new RuntimeException(msg);
                }

                // Have to find the next switch and next action
                IOFSwitch iofSwitch = switchService.getSwitch(hop.getDst());

            }

            return null;
        }
    }
}
