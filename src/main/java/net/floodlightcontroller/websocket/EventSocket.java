package net.floodlightcontroller.websocket;

import java.io.IOException; 
import java.util.Iterator;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.floodlightcontroller.websocket.webSocket;

/**
 * Websocket class that handles connections between client and server
 *
 * @author luke.sanderson@carleton.ca
 */
public class EventSocket extends WebSocketAdapter
{
	//private List<Session> sessions = new ArrayList<Session>();	
	//public sessionListener activeSessions = sessionListener.getInstance();
	public webSocket activeSessions = webSocket.getInstance();

	private Session currSess;
    @Override
    public void onWebSocketConnect(Session sess)
    {
        super.onWebSocketConnect(sess);
        //System.out.println("Socket Connected: " + sess);
        //System.out.println(sess.getRemoteAddress());
        currSess = sess;
        activeSessions.addActiveSession(currSess);
       
        try {
			sess.getRemote().sendString("Connection established to websocket");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    @Override
    public void onWebSocketText(String message)
    {
        super.onWebSocketText(message);
        
        System.out.println("JSON received: " + message);

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();

        //read JSON like DOM Parser
        JsonNode rootNode = null;
		try {
			rootNode = objectMapper.readTree(message);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        JsonNode subscribeNode = rootNode.path("subscribe");
        Iterator<JsonNode> elements = subscribeNode.elements();
        while(elements.hasNext()){
        	JsonNode subscribed = elements.next();
        	activeSessions.registerTable(currSess, subscribed.asText());
        	//System.out.println("subscribe = "+subscribed.asText());
        }
    }
    
    @Override
    public void onWebSocketClose(int statusCode, String reason)
    {
        super.onWebSocketClose(statusCode,reason);
        //System.out.println("Socket Closed: [" + statusCode + "] " + reason);
        activeSessions.removeActiveSession(currSess);
        
    }
    
    @Override
    public void onWebSocketError(Throwable cause)
    {
        super.onWebSocketError(cause);
        cause.printStackTrace(System.err);
    }
}