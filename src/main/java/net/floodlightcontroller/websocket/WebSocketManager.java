package net.floodlightcontroller.websocket;

import java.io.IOException;  
import java.util.Collection;    
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.server.Server; 
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.api.Session;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;


import net.floodlightcontroller.storage.IResultSet;
import net.floodlightcontroller.storage.IStorageSourceListener;
import net.floodlightcontroller.storage.IStorageSourceService;
import net.floodlightcontroller.storage.StorageException;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;


/**
 * Module starts a Jetty server, and registers storage listeners for real-time table changes
 *
 * @author luke.sanderson@carleton.ca
 */
public class WebSocketManager implements IStorageSourceListener, IFloodlightModule {
	
	protected static final Logger logger = LoggerFactory.getLogger(WebSocketManager.class);
	protected FloodlightModuleContext fmlContext;
	private static WebSocketManager instance;
	private static int httpWSPort;
	protected IStorageSourceService storageSourceService;
	private HashMap<Session, List<String>> activeTables= new HashMap<Session, List<String>>();
	private List<Session> sessions = new ArrayList<Session>();	
	
	
    /** Method to keep instance static **/
    public static WebSocketManager getInstance() {
        return instance;
    }
    
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		
		
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
	
		l.add(IStorageSourceService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		storageSourceService = context.getServiceImpl(IStorageSourceService.class);
		this.fmlContext = context;
		Map<String, String> configOptions = context.getConfigParams(this);
		httpWSPort = Integer.parseInt(configOptions.get("httpWSPort"));
		
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		
		instance = this; //singleton to assure one is being called
	
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(httpWSPort);
		server.addConnector(connector);
	     

		// Setup server endpoint for this application at "/"
		ServletContextHandler serverEndpoint = new ServletContextHandler(ServletContextHandler.SESSIONS);
		serverEndpoint.setContextPath("/");
		server.setHandler(serverEndpoint);
	        
	        // Add a webSocket to a specific path spec
	        ServletHolder holderEvents = new ServletHolder("ws-events", WSServlet.class);
	        serverEndpoint.addServlet(holderEvents, "/events/*");
	       
	        try
	        {
	            server.start();
	            logger.info("Jetty server is up for websocket connections on port: " + httpWSPort);
	        }
	        catch (Throwable t)
	        {
	        	logger.warn("server did not start");
	        	logger.error(t.getLocalizedMessage());
	        }
	}
	

	/**
	 * Register for storage updates on the tables requested by web-gui
	 * @param session		Session we are looking at
	 * @param tableName		Tables that are associated with the session, to be registered
	 * @return				Returns a HashMap that contains the sessions with their associated tables
	 */
	public HashMap<Session, List<String>> registerTable(Session session, String tableName) {
		
		try {
			List<String> tableNames = new ArrayList<String>();
			storageSourceService.addListener(tableName, this);
			logger.info("Added listener: "+ tableName);
	
			if(activeTables.containsKey(session)){
				tableNames = activeTables.get(session);
				tableNames.add(tableName);
				activeTables.put(session, tableNames);
			}else {
				tableNames.add(tableName);
				activeTables.put(session, tableNames);
			}
		
		} catch (StorageException ex) {
			logger.error("Error in installing listener for "
					+ "switch table {}");
		}
		return activeTables;
	}
	

	/**
	* Sets the IStorageSource to use for Topology
	*
	* @param storageSource
	*            the storage source to use
	*/
	public void setStorageSource(IStorageSourceService storageSourceService) {
		this.storageSourceService = storageSourceService;
	}	
	
	/**
	 * Gets the storage source for this ITopology
	 *
	 * @return The IStorageSource ITopology is writing to
	 */
	public IStorageSourceService getStorageSource() {
		return storageSourceService;
	}
	
	@Override
	public void rowsModified(String tableName, Set<Object> rowKeys) {
			logger.debug("Table: " + tableName + " was modified");
			readTableFromStorage(tableName);
	}

	@Override
	public void rowsDeleted(String tableName, Set<Object> rowKeys) {
			logger.debug("Table: " + tableName + " had rows deleted");
			readTableFromStorage(tableName);
	}
	
	/**
	 * When a table is updated, a query is performed then the result 
	 * is sent out over the websocket to the relevant session client
	 * 
	 * @param tableName
	 * 			the name of the updated table to query
	 */
	protected void readTableFromStorage(String tableName) {
		IResultSet tableResult = storageSourceService.executeQuery(tableName, null, null, null); 
		if(tableResult.next()) {
			logger.debug(tableResult.getRow().toString());
		}else {
			logger.debug("No row found!");
			return;
		}
		
		Session sess = getActiveSession().get(0); //Currently there is only ever one session active, however this code is here for future improvements
		try {
			sess.getRemote().sendString(tableResult.getRow().toString()); //Send table data out to client
		} catch (IOException e) {
			logger.warn("Error sending updated table data to client");
			logger.error(e.getLocalizedMessage());
		}
	}
	
	/**
	 * Method to keep track of active websocket sessions on server
	 * 
	 * @param session
	 * 			session to add to list
	 */
	public void addActiveSession(Session session)
	{
		this.sessions.add(session);
		logger.info("Session has been added");
	}
	
	/**
	 * Removes active sessions from both the registered tables and the list that 
	 * keeps track of current sessions. This makes sure that when you close the connection that we are not still listening
	 * for irrelevant tables.
	 * 
	 * @param session
	 * 			Session that is being closed
	 */
	public void removeActiveSession(Session session)
	{
		logger.info("removing listeners for: "+activeTables.get(session));
		if(activeTables.containsKey(session)) {
			List<String> tablesToRemove = activeTables.get(session);
			for(String table : tablesToRemove){
				storageSourceService.removeListener(table, this); //remove the listeners when the connection is closed
				logger.info("removed: " + table);
			}
		}
		
		//remove session from lists				
		activeTables.remove(session);
	    this.sessions.remove(session);
	    logger.info("Session removed");
	}
	
	//Return how many active sessions on websockets
	public List<Session> getActiveSession()
	{
		return this.sessions;
	}
}
