package net.floodlightcontroller.websocket;

import java.io.IOException; 
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import net.floodlightcontroller.storage.IStorageSourceService;
import net.floodlightcontroller.websocket.webSocket;

public class EventSocket extends WebSocketAdapter
{
	
	//public sessionListener activeSessions = sessionListener.getInstance();
	public webSocket activeSessions = webSocket.getInstance();
	
	/*
	@Inject
	public EventSocket(IStorageSourceService storageSourceService){
		this.storageSourceService = storageSourceService;
		
	}
	*/
	private Session currSess;
    @Override
    public void onWebSocketConnect(Session sess)
    {
        super.onWebSocketConnect(sess);
        System.out.println("Socket Connected: " + sess);
        System.out.println(sess.getRemoteAddress());
        currSess = sess;
        activeSessions.addActiveSession(currSess);
       
        try {
			sess.getRemote().sendString("HEYA muthafucka");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
  
    }
    
    @Override
    public void onWebSocketText(String message)
    {
        super.onWebSocketText(message);
        
        
        System.out.println("Received TEXT message: " + message);
    }
    
    @Override
    public void onWebSocketClose(int statusCode, String reason)
    {
        super.onWebSocketClose(statusCode,reason);
        System.out.println("Socket Closed: [" + statusCode + "] " + reason);
        activeSessions.removeActiveSession(currSess);
        
    }
    
    @Override
    public void onWebSocketError(Throwable cause)
    {
        super.onWebSocketError(cause);
        cause.printStackTrace(System.err);
    }
}