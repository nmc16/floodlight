package net.floodlightcontroller.statistics.web;


import java.io.IOException;
import java.util.Date;

import net.floodlightcontroller.statistics.MeterBandwidth;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class SwitchMeterBandwidthSerializer extends JsonSerializer<MeterBandwidth> {
	@Override
	public void serialize(MeterBandwidth smb, JsonGenerator jGen, SerializerProvider serializer) throws IOException, JsonProcessingException {
		jGen.configure(Feature.WRITE_NUMBERS_AS_STRINGS, true);

		jGen.writeStartObject();
		jGen.writeStringField("dpid", smb.getSwitchId().toString());
		jGen.writeStringField("merterId", smb.getMatch().toString());
		jGen.writeStringField("updated", new Date(smb.getUpdateTime()).toString());
		jGen.writeStringField("link-speed-bits-per-second", smb.getLinkSpeedBitsPerSec().getBigInteger().toString());
		jGen.writeStringField("bits-per-second-rx", smb.getBitsPerSecondRx().getBigInteger().toString());
		jGen.writeStringField("bits-per-second-tx", smb.getBitsPerSecondTx().getBigInteger().toString());
		jGen.writeEndObject();
	}

}