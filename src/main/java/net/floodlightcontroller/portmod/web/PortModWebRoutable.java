package net.floodlightcontroller.portmod.web;

import net.floodlightcontroller.restserver.RestletRoutable;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

/**
 * Handles routing of REST requests from URLs to their respective handler classes for the port modification
 * package.
 *
 * Provides REST interfaces for creating port modifications, retrieving currently applied port modifications,
 * and retrieving historically applied port modifications.
 *
 * @author nicolas.mccallum@carleton.ca
 */
public class PortModWebRoutable implements RestletRoutable {

	@Override
	public Restlet getRestlet(Context context) {

		Router router = new Router(context);
		router.attach("/create/{switch}/{port}/json", CreatePortModResource.class);
		router.attach("/retrieve/{switch}/{port}/json", RetrievePortModResource.class);
		router.attach("/history/{switch}/{port}/json", PortModHistoryResource.class);

		return router;
	}

	@Override
	public String basePath() {
		return "/wm/portmod";
	}
}
