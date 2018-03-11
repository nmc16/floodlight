package net.floodlightcontroller.topology.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.floodlightcontroller.core.web.serializers.DPIDSerializer;
import net.floodlightcontroller.core.web.serializers.OFPortSerializer;
import net.floodlightcontroller.core.web.serializers.U64Serializer;
import net.floodlightcontroller.linkdiscovery.Link;
import net.floodlightcontroller.topology.ITopologyService;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

public class RetrieveBlockedLinksResource extends ServerResource {

    private static final Logger LOG = LoggerFactory.getLogger(RetrieveBlockedLinksResource.class);

    @Get("json")
    public String retrieveBlockedLinks() {

        ITopologyService topologyService = (ITopologyService) getContext()
                                                             .getAttributes()
                                                             .get(ITopologyService.class.getCanonicalName());

        // Kind of annoying but we need to change the links to a serializable type
        Set<Link> blockedLinks = topologyService.getBlockedLinks();
        LOG.info("Blocked Links: {}", blockedLinks);

        try {
            SimpleModule simpleModule = new SimpleModule();
            simpleModule.addSerializer(DatapathId.class, new DPIDSerializer())
                        .addSerializer(OFPort.class, new OFPortSerializer())
                        .addSerializer(U64.class, new U64Serializer());

            // Send table data out to client
            ObjectMapper mapper = new ObjectMapper().registerModule(simpleModule);
            this.setStatus(Status.SUCCESS_OK);
            return mapper.writeValueAsString(blockedLinks);

        } catch (IOException e) {
            // This should never happen
            LOG.error("Hit unexpected error: " + e.getLocalizedMessage());
            this.setStatus(Status.SERVER_ERROR_INTERNAL);
            return "{\"error\": \"" + e.getLocalizedMessage() + "\"}";
        }
    }
}
