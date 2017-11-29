package net.floodlightcontroller.portmod.web;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.projectfloodlight.openflow.protocol.OFPortConfig;

import java.io.IOException;

public class PortModRequest {
    @JsonDeserialize(using=StupidValueDeserializer.class)
    private String config;

    public PortModRequest(String config) {
        this.config = config;
    }

    public PortModRequest() {}


    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }
}
