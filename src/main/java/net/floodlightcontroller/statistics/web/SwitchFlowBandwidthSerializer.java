package net.floodlightcontroller.statistics.web;

import java.io.IOException;


import net.floodlightcontroller.statistics.FlowBandwidth;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class SwitchFlowBandwidthSerializer extends JsonSerializer<FlowBandwidth> {

	@Override
	public void serialize(FlowBandwidth spb, JsonGenerator jGen, SerializerProvider serializer) throws IOException, JsonProcessingException {
		jGen.configure(Feature.WRITE_NUMBERS_AS_STRINGS, true);
		jGen.writeStartObject();
		jGen.writeStringField("dpid", spb.getSwitchId().toString());
		jGen.writeStringField("match", spb.getMatch().toString());
		jGen.writeNumberField("dur", spb.getDuration());
		jGen.writeStringField("bytes", spb.getBytes().toString());
		jGen.writeNumberField("speed bps", spb.getFlowSpeedBitsPerSec());
		jGen.writeStringField("In_Port", spb.getInPort().toString());
		jGen.writeEndObject();
		
		
		//DatapathId d, Match m, long dur, U64 b, long s, OFPort p
		
		
		
	}

	

}