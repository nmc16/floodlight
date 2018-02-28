package net.floodlightcontroller.core.types;


import net.floodlightcontroller.core.web.serializers.DPIDSerializer;
import net.floodlightcontroller.core.web.serializers.NodeMeterTupleSerializer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.projectfloodlight.openflow.types.DatapathId;

/**
 * A NodeMeterTuple is similar to a SwitchPortTuple
 * but it only stores meter IDs instead of references
 * to the actual objects.
 * @author CharlieHardwick-Kelly interpreted from srini
 */

@JsonSerialize(using=NodeMeterTupleSerializer.class)
public class NodeMeterTuple implements Comparable<NodeMeterTuple> {
    private DatapathId nodeId; // switch DPID
    private long metertId; // switch meter id

    /**
     * Creates a NodeMeterTuple
     * @param nodeId The DPID of the switch
     * @param metertId The port of the switch
     */
    public NodeMeterTuple(DatapathId nodeId, long metertId) {
        this.nodeId = nodeId;
        this.metertId = metertId;
    }

    @JsonProperty("switch")
    @JsonSerialize(using=DPIDSerializer.class)
    public DatapathId getNodeId() {
        return nodeId;
    }
    public void setNodeId(DatapathId nodeId) {
        this.nodeId = nodeId;
    }
    @JsonProperty("port")
    public long getmetertId() {
        return metertId;
    }
    public void setmetertId(long metertId) {
        this.metertId = metertId;
    }
    
    public String toString() {
        return "[id=" + nodeId.toString() + ", meterId=" + metertId + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (nodeId.getLong() ^ (nodeId.getLong() >>> 32));
        result = (int) (prime * result + metertId);
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
        NodeMeterTuple other = (NodeMeterTuple) obj;
        if (!nodeId.equals(other.nodeId))
            return false;
        if (!(metertId == other.metertId))
            return false;
        return true;
    }

    /**
     * API to return a String value formed wtih NodeID and metertId
     * The metertId is a 16-bit field, so mask it as an integer to get full
     * positive value
     * @return
     */
    public String toKeyString() {
        return (nodeId.toString()+ "|" + metertId);
    }

    @Override
    public int compareTo(NodeMeterTuple obj) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (this.getNodeId().getLong() < obj.getNodeId().getLong())
            return BEFORE;
        if (this.getNodeId().getLong() > obj.getNodeId().getLong())
            return AFTER;

        if (this.getmetertId() < obj.getmetertId())
            return BEFORE;
        if (this.getmetertId()> obj.getmetertId())
            return AFTER;

        return EQUAL;
    }
}
