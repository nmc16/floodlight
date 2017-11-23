package net.floodlightcontroller.portmod.web;

import net.floodlightcontroller.restserver.RestletRoutable;
import net.floodlightcontroller.portmod.web.CreatePortModResource;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

public class PortModWebRoutable implements RestletRoutable {

	@Override
	public Restlet getRestlet(Context context) {
		// TODO Auto-generated method stub
		
		Router router = new Router(context);
		
		router.attach("/create/{switch}/{port}/json", CreatePortModResource.class);
		return router;
	}

	@Override
	public String basePath() {
		// TODO Auto-generated method stub
		return "/wm/portmod";
	}

}
