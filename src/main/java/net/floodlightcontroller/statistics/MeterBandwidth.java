package net.floodlightcontroller.statistics;

import java.util.Date;

import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.U64;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.floodlightcontroller.statistics.web.SwitchMeterBandwidthSerializer;


@JsonSerialize(using=SwitchMeterBandwidthSerializer.class)
public class MeterBandwidth {
	private DatapathId id;
	private Match match; 
	private U64 speed;
	private U64 rx;
	private U64 tx;
	private Date time;
	private U64 rxValue;
	private U64 txValue;
	
	private MeterBandwidth() {}
	private MeterBandwidth(DatapathId d, Match m, U64 s, U64 rx, U64 tx, U64 rxValue, U64 txValue) {
		id = d;
		match = m;
		speed = s;
		this.rx = rx;
		this.tx = tx;
		time = new Date();
		this.rxValue = rxValue;
		this.txValue = txValue;
	}
	
	public static MeterBandwidth of(DatapathId d,Match m, U64 s, U64 rx, U64 tx, U64 rxValue, U64 txValue) {
		if (d == null) {
			throw new IllegalArgumentException("Datapath ID cannot be null");
		}
		if (m == null) {
			throw new IllegalArgumentException("Match cannot be null");
		}
		if (s == null) {
			throw new IllegalArgumentException("Link speed cannot be null");
		}
		if (rx == null) {
			throw new IllegalArgumentException("RX bandwidth cannot be null");
		}
		if (tx == null) {
			throw new IllegalArgumentException("TX bandwidth cannot be null");
		}
		if (rxValue == null) {
			throw new IllegalArgumentException("RX value cannot be null");
		}
		if (txValue == null) {
			throw new IllegalArgumentException("TX value cannot be null");
		}
		return new MeterBandwidth(d,m, s, rx, tx, rxValue, txValue);
	}
	
	public DatapathId getSwitchId() {
		return id;
	}
	
	public Match getMatch() {
		return match;
	}
	
	public U64 getLinkSpeedBitsPerSec() {
		return speed;
	}
	
	public U64 getBitsPerSecondRx() {
		return rx;
	}
	
	public U64 getBitsPerSecondTx() {
		return tx;
	}
	
	protected U64 getPriorByteValueRx() {
		return rxValue;
	}
	
	protected U64 getPriorByteValueTx() {
		return txValue;
	}
	
	public long getUpdateTime() {
		return time.getTime();
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
		MeterBandwidth other = (MeterBandwidth) obj;
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