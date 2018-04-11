package net.floodlightcontroller.statistics;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.U64;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.floodlightcontroller.statistics.web.SwitchMeterBandwidthSerializer;


@JsonSerialize(using=SwitchMeterBandwidthSerializer.class)
public class MeterBandwidth {
	private DatapathId id;
	private long meterId; 
	private long duration;
	private U64 bytesIn;  
	private U64 speed;
	
	
	private MeterBandwidth() {}
	private MeterBandwidth(DatapathId d, long m, long t, U64 b, U64 s) {
		id = d;
		meterId = m;
		duration = t; 
		bytesIn = b;
		speed = s;
	}
	
	public static MeterBandwidth of(DatapathId d, long m, long t, U64 b, U64 s) {
		if (d == null) {
			throw new IllegalArgumentException("Datapath ID cannot be null");
		}
		if (m < 0) {
			throw new IllegalArgumentException("MeterID cannot be less than 0");
		}
		if (t < 0) {
			throw new IllegalArgumentException("time cannot be less than 0");
		}

		if (b == null) {
			throw new IllegalArgumentException("Bytes in cannot be null");
		}

		if (s == null) {
			throw new IllegalArgumentException("Link speed cannot be null");
		}
		
		return new MeterBandwidth(d ,m ,t ,b , s);
	}
	
	public DatapathId getSwitchId() {
		return id;
	}
	
	public long getMeterId() {
		return meterId;
	}
	public long getUpdateTime() {
		return duration;
	}
	
	public U64 getBytesIn() {
		return bytesIn; 
		
	}
	public U64 getFlowSpeedBitsPerSec() {
		return speed;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = (int) (prime * result + meterId);
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
		MeterBandwidth other = (MeterBandwidth) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (meterId == 0) {
			if (other.meterId != 0)
				return false;
		} else if (!(meterId == other.meterId))
			return false;
		return true;
	}
}