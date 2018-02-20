package net.floodlightcontroller.statistics;




import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.floodlightcontroller.statistics.web.SwitchPortBandwidthSerializer;

@JsonSerialize(using=SwitchPortBandwidthSerializer.class)
public class FlowBandwidth {
	private DatapathId id;
	private Match match; 
	private long duration;
	private U64 bytes;
	private long speed;
	private OFPort in_port;
	
	private FlowBandwidth() {}
	private FlowBandwidth(DatapathId d, Match m, long dur, U64 b, long s, OFPort p) {
		id = d;
		match = m;
		duration = dur;
		bytes = b;
		speed = s;
		in_port = p;
	}
	
	public static FlowBandwidth of(DatapathId d, Match m, long dur, U64 b, long s, OFPort p) {
		if (d == null) {
			throw new IllegalArgumentException("Datapath ID cannot be null");
		}
		if (m == null) {
			throw new IllegalArgumentException("Match cannot be null");
		}
		if(dur < 0) {
			throw new IllegalArgumentException("Duration cannot be negative");
		}
		if (b == null) {
			throw new IllegalArgumentException("bytes cannot be null");
		}	
		if (s < 0) {
			throw new IllegalArgumentException("Link speed cannot be null");
		}
		
		return new FlowBandwidth(d,m,dur, b, s,p);
	}
	
	public DatapathId getSwitchId() {
		return id;
	}
	
	public Match getMatch() {
		return match;
	}
	
	public long getDuration() {
		return duration;
	}
	
	public long getFlowSpeedBitsPerSec() {
		return speed;
	}
	
	public U64 getBytes() {
		return bytes;
	}
	public OFPort getInPort() {
		return in_port;
	}
	
		
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((match == null) ? 0 : match.hashCode());
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
		FlowBandwidth other = (FlowBandwidth) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (match == null) {
			if (other.match != null)
				return false;
		} else if (!match.equals(other.match))
			return false;
		return true;
	}
}