package net.floodlightcontroller.statistics.web;


import java.io.IOException;
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
		jGen.writeNumberField("merterId", smb.getMeterId());
		jGen.writeNumberField("duration", smb.getMeterId());
		jGen.writeStringField("bytesIn", smb.getBytesIn().getBigInteger().toString());
		jGen.writeStringField("flow-speed-bits-per-second", smb.getFlowSpeedBitsPerSec().getBigInteger().toString());
		jGen.writeEndObject();
	}
}