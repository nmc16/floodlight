package net.floodlightcontroller.portmod.web;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.floodlightcontroller.portmod.IPortModService;
import net.floodlightcontroller.portmod.PortModException;
import org.projectfloodlight.openflow.protocol.OFPortMod;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.restlet.data.Status;
import org.restlet.resource.ServerResource;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST module that handles the port modification requests for ports on switches in the network.
 *
 * @author nicolas.mccallum@carleton.ca
 */
public class CreatePortModResource extends ServerResource {

	private static final Logger LOG = LoggerFactory.getLogger(CreatePortModResource.class);

    /**
     * REST module that handles the port modification requests. Expects the following URL:
     *     http://127.0.0.1:8080/wm/portmod/create/{switch}/{port}/json
     *
     * Where switch is the DPID of the switch and port is the port number on the switch to modify. Expects
     * either a PUT or POST request with the following JSON format:
     *     {
     *         "config": {
     *             "config_name": true/false,
     *             ...
     *         }
     *     }
     *
     * The returned JSON will have the following format:
     *     {
     *         "version": "OF_XX",
     *         "config" : ["config1", "config2", ...],
     *         "mask"   : ["config1", "config2", ...],
     *         "mac"    : "XX:XX:XX:XX:XX:XX",
     *         "port"   : 1,
     *         "error"  : "Error message if request was not valid"
     *     }
     *
     * The request will set the HTTP return based on the success or fail of the request.
     *
     * @param json JSON request string that contains the configuration to apply
     * @return JSON representation of the applied port modification or an error message of the failed request
     */
	@Post
	@Put
	public String createPortMod(String json) {

	    // Get the parametrized inputs from the request URI
        String dpidString = (String) getRequestAttributes().get("switch");
        String portNumber = (String) getRequestAttributes().get("port");

        IPortModService portModService = (IPortModService) getContext().getAttributes()
                                                                       .get(IPortModService.class.getCanonicalName());

	    // We have to add our custom deserializer here because we don't own the OFPortMod class
        SimpleModule simpleModule = new SimpleModule().addSerializer(OFPortMod.class, new OFPortModSerializer());
        ObjectMapper objectMapper = new ObjectMapper().registerModule(simpleModule);

        try {
            // Create the request object from the JSON
            final PortModRequest request = objectMapper.readValue(json, PortModRequest.class);

            // Create a DPID instance from the provided DPID
            DatapathId dpid = DatapathId.of(dpidString);

            // Create our port class using the port number
            OFPort port = OFPort.of(Integer.valueOf(portNumber));

            LOG.debug("Creating port mod request with: dpid={}, port={}, config={}",
                      new Object[] {dpid.toString(), port.toString(), request.getConfigs()});

            // Now make the request to change the port
            OFPortMod mod = portModService.createPortMod(dpid, port, request.getConfigs());

            this.setStatus(Status.SUCCESS_OK);
            return objectMapper.writeValueAsString(mod);

        } catch (IOException | PortModException e) {
            LOG.error(e.getLocalizedMessage());
            this.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return "{\"error\": \"" + e.getLocalizedMessage() + "\"}";

        }
	}
}
