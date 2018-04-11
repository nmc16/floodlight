package net.floodlightcontroller.core.web.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.projectfloodlight.openflow.types.U64;

import java.io.IOException;

/**
 * Serializer for the {@link U64} class.
 *
 * @author nicolas.mccallum@carleton.ca
 */
public class U64Serializer extends JsonSerializer<U64> {

    @Override
    public void serialize(U64 u64,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException, JsonProcessingException {

        jsonGenerator.writeNumber(u64.getValue());
    }
}
