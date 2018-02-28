package net.floodlightcontroller.websocket;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet; 
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

/**
 * Basic websocket java servlet to handle web-gui connections
 *
 * @author luke.sanderson@carleton.ca
 */
@SuppressWarnings("serial")
public class WSServlet extends WebSocketServlet
{
    @Override
    public void configure(WebSocketServletFactory factory)
    {
        factory.register(WebSocket.class);
        
    }
}