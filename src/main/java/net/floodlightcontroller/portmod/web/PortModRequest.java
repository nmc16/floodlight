package net.floodlightcontroller.portmod.web;

import org.projectfloodlight.openflow.protocol.OFPortConfig;

import java.util.Map;

/**
 * REST request for port modifications that holds the requested configuration applications.
 *
 * @author nicolas.mccallum@carleton.ca
 */
public class PortModRequest {

    private Map<OFPortConfig, Boolean> configs;

    public PortModRequest() {}

    public PortModRequest(Map<OFPortConfig, Boolean> configs) {
        this.configs = configs;
    }

    public Map<OFPortConfig, Boolean> getConfigs() {
        return configs;
    }

    public void setConfig(Map<OFPortConfig, Boolean> configs) {
        this.configs = configs;
    }
}
