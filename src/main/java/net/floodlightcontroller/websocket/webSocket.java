package net.floodlightcontroller.websocket;

import java.util.Collection; 
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.portmod.PortModManager;
import net.floodlightcontroller.storage.IStorageSourceListener;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;


public class webSocket implements IStorageSourceListener, IFloodlightModule {
	
	// Class logger
	private static final Logger logger = LoggerFactory.getLogger(webSocket.class);
	
	protected IFloodlightProviderService floodlightProvider;
	protected Set<Long> macAddresses;


	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		Collection<Class<? extends IFloodlightService>> l =
		        new ArrayList<Class<? extends IFloodlightService>>();
		    l.add(IFloodlightProviderService.class);
		    return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		// TODO Auto-generated method stub
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
	    macAddresses = new ConcurrentSkipListSet<Long>();
	
	    logger.warn("BINGO BINGO!");
	    logger.info("Test test TESTTTTTTTTTTTTest test TESTTTTTTTTTTT");
	    

	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		
		 logger.info("Test test TESTTTTTTTTTTTTest test TESTTTTTTTTTTT");
		// TODO Auto-generated method stub
		//floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		

	}

	@Override
	public void rowsModified(String tableName, Set<Object> rowKeys) {
		// TODO Auto-generated method stub
		logger.info("Test test TESTTTTTTTTTTTTest test TESTTTTTTTTTTT");
	}

	@Override
	public void rowsDeleted(String tableName, Set<Object> rowKeys) {
		// TODO Auto-generated method stub
		 logger.warn("BINGO BINGO!");
		 logger.info("Test test TESTTTTTTTTTTTTest test TESTTTTTTTTTTT");

	}

}
