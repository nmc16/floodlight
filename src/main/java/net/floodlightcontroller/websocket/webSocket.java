package net.floodlightcontroller.websocket;

import java.util.Collection; 
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IShutdownService;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.debugcounter.IDebugCounterService;

import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.portmod.IPortModService;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.storage.IResultSet;
import net.floodlightcontroller.storage.IStorageSourceListener;
import net.floodlightcontroller.storage.IStorageSourceService;
import net.floodlightcontroller.storage.StorageException;
import net.floodlightcontroller.threadpool.IThreadPoolService;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;


public class webSocket implements IStorageSourceListener, IFloodlightModule {
	
	// Class logger
	private static final Logger logger = LoggerFactory.getLogger(webSocket.class);
	
	
	protected IStorageSourceService storageSourceService;


	private static final String TOPOLOGY_TABLE_NAME = "controller_firewallrules";
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		
		/*
		Collection<Class<? extends IFloodlightService>> l =
		        new ArrayList<Class<? extends IFloodlightService>>();
		    l.add(IStorageSourceService.class);
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
		return l;
		
		
		
		
		
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		
	
		storageSourceService = context.getServiceImpl(IStorageSourceService.class);

	
	    logger.warn("made it to init");

	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		
		
		
		logger.info("made it before startup");
		// Register for storage updates for the switch table
			try {
			
				storageSourceService.addListener(TOPOLOGY_TABLE_NAME, this);
				logger.info("inside startup");
			} catch (StorageException ex) {
				logger.error("Error in installing listener for "
						+ "switch table {}");
			}

	}
	
	//*********************
	//   Storage Listener
	//*********************
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
		
		if (tableName.equals(TOPOLOGY_TABLE_NAME)) {
			readTopologyConfigFromStorage();
			return;
		}
		logger.info("rows modified for table");
	}

	@Override
	public void rowsDeleted(String tableName, Set<Object> rowKeys) {
		if (tableName.equals(TOPOLOGY_TABLE_NAME)) {
			logger.warn(tableName);
			readTopologyConfigFromStorage();
			logger.info("rows deleted1");
			return;
		}

		 logger.info("rows deleted2");

	}
	
	protected void readTopologyConfigFromStorage() {
		IResultSet topologyResult = storageSourceService.executeQuery(TOPOLOGY_TABLE_NAME,
				null, null,
				null);
		logger.warn("read topology");
		

	}

}
