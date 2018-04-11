package net.floodlightcontroller.topology.web;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.floodlightcontroller.core.web.ControllerSwitchesResource;
import net.floodlightcontroller.linkdiscovery.Link;
import net.floodlightcontroller.topology.ITopologyService;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.U64;
import org.restlet.data.Status;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BlockLinkResource extends ServerResource {

    private static final Logger LOG = LoggerFactory.getLogger(BlockLinkResource.class);
    private static final String BLOCK_KEYWORD = "block";

    /**
     * {
     *     "srcID": "<Data Path ID>",
     *     "srcPort": <Port Number>,
     *     "dstID": "<Data Path ID>",
     *     "dstPort": <Port Number>
     * }
     *
     *
     * @param json
     * @return
     */
    @Post
    @Put
    public String blockOrUnblockLink(String json) {

        // Determine whether we should be blocking the link or unblocking it
        boolean block = getRequestAttributes().get("block").equals(BLOCK_KEYWORD);
        boolean ret;

        // Get our topology service
        ITopologyService topologyService = (ITopologyService) getContext()
                                                              .getAttributes()
                                                              .get(ITopologyService.class.getCanonicalName());

        try {
            // Try and deserialize the JSON into a link
            ObjectMapper mapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addDeserializer(DatapathId.class, new TestDeserializer());
            mapper.registerModule(module);
            Link link = mapper.readValue(json, Link.class);
            link.setLatency(U64.ZERO);

            // Either disable or enable the link
            LOG.debug((block) ? "Disabling" : "Unblocking" + " link: " + link.toString());
            if (block) {
                ret = topologyService.blockLink(link);
            } else {
                ret = topologyService.unblockLink(link);
            }

        } catch (IOException e) {
            LOG.error("Could not decode the JSON into a link: " + e.getLocalizedMessage());
            this.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return "\"error\": \"" + e.getLocalizedMessage() + "\"";
        }

        // Set the return based on whether the request was a success or failure
        this.setStatus((ret) ? Status.SUCCESS_OK : Status.SERVER_ERROR_INTERNAL);
        return "";
    }

    /** TODO: Rename this */
    private class TestDeserializer extends StdDeserializer<DatapathId> {

        TestDeserializer() {
            this(null);
        }

        TestDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public DatapathId deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException, JsonProcessingException {

            return DatapathId.of(jsonParser.getValueAsString());
        }
    }
}
