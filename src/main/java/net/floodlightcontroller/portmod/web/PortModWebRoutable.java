package net.floodlightcontroller.portmod.web;

import net.floodlightcontroller.restserver.RestletRoutable;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

public class PortModWebRoutable implements RestletRoutable {

	@Override
	public Restlet getRestlet(Context context) {

		Router router = new Router(context);
		
		router.attach("/create/{switch}/{port}/json", CreatePortModResource.class);
		router.attach("/retrieve/{switch}/{port}/json", RetrievePortModResource.class);
		// TODO: router.attach("/history/{switch}/{port}/json", HistoryPortModResource.class);
		return router;
	}

	@Override
	public String basePath() {
		return "/wm/portmod";
	}
}
