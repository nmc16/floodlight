package net.floodlightcontroller.websocket;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class sessionListener {
	private static sessionListener instance;
	private static final Logger logger = LoggerFactory.getLogger(sessionListener.class);

    /**
     *Singleton approach to the class
     */
    private sessionListener() {
    }

    static {
        instance = new sessionListener();
    }

    //to return one instance
    public static sessionListener getInstance() {
        return instance;
    }

    private List<Session> sessions = new ArrayList<Session>();	
	public void addActiveSession(Session session)
    {
        this.sessions.add(session);
        logger.warn("Session added aayyyyyyy");
    }
	public void removeActiveSession(Session session)
    {
        this.sessions.remove(session);
        logger.warn("Session removed");
    }
	public List<Session> getActiveSession()
    {
        return this.sessions;
    }
    
}