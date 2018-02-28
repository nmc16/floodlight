package net.floodlightcontroller.core.web.serializers;


import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import net.floodlightcontroller.core.types.NodeMeterTuple;

/**
 * Serialize a NodeMeterTupleSerializer
 */
public class NodeMeterTupleSerializer extends JsonSerializer<NodeMeterTuple> {

    @Override
    public void serialize(NodeMeterTuple npt, JsonGenerator jGen,
                          SerializerProvider serializer)
                                  throws IOException, JsonProcessingException {
        serialize(npt, jGen);
    }
    
    public static void serialize(NodeMeterTuple nmt, JsonGenerator jGen) throws IOException {
        jGen.writeStartObject();
        jGen.writeStringField("switch", nmt.getNodeId().toString());
        jGen.writeNumberField("port", nmt.getmetertId());
        jGen.writeEndObject();
    }

}
