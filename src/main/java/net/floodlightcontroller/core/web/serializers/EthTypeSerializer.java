package net.floodlightcontroller.core.web.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.projectfloodlight.openflow.types.EthType;

import java.io.IOException;

public class EthTypeSerializer extends JsonSerializer<EthType> {
    @Override
    public void serialize(EthType ethType,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException, JsonProcessingException {

        jsonGenerator.writeNumber(ethType.getValue());

    }
}
