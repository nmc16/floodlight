package net.floodlightcontroller.topology.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.floodlightcontroller.linkdiscovery.ILinkDiscovery;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.linkdiscovery.Link;
import net.floodlightcontroller.linkdiscovery.internal.LinkInfo;
import net.floodlightcontroller.linkdiscovery.web.LinkWithType;
import net.floodlightcontroller.topology.ITopologyService;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RetrieveBlockedLinksResource extends ServerResource {

    private static final Logger LOG = LoggerFactory.getLogger(RetrieveBlockedLinksResource.class);

    @Get("json")
    public String retrieveBlockedLinks() {

        // Get our required services
        ILinkDiscoveryService ldService = (ILinkDiscoveryService) getContext()
                                                                 .getAttributes()
                                                                 .get(ILinkDiscoveryService.class.getCanonicalName());

        ITopologyService topologyService = (ITopologyService) getContext()
                                                             .getAttributes()
                                                             .get(ITopologyService.class.getCanonicalName());

        // Kind of annoying but we need to change the links to a serializable type
        Set<Link> blockedLinks = topologyService.getBlockedLinks();
        Map<Link, LinkInfo> links = ldService.getLinks();
        Set<LinkWithType> retLinks = new HashSet<>();

        for (Link link : blockedLinks) {
            LinkInfo info = links.get(link);
            ILinkDiscovery.LinkType type = ldService.getLinkType(link, info);

            // TODO: I really don't know if this is right
            if (type == ILinkDiscovery.LinkType.DIRECT_LINK || type == ILinkDiscovery.LinkType.TUNNEL) {
                retLinks.add(new LinkWithType(link, type, ILinkDiscovery.LinkDirection.BIDIRECTIONAL));
            } else {
                retLinks.add(new LinkWithType(link, type, ILinkDiscovery.LinkDirection.UNIDIRECTIONAL));
            }

        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            this.setStatus(Status.SUCCESS_OK);
            return mapper.writeValueAsString(retLinks);

        } catch (IOException e) {
            // This should never happen
            LOG.error("Hit unexpected error: " + e.getLocalizedMessage());
            this.setStatus(Status.SERVER_ERROR_INTERNAL);
            return "{\"error\": \"" + e.getLocalizedMessage() + "\"}";
        }
    }
}
