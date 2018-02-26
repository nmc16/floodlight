package net.floodlightcontroller.core.types;

import net.floodlightcontroller.core.web.serializers.DPIDSerializer;
import net.floodlightcontroller.core.web.serializers.NodeFlowTupleSerializer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.DatapathId;

/**
 * A NodelowTuple is similar to a SwitchPortTuple
 * but it only stores flow Match instead of references
 * to the actual objects.
 * @author srini
 */

@JsonSerialize(using=NodeFlowTupleSerializer.class)
public class NodeFlowTuple implements Comparable<NodeFlowTuple> {
    private DatapathId nodeId; // switch DPID
    private Match match; // Flow match

    /**
     * Creates a NodeFlowTuple
     * @param nodeId The DPID of the switch
     * @param portId The port of the switch
     */
    public NodeFlowTuple(DatapathId nodeId, Match match) {
        this.nodeId = nodeId;
        this.match = match;
    }

    @JsonProperty("switch")
    @JsonSerialize(using=DPIDSerializer.class)
    public DatapathId getNodeId() {
        return nodeId;
    }
    public void setNodeId(DatapathId nodeId) {
        this.nodeId = nodeId;
    }
    @JsonProperty("flow")
    public Match getMatch() {
        return match;
    }
    public void setPortId(Match match) {
        this.match = match;
    }
    
    public String toString() {
        return "[id=" + nodeId.toString() + ", Match=" + match.toString() + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (nodeId.getLong() ^ (nodeId.getLong() >>> 32));
        //result = prime * result + match.getVersion();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NodeFlowTuple other = (NodeFlowTuple) obj;
        if (!nodeId.equals(other.nodeId))
            return false;
        if (!match.equals(other.match))
            return false;
        return true;
    }

    /**
     * API to return a String value formed wtih NodeID and PortID
     * The portID is a 16-bit field, so mask it as an integer to get full
     * positive value
     * @return
     */
    public String toKeyString() {
        return (nodeId.toString()+ "|" + match.toString());
    }

    @Override
    public int compareTo(NodeFlowTuple obj) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

       if (this.getNodeId().getLong() < obj.getNodeId().getLong())
            return BEFORE;
        if (this.getNodeId().getLong() > obj.getNodeId().getLong())
            return AFTER;

       // if (this.getPortId().getPortNumber() < obj.getPortId().getPortNumber())
        //    return BEFORE;
       // if (this.getPortId().getPortNumber() > obj.getPortId().getPortNumber())
        //    return AFTER;

        return EQUAL;
    }
}
