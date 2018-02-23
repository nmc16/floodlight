package net.floodlightcontroller.websocket;


import java.io.IOException;


import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/** 
 * @ServerEndpoint gives the relative name for the end point
 */

@ServerEndpoint("/endpoint") 
public class webSocketServer {
	
	// Class logger
	private static final Logger logger = LoggerFactory.getLogger(webSocket.class);
	
	/**
     * @OnOpen allows us to intercept the creation of a new session.
     * The session class allows us to send data to the user.
     * In the method onOpen, we'll let the user know that the handshake was 
     * successful.
     */
    @OnOpen
    public void onOpen(Session session){
        logger.info(session.getId() + " has opened a connection"); 
        try {
            session.getBasicRemote().sendText("Connection Established");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
 
    /**
     * When a user sends a message to the server, this method will intercept the message
     * and allow us to react to it. For now the message is read as a String.
     */
    @OnMessage
    public void onMessage(String message, Session session){
        logger.info("Message from " + session.getId() + ": " + message);
        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
 
    /**
     * The user closes the connection.
     * 
     * Note: you can't send messages to the client from this method
     */
    @OnClose
    public void onClose(Session session){
        logger.info("Session " +session.getId()+" has ended");
    }
    
    /**
     * Sends out a warning if an error occurred.
     * 
     */
    @OnError
    public void onError(Throwable t) {
        logger.warn("onError::" + t.getMessage());
    }
}
