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

import net.floodlightcontroller.restserver.IRestApiService;
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
 * TODO: Check to make sure all threads are finished correctly.
 * TODO: Refactor for easier reading.
 *
 * @author luke.sanderson@carleton.ca
 */
public class webSocket implements IStorageSourceListener, IFloodlightModule {
	
	// Class logger
	private static final Logger logger = LoggerFactory.getLogger(webSocket.class);
	
	private static webSocket instance;
	protected IStorageSourceService storageSourceService;
	protected IRestApiService restApiService;
	private HashMap<Session, List<String>> activeTables= new HashMap<Session, List<String>>();
	private List<Session> sessions = new ArrayList<Session>();	
	
	//public sessionListener activeSessions = sessionListener.getInstance();
	//private static final String TOPOLOGY_TABLE_NAME = "controller_firewallrules";


    /** Static 'instance' method */
    public static webSocket getInstance() {
        return instance;
    }

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		
		/*
		Collection<Class<? extends IFloodlightService>> l =
		        new ArrayList<Class<? extends IFloodlightService>>();
		    l.add(IStorageSourceService.class);
		    l.add(IRestApiService.class);
		    return l;
		    */

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
		l.add(IRestApiService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		storageSourceService = context.getServiceImpl(IStorageSourceService.class);
		restApiService = context.getServiceImpl(IRestApiService.class);
	    logger.warn("made it to init");
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		
		
		instance = this; //singleton to assure one is being called
		logger.info("made it before startup");
		
		logger.warn("begginning main");
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(8111);
		server.addConnector(connector);
	     

		// Setup server endpoint for this application at "/"
		ServletContextHandler serverEndpoint = new ServletContextHandler(ServletContextHandler.SESSIONS);
		serverEndpoint.setContextPath("/");
		server.setHandler(serverEndpoint);
	        
	        // Add a websocket to a specific path spec
	        ServletHolder holderEvents = new ServletHolder("ws-events", EventServlet.class);
	        serverEndpoint.addServlet(holderEvents, "/events/*");
	       
	        try
	        {
	            server.start();
	            logger.warn("server is up");
	            
	            //server.dump(System.err);
	            //server.join();
	            //logger.warn("server joined");
	        }
	        catch (Throwable t)
	        {
	        	logger.warn("server did not start");
	            t.printStackTrace(System.err);
	        }
	}
	

	//Register for storage updates on the tables requested by web-gui
	public HashMap<Session, List<String>> registerTable(Session session, String tableName){
		
		try {
		List<String> tableNames = new ArrayList<String>();
		storageSourceService.addListener(tableName, this);
		logger.info("Added listener: "+ tableName);
	
		if(activeTables.containsKey(session)){
			tableNames = activeTables.get(session);
			tableNames.add(tableName);
			activeTables.put(session, tableNames);
		}else{
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
		
		//if (tableName.equals(TOPOLOGY_TABLE_NAME)) {
			logger.info("Table: " + tableName + " was modified");
			readTableFromStorage(tableName);
			return;
		//}
		//logger.info("rows modified for table");
	}

	@Override
	public void rowsDeleted(String tableName, Set<Object> rowKeys) {
		//if (tableName.equals(TOPOLOGY_TABLE_NAME)) {
			logger.warn(tableName);
			logger.info("Table: " + tableName + " had rows deleted");
			readTableFromStorage(tableName);
			return;
		//}
		 //logger.info("rows deleted2");
	}
	
	protected void readTableFromStorage(String tableName) {
		IResultSet tableResult = storageSourceService.executeQuery(tableName, null, null, null); //TODO configure the table results to send to web-gui
		logger.info("number of sessions: " + getActiveSession().size());
		
		Session sess = getActiveSession().get(0);
		try {
			sess.getRemote().sendString("Yo the table was changed eh");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	public void addActiveSession(Session session)
	{
		this.sessions.add(session);
		logger.warn("Session has been added");
	}
	public void removeActiveSession(Session session)
	{
		logger.warn("removing listeners for: "+activeTables.get(session));
		if(activeTables.containsKey(session)){
			List<String> tablesToRemove = activeTables.get(session);
			for(String table : tablesToRemove){
				storageSourceService.removeListener(table, this); //remove the listeners when the connection is closed
				logger.warn("removed: " + table);
			}
		}
			//remove session from lists				
		activeTables.remove(session);
	    this.sessions.remove(session);
	    logger.warn("Session removed");
	}
	public List<Session> getActiveSession()
	{
		return this.sessions;
	}
}
