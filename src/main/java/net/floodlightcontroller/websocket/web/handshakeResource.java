package net.floodlightcontroller.websocket.web;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class handshakeResource extends ServerResource {
	
	
	@Get("json")
	public String createWebsocketConnection(String json){
		return json;
		
	}

}
