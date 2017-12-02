package net.floodlightcontroller.portmod.web;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.floodlightcontroller.portmod.IPortModService;
import net.floodlightcontroller.portmod.PortModException;
import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.restlet.resource.ServerResource;
import org.restlet.resource.Post;
import org.restlet.resource.Put;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreatePortModResource extends ServerResource {

	private static final Logger LOG = LoggerFactory.getLogger(CreatePortModResource.class);

    /**
     * TODO: Look into setStatus etc methods from ServerResource base class
     *
     * @param json
     * @return
     */
	@Post
	@Put
	public Map<String, String> createPortMod(String json) {

	    // Get the parametrized inputs from the request URI
        String dpidString = (String) getRequestAttributes().get("switch");
        String portNumber = (String) getRequestAttributes().get("port");

        IPortModService portModService = (IPortModService) getContext()
                                                           .getAttributes()
                                                           .get(IPortModService.class.getCanonicalName());

	    // Create the request object from the JSON
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            final PortModRequest request = objectMapper.readValue(json, PortModRequest.class);

            // Create a DPID instance from the provided DPID
            DatapathId dpid = DatapathId.of(dpidString);

            // Create our port class using the port number
            OFPort port = OFPort.of(Integer.valueOf(portNumber));

            LOG.debug("Creating port mod request with: dpid={}, port={}, config={}",
                      new Object[] {dpid.toString(), port.toString(), request.getConfig()});

            // Now make the request to change the port
            portModService.createPortMod(dpid, port, OFPortConfig.valueOf(request.getConfig()));

        } catch (IOException | PortModException e) {
            LOG.error(e.getMessage());

            HashMap<String, String> ret = new HashMap<String, String>();
            ret.put("status", "fail");
            ret.put("message", e.toString());
            return ret;

        }
		
        return Collections.singletonMap("status", "success");
	}
}
