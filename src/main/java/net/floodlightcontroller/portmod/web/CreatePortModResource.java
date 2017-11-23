package net.floodlightcontroller.portmod.web;

import java.util.Collections;
import java.util.Map;

import org.restlet.resource.ServerResource;
import org.restlet.resource.Post;
import org.restlet.resource.Put;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreatePortModResource extends ServerResource {

	private static final Logger LOG = LoggerFactory.getLogger(CreatePortModResource.class);
	
	@Post
	@Put
	public Map<String, String> createPortMod(String json) {
		LOG.info("Got a request: " + json);
		
        return Collections.singletonMap("request", json);
	}
}
