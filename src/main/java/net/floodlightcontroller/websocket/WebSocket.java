package net.floodlightcontroller.websocket;

import java.io.IOException;  
import java.util.Iterator;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.floodlightcontroller.websocket.WebSocketManager;

/**
 * WebSocket class that handles connections between client and server
 *
 * @author luke.sanderson@carleton.ca
 */
public class WebSocket extends WebSocketAdapter
{

	private static final Logger logger = LoggerFactory.getLogger(WebSocket.class); 	// Class logger
	private static final boolean DEBUG = true; //Set to false to disable excessive logging
	public WebSocketManager activeSessions = WebSocketManager.getInstance(); //Singleton architecture so we do not reference the wrong server
	private Session currSess;

	
	
    @Override
    public void onWebSocketConnect(Session sess)
    {
        super.onWebSocketConnect(sess);
        currSess = sess;
        activeSessions.addActiveSession(currSess);
       
        try {
			sess.getRemote().sendString("Connection established to websocket");
		} catch (IOException e) {
			logger.error("Could not reach client websocket");
			if(DEBUG){e.printStackTrace();}
		}
    }
    
    @Override
    public void onWebSocketText(String message)
    {
        super.onWebSocketText(message);
        
        if(DEBUG){System.out.println("JSON received: " + message);}

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();

        //attempt to parse JSON received from client
        JsonNode rootNode = null;
		try {
			rootNode = objectMapper.readTree(message);
		} catch (JsonProcessingException e) {
			logger.error("Error while parsing JSON received from client");
			if(DEBUG){e.printStackTrace();}
		} catch (IOException e) {
			logger.error("Error while parsing JSON received from client");
			if(DEBUG){e.printStackTrace();}
		}
		
		//Parses the JSON for the subscribe object, then registers the tables located inside
        JsonNode subscribeNode = rootNode.path("subscribe");
        Iterator<JsonNode> elements = subscribeNode.elements();
        while(elements.hasNext()){
        	JsonNode subscribed = elements.next();
        	activeSessions.registerTable(currSess, subscribed.asText());
        
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
        if(DEBUG){cause.printStackTrace(System.err);}
    }
}