package net.floodlightcontroller.portmod;

import net.floodlightcontroller.core.module.IFloodlightService;
import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.protocol.OFPortMod;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

import java.util.Set;

/**
 *
 */
public interface IPortModService extends IFloodlightService {
    /**
     * Creates a port modification that can be applied on a port.
     *
     * TODO: Should probably have one that takes a list
     * TODO: Need to change port config to something else cause it sucks
     *
     * @param port Port to apply the modification to
     * @param config Modification type (i.e. port down, no receive, etc.)
     * @return Port modification that can be applied on the switch's port
     */
    OFPortMod createPortMod(DatapathId dpid, OFPort port, OFPortConfig config) throws PortModException;

    /**
     * TODO: Better to use database or better to send a message to switch? Not sure
     * @param port
     * @return
     */
    Set<OFPortConfig> retrievePortMods(DatapathId dpid, OFPort port) throws PortModException;
}
