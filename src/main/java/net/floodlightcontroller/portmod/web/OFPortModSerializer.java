package net.floodlightcontroller.portmod.web;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.protocol.OFPortMod;

import java.io.IOException;

/**
 * Serializer for the {@link OFPortMod} class. Extracts the important information to return to the user request.
 *
 * Serializes the class into a JSON String with the following format:
 * {
 *     "version": "OF_XX",
 *     "config" : ["config1", "config2", ...],
 *     "mask"   : ["config1", "config2", ...],
 *     "mac"    : "XX:XX:XX:XX:XX:XX",
 *     "port"   : 1
 * }
 *
 * @author nicolas.mccallum@carleton.ca
 */
public class OFPortModSerializer extends JsonSerializer<OFPortMod> {

    // Define constants for field names
    private static final String VERSION = "version";
    private static final String CONFIG = "config";
    private static final String MASK = "mask";
    private static final String MAC = "mac";
    private static final String PORT = "port";

    @Override
    public void serialize(OFPortMod ofPortMod,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException, JsonProcessingException {

        /*
         * Write the string representation of the version because the integer version will be unreadable to a
         * normal user (due to wire representation)
         */
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(VERSION, ofPortMod.getVersion().toString());

        // Write the current port configurations as an array of strings
        jsonGenerator.writeArrayFieldStart(CONFIG);

        for (OFPortConfig config : ofPortMod.getConfig()) {
            jsonGenerator.writeString(config.toString());
        }

        jsonGenerator.writeEndArray();

        // Write the mask as an array of strings
        jsonGenerator.writeArrayFieldStart(MASK);

        for (OFPortConfig config : ofPortMod.getMask()) {
            jsonGenerator.writeString(config.toString());
        }

        jsonGenerator.writeEndArray();

        // Write the MAC address of the machine we sent the port modification to
        jsonGenerator.writeStringField(MAC, ofPortMod.getHwAddr().toString());

        // Write the port number
        jsonGenerator.writeNumberField(PORT, ofPortMod.getPortNo().getPortNumber());

        // End the JSON string
        jsonGenerator.writeEndObject();
        jsonGenerator.close();
    }
}
