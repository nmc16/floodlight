package net.floodlightcontroller.portmod.web;

import net.floodlightcontroller.portmod.IPortModService;
import net.floodlightcontroller.portmod.PortModException;
import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RetrievePortModResource extends ServerResource {

    private static final Logger LOG = LoggerFactory.getLogger(RetrievePortModResource.class);

    /**
     * TODO: Look into setStatus etc methods from ServerResource base class
     *
     * @return
     */
    @Get("json")
    public Map<String, String> retrievePortMod() {

        // Get the parametrized inputs from the request URI
        String dpidString = (String) getRequestAttributes().get("switch");
        String portNumber = (String) getRequestAttributes().get("port");

        IPortModService portModService = (IPortModService) getContext().getAttributes()
                                                                       .get(IPortModService.class.getCanonicalName());

        try {

            // Create a DPID instance from the provided DPID
            DatapathId dpid = DatapathId.of(dpidString);

            // Create our port class using the port number
            OFPort port = OFPort.of(Integer.valueOf(portNumber));

            // Now make the request to change the port
            Set<OFPortConfig> configs = portModService.retrievePortMods(dpid, port);

            return Collections.singletonMap("response", configs.toString());

        } catch (PortModException e) {
            LOG.error(e.getMessage());

            HashMap<String, String> ret = new HashMap<String, String>();
            ret.put("status", "fail");
            ret.put("message", e.toString());
            return ret;

        }
    }
}
