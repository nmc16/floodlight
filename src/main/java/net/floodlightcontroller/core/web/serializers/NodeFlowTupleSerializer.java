package net.floodlightcontroller.core.web.serializers;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import net.floodlightcontroller.core.types.NodeFlowTuple;

/**
 * Serialize a NodeFlowTupleSerializer
 */
public class NodeFlowTupleSerializer extends JsonSerializer<NodeFlowTuple> {

    @Override
    public void serialize(NodeFlowTuple nft, JsonGenerator jGen,SerializerProvider serializer) throws IOException, JsonProcessingException {
        serialize(nft, jGen);
    }
    
    public static void serialize(NodeFlowTuple nft, JsonGenerator jGen) throws IOException {
    
    	
    	//TO-DO need to update this with flow data    
    	
    	jGen.writeStartObject();
        jGen.writeStringField("switch", nft.getNodeId().toString());
        jGen.writeStringField("match", nft.getMatch().toString());
        jGen.writeEndObject();
        
    }

}
