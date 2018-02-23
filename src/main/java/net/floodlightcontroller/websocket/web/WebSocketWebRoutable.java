package net.floodlightcontroller.websocket.web;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.websocket.web.handshakeResource;
import net.floodlightcontroller.restserver.RestletRoutable;

public class WebSocketWebRoutable implements RestletRoutable {
	@Override
	public Restlet getRestlet(Context context) {

		Router router = new Router(context);
		router.attach("/realtime/json", handshakeResource.class);

		return router;
	}

	@Override
	public String basePath() {
		return "/wm/websocket";
	}
}

