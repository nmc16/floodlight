package net.floodlightcontroller.portmod.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.floodlightcontroller.portmod.IPortModService;
import net.floodlightcontroller.portmod.PortModException;
import org.projectfloodlight.openflow.protocol.OFPortMod;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * REST module that returns the port modifications applied on switch ports in the current network.
 *
 * Note: history data is limited to the lifetime of the controller (i.e. only remembered for the current session) as
 *       there is no guarantee it is pointing to the same network after restart.
 *
 * @author nicolas.mccallum@carleton.ca
 */
public class PortModHistoryResource extends ServerResource {

    private static final Logger LOG = LoggerFactory.getLogger(RetrievePortModResource.class);

    /**
     * REST module that retrieves history of port modifications on a switch port. Expects the following URL:
     *     http://127.0.0.1:8080/wm/portmod/history/{switch}/{port}/json
     *
     * Where switch is the DPID of the switch and port is the port number on the switch to modify. The URL can have
     * the optional parameters startTime and endTime which are unix timestamps since epoch. Expects a GET request.
     *
     * The returned JSON will have the following format:
     *     [
     *         {
     *             "version": "OF_XX",
     *             "config" : ["config1", "config2", ...],
     *             "mask"   : ["config1", "config2", ...],
     *             "mac"    : "XX:XX:XX:XX:XX:XX",
     *             "port"   : 1
     *         },
     *         ...
     *     ]
     *
     *     **OR**
     *
     *     {"error": "Error message if request was not valid"}
     *
     * The request will return 200 on success and 500 if there is an unexpected error.
     *
     * @return JSON array of the port modifications or an error message of the failed request
     */
    @Get("json")
    public String getHistory() {

        // Get the parametrized inputs from the request URI
        String dpidString = (String) getRequestAttributes().get("switch");
        String portNumber = (String) getRequestAttributes().get("port");
        String startTime = getQueryValue("startTime");
        String endTime = getQueryValue("endTime");

        IPortModService portModService = (IPortModService) getContext().getAttributes()
                                                                       .get(IPortModService.class.getCanonicalName());

        // We have to add our custom deserializer here because we don't own the OFPortMod class
        SimpleModule simpleModule = new SimpleModule("SimpleModule").addSerializer(OFPortMod.class,
                                                                                         new OFPortModSerializer());
        ObjectMapper objectMapper = new ObjectMapper().registerModule(simpleModule);

        // Convert the times to dates if possible, otherwise just leave them as null
        Date startDate = (startTime != null) ? new Date(Long.parseLong(startTime)) : null;
        Date endDate = (endTime != null) ? new Date(Long.parseLong(endTime)) : null;

        // Create a DPID instance from the provided DPID
        DatapathId dpid = DatapathId.of(dpidString);

        // Create our port class using the port number
        OFPort port = OFPort.of(Integer.valueOf(portNumber));

        try {
            // Retrieve the history from the controller
            List<OFPortMod> history = portModService.getHistory(dpid, port, startDate, endDate);
            this.setStatus(Status.SUCCESS_OK);
            return objectMapper.writeValueAsString(history);

        } catch (PortModException | IOException e) {
            e.printStackTrace();
            LOG.error(e.getLocalizedMessage());
            this.setStatus(Status.SERVER_ERROR_INTERNAL);
            return "{\"error\": \"" + e.getLocalizedMessage() + "\"}";
        }
    }
}
