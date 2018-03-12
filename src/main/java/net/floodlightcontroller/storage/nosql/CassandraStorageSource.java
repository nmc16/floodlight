package net.floodlightcontroller.storage.nosql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.perfmon.IPktInProcessingTimeService;
import net.floodlightcontroller.storage.IStorageSourceCassandraService;

import net.floodlightcontroller.storage.SynchronousExecutorService;
import com.codahale.metrics.*;
import com.google.common.util.concurrent.FutureFallback;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;
import com.datastax.driver.core.querybuilder.QueryBuilder;

public class CassandraStorageSource extends NoSqlStorageSource {
	private static Cluster cluster;
	private static Session session;
	IPktInProcessingTimeService pktinProcessingTime;
	//opens the connection to the DB
	private static void openCassConnection(){
		cluster = Cluster.builder().addContactPoint("127.0.0.1").withRetryPolicy(DefaultRetryPolicy.INSTANCE).
			withLoadBalancingPolicy(new TokenAwarePolicy(new DCAwareRoundRobinPolicy())).build();
		session = cluster.connect("testdatabase");
	}
	
	//executes the CQL commands
	private static void executeCommand(String cmd){
		session.execute(cmd);
	}

	public void setPktinProcessingTime(
	    IPktInProcessingTimeService pktinProcessingTime) {
	    this.pktinProcessingTime = pktinProcessingTime;
	}
	
	public static void deleteTable(String tableName){
		openCassConnection();
		
		String a = "Drop table " + tableName;
		executeCommand(a);
		
		cluster.close();
	}
		
	public void deleteRows(String tableName, Set<Object> rowKeys){
		openCassConnection();
		
		String primeKey = cluster.getMetadata().getKeyspace(session.getLoggedKeyspace())
				.getTable("users").getPrimaryKey().get(0).getName();
		
		QueryBuilder.delete().from(tableName).where(QueryBuilder.eq(primeKey, rowKeys));
		
		cluster.close();
	}
	//creates a table with primary key being the first column of type cls [must be string or int] 
	public void createTable(String tableName, String primaryKeyName, Class<?> cls)
	{
		openCassConnection();
		
		String a ="CREATE TABLE " +tableName+ " (" +primaryKeyName+ " " +cls+ ", PRIMARY KEY(" +primaryKeyName+ "))"; 
		executeCommand(a);
		
		cluster.close();
	}
	//creates a column in the given table with given name and type	
	public static void createColumn(String tableName, String columnName, Class<?> cls){
		openCassConnection();
		
		String a = "ALTER TABLE " +tableName+ " ADD " +columnName+ " " +cls;
		executeCommand(a);
		
		cluster.close();
	}
	@Override
	//gets all rows for given table name ?is it suppose to print them or what?
	protected Collection<Map<String,Object>> getAllRows(String tableName, String[] colName){
		openCassConnection();
		
		Collection<Map<String,Object>> allRows = new ArrayList<Map<String,Object>>();
		Statement selQuery = QueryBuilder.select(colName).from(tableName);
		ResultSet results = session.execute(selQuery);
		List<Row> rows = results.all();
		
		for(int i=0; i<rows.size(); i++){
			Row row = rows.get(i);
			//Map<String,Object> map = new HashMap<String,Object>();
			for(int j=0; j<colName.length; j++){
				Map<String,Object> map = new HashMap<String,Object>();
				map.put(colName[j], row.getObject(j));
				allRows.add(map);
			}
			
		}
		cluster.close();
		return allRows;
	}
	@Override
	//for now only going to be strings or ints/floats and stuff
	protected Map<String,Object> getRow (String tableName, String[] colName, Object primaryKeyValue){
		openCassConnection();
		
		String primeKey = cluster.getMetadata().getKeyspace(session.getLoggedKeyspace())
					.getTable("users").getPrimaryKey().get(0).getName();
		
		Map<String,Object> map = new HashMap<String,Object>();
		
		Statement selQuery = QueryBuilder.select(colName).from(tableName)
					.where(QueryBuilder.contains(primeKey, primaryKeyValue));
		
		ResultSet results = session.execute(selQuery);
		List<Row> row = results.all();
		
		for(int i = 0; i<colName.length; i++){
			 map.put(colName[i], row.get(i));
		 }
		cluster.close();
		return map;
	
	}
	@Override
	protected List<Map<String,Object>> executeEqualityQuery(String tableName,
            String[] columnNameList, String predicateColumnName, Comparable<?> value) {
		openCassConnection();
		
		List<Map<String,Object>> equalityQ = new ArrayList<Map<String,Object>>();
		
		String primeKey = cluster.getMetadata().getKeyspace(session.getLoggedKeyspace())
				.getTable(tableName).getPrimaryKey().get(0).getName();
		
		Statement equalityQuery= QueryBuilder.select(columnNameList).from(tableName).where(QueryBuilder.eq(predicateColumnName,value))
		        .orderBy(QueryBuilder.asc(primeKey));
		
		ResultSet results= session.execute(equalityQuery);
		
		List<Row> rows = results.all();
		
		for(int i=0; i<rows.size(); i++){
			Row row = rows.get(i);
			for(int j=0; j<columnNameList.length; j++){
				Map<String,Object> map = new HashMap<String,Object>();
				map.put(columnNameList[j], row.getObject(j));
				equalityQ.add(map);
			}
		}
		cluster.close();
		return equalityQ;	
	}
	@Override
	protected List<Map<String,Object>> executeRangeQuery(String tableName,
            String[] columnNameList, String predicateColumnName,
            Comparable<?> startValue, boolean startInclusive, Comparable<?> endValue, boolean endInclusive){
		openCassConnection();
		
		
		List<Map<String,Object>> rangeQ = new ArrayList<Map<String,Object>>();
		String primeKey = cluster.getMetadata().getKeyspace(session.getLoggedKeyspace())
				.getTable(tableName).getPrimaryKey().get(0).getName();
		
		// <= x >=
		if (startInclusive && endInclusive){
			Statement rangeQuery= QueryBuilder.select(columnNameList).from(tableName).where(QueryBuilder.gte(predicateColumnName,startValue))
			        .and(QueryBuilder.lte(predicateColumnName,endValue)).orderBy(QueryBuilder.asc(primeKey));
			
			ResultSet results= session.execute(rangeQuery);
			
			List<Row> rows = results.all();
			
			for(int i=0; i<rows.size(); i++){
				Row row = rows.get(i);
				for(int j=0; j<columnNameList.length; j++){
					Map<String,Object> map = new HashMap<String,Object>();
					map.put(columnNameList[j], row.getObject(j));
					rangeQ.add(map);
				}
			}
		}	
		// <= x >
		if (startInclusive && !endInclusive){
			Statement rangeQuery= QueryBuilder.select(columnNameList).from(tableName).where(QueryBuilder.gte(predicateColumnName,startValue))
			        .and(QueryBuilder.lt(predicateColumnName,endValue)).orderBy(QueryBuilder.asc(primeKey));
			
			ResultSet results= session.execute(rangeQuery);
			
			List<Row> rows = results.all();
			
			for(int i=0; i<rows.size(); i++){
				Row row = rows.get(i);
				for(int j=0; j<columnNameList.length; j++){
					Map<String,Object> map = new HashMap<String,Object>();
					map.put(columnNameList[j], row.getObject(j));
					rangeQ.add(map);
				}
			}
		}
		
		// < x >=
		if (!startInclusive && endInclusive){
			Statement rangeQuery= QueryBuilder.select(columnNameList).from(tableName).where(QueryBuilder.gt(predicateColumnName,startValue))
			        .and(QueryBuilder.lte(predicateColumnName,endValue)).orderBy(QueryBuilder.asc(primeKey));
			
			ResultSet results= session.execute(rangeQuery);
			
			List<Row> rows = results.all();
			
			for(int i=0; i<rows.size(); i++){
				Row row = rows.get(i);
				for(int j=0; j<columnNameList.length; j++){
					Map<String,Object> map = new HashMap<String,Object>();
					map.put(columnNameList[j], row.getObject(j));
					rangeQ.add(map);
				}
			}	
		}
		
		// < x >
		if (!startInclusive && !endInclusive){
			Statement rangeQuery= QueryBuilder.select(columnNameList).from(tableName).where(QueryBuilder.gt(predicateColumnName,startValue))
			        .and(QueryBuilder.lt(predicateColumnName,endValue)).orderBy(QueryBuilder.asc(primeKey));
			
			ResultSet results= session.execute(rangeQuery);
			
			List<Row> rows = results.all();
			
			for(int i=0; i<rows.size(); i++){
				Row row = rows.get(i);
				for(int j=0; j<columnNameList.length; j++){
					Map<String,Object> map = new HashMap<String,Object>();
					map.put(columnNameList[j], row.getObject(j));
					rangeQ.add(map);
				}
			}	
		}
		cluster.close();
		return rangeQ;
	}
		
	
	@Override
	//make sure primary key is first 
	protected void insertRows(String tableName, List<Map<String,Object>> lst){
		openCassConnection();
		
		List<Object> values = new ArrayList<Object>();
		List<String> names = new ArrayList<String>();
		for (Map<String, Object> map : lst) {
		    for (Map.Entry<String, Object> entry : map.entrySet()) {
		        values.add(entry.getValue());
		        names.add(entry.getKey());
		    }
		}
		String n ="";
		String v ="";
		for(int i=0; i<names.size(); i++){
			if(i == names.size() - 1){
				n = n + " "+names.get(i);
			}
			else{
			n = n + " "+names.get(i)+ ",";
			}
			
			if (values.get(i).getClass() == n.getClass()){
				if(i == names.size() - 1){
					v = v + " '"+values.get(i);
				}
				else{
				v = v + " '"+values.get(i)+ "',";
				}
				
			}
			else {
				if(i == names.size() - 1){
					v = v + " "+values.get(i);
				}
				
				else{
				v = v + " "+values.get(i)+ ",";
				}
			}
		}
		String a ="INSERT INTO "+tableName+" (" + n + ") VALUES ("+ v + ")";
		executeCommand(a);
		
		cluster.close();
	}
	@Override
	protected void updateRows(String tableName, Set<Object> rowKeys, Map<String,Object> updateColumnMap){
		openCassConnection();
		
		String primeKey = cluster.getMetadata().getKeyspace(session.getLoggedKeyspace())
				.getTable(tableName).getPrimaryKey().get(0).getName();
		
		List<Object> values = new ArrayList<Object>();
		List<String> names = new ArrayList<String>();

		for (Map.Entry<String, Object> entry : updateColumnMap.entrySet()) {
			values.add(entry.getValue());
		    names.add(entry.getKey());
		}
		int i = 0;
		for(String column: names){
		Statement exampleQuery = QueryBuilder.update(tableName).with(QueryBuilder.set(column, values.get(i)))
		        .where(QueryBuilder.eq(primeKey, rowKeys));
		i++;
		session.execute(exampleQuery);
		}
		cluster.close();
	}

	@Override
	protected void updateRowsImpl(String tableName,
			List<Map<String, Object>> rows) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void deleteRowsImpl(String tableName, Set<Object> rowKeys) {
		// TODO Auto-generated method stub
		
	}
	
	//Floodlight modules
	
	@Override
    public void startUp(FloodlightModuleContext context) {
        super.startUp(context);
        executorService = new SynchronousExecutorService();
    }
	
	@Override
    public void init(FloodlightModuleContext context) throws net.floodlightcontroller.core.module.FloodlightModuleException {
    	super.init(context);
    };
    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l = 
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IStorageSourceCassandraService.class);
		return l;
	}

    @Override
    public Map<Class<? extends IFloodlightService>,
               IFloodlightService> getServiceImpls() {
        Map<Class<? extends IFloodlightService>,
            IFloodlightService> m =
                new HashMap<Class<? extends IFloodlightService>,
                            IFloodlightService>();
        m.put(IStorageSourceCassandraService.class, this);
        return m;
    }

}