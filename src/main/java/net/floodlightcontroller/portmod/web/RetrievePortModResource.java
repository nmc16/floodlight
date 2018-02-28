package net.floodlightcontroller.portmod.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.floodlightcontroller.portmod.IPortModService;
import net.floodlightcontroller.portmod.PortModException;
import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

/**
 * REST module that handles requests for currently applied port modifications on a switch's port.
 *
 * @author nicolas.mccallum@carleton.ca
 */
public class RetrievePortModResource extends ServerResource {

    private static final Logger LOG = LoggerFactory.getLogger(RetrievePortModResource.class);

    /**
     * REST module that returns the currently applied port modifications applied on a switch port. Expects the
     * following URL:
     *     http://127.0.0.1:8080/wm/portmod/retrieve/{switch}/{port}/json
     *
     * Where switch is the DPID of the switch and port is the port number on the switch to modify. Expects a GET
     * request.
     *
     * The returned JSON will have the following format:
     *     {
     *         "configurations": [
     *             "config1",
     *             ...
     *         ],
     *         "error": "Error message if there was an error processing the request"
     *     }
     *
     * The request will set the HTTP return based on the success or fail of the request.
     *
     * @return Returns the JSON response with the format above
     */
    @Get("json")
    public String retrievePortMod() {

        // Get the parametrized inputs from the request URI
        String dpidString = (String) getRequestAttributes().get("switch");
        String portNumber = (String) getRequestAttributes().get("port");

        ObjectMapper objectMapper = new ObjectMapper();
        IPortModService portModService = (IPortModService) getContext().getAttributes()
                                                                       .get(IPortModService.class.getCanonicalName());

        try {

            // Create a DPID instance from the provided DPID
            DatapathId dpid = DatapathId.of(dpidString);

            // Create our port class using the port number
            OFPort port = OFPort.of(Integer.valueOf(portNumber));

            // Now make the request to change the port
            Set<OFPortConfig> configs = portModService.retrievePortMods(dpid, port);

            this.setStatus(Status.SUCCESS_OK);
            return "{\"configurations\": " + objectMapper.writeValueAsString(configs) + "}";

        } catch (PortModException e) {
            LOG.error(e.getLocalizedMessage());
            this.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return "{\"error\": \"" + e.getLocalizedMessage() + "\"}";

        } catch (IOException e) {
            // This should never happen so catch it separately
            LOG.error("Hit unexpected error: " + e.getLocalizedMessage());
            this.setStatus(Status.SERVER_ERROR_INTERNAL);
            return "{\"error\": \"" + e.getLocalizedMessage() + "\"}";
        }
    }
}
