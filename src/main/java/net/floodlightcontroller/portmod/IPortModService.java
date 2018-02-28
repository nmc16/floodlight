package net.floodlightcontroller.portmod;

import net.floodlightcontroller.core.module.IFloodlightService;
import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.protocol.OFPortMod;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service that provides utilities to apply port modifications on switches and to retrieve the current
 * configurations applied on a switch port.
 *
 * The list of valid modifications can be seen in {@link OFPortConfig}.
 *
 * @author nicolas.mccallum@carleton.ca
 */
public interface IPortModService extends IFloodlightService {
    /**
     * Applies the port configuration on the port on the switch via a port modification message.
     *
     * @param dpid Data path ID of the switch the port belongs to
     * @param port Port to apply the modification to
     * @param config Modification type (i.e. port down, no receive, etc.)
     * @param enable Flag to enable or disable the modification
     * @return Port modification that can be applied on the switch's port
     * @throws PortModException There was an error applying the port modification
     */
    OFPortMod createPortMod(DatapathId dpid, OFPort port, OFPortConfig config, boolean enable) throws PortModException;

    /**
     * Applies a set of port configurations on a port on the switch via a port modification message.
     *
     * There is no guarantee of order of application on the port.
     *
     * @param dpid Data path ID of the switch the port belongs to
     * @param port Port to apply the modification to
     * @param configs Map of the modification and its enabled flag (True will enable the modification)
     * @return Port modification that can be applied on the switch's port
     * @throws PortModException There was an error applying the one or more of the port modifications
     */
    OFPortMod createPortMod(DatapathId dpid, OFPort port, Map<OFPortConfig, Boolean> configs) throws PortModException;

    /**
     * Retrieves the current configurations applied to the port on the switch given by the data path ID.
     *
     * TODO: I think current implementation only works for OF_13, needs to be checked
     *
     * @param dpid Data path ID of the switch
     * @param port Port on the switch to retrieve modifications from
     * @return Set of applied configurations on the port
     */
    Set<OFPortConfig> retrievePortMods(DatapathId dpid, OFPort port) throws PortModException;

    /**
     * Retrieves all of the port modifications that have been applied to the switch port via the controller.
     *
     * Note: history data is limited to the lifetime of the controller (i.e. only remembered for the current session)
     *       as there is no guarantee it is pointing to the same network after restart.
     *
     * @param dpid Data path ID of the switch
     * @param port Port on the switch to retrieve the history for
     * @return List of port modifications sent to the port
     */
    List<OFPortMod> getHistory(DatapathId dpid, OFPort port) throws PortModException;

    /**
     * Retrieves all of the port modifications that have been applied to the switch port via the controller in
     * between the start and end times. If the start time is not specified, the query will return everything
     * until the end time. If the end time is not specified the query will return everything from the start time.
     *
     * Note: history data is limited to the lifetime of the controller (i.e. only remembered for the current session)
     *       as there is no guarantee it is pointing to the same network after restart.
     *
     * @param dpid Data path ID of the switch
     * @param port Port on the switch to retrieve the history for
     * @param startTime Start of the time range to filter results from
     * @param endTime End of the time range to filter results from
     * @return List of port modifications sent to the port
     */
    List<OFPortMod> getHistory(DatapathId dpid, OFPort port, Date startTime, Date endTime) throws PortModException;
}
