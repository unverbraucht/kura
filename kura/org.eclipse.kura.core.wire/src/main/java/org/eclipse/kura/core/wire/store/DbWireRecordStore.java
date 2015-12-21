/**
 * Copyright (c) 2011, 2015 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.core.wire.store;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.core.wire.store.DbDataTypeMapper.JdbcType;
import org.eclipse.kura.db.DbService;
import org.eclipse.kura.wire.WireDataType;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireField;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.eclipse.kura.wire.WireValueBoolean;
import org.eclipse.kura.wire.WireValueByte;
import org.eclipse.kura.wire.WireValueDouble;
import org.eclipse.kura.wire.WireValueInteger;
import org.eclipse.kura.wire.WireValueLong;
import org.eclipse.kura.wire.WireValueRaw;
import org.eclipse.kura.wire.WireValueShort;
import org.eclipse.kura.wire.WireValueString;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FIXME: Extract the DataRecordStore service interface in the API and implement a store and query method
 * FIXME: Add primary key and index on timestamp
 * FIXME: Verify timestamp resolution to milliseconds
 * FIXME: Add support for period cleanup of the data records collected!
 * FIXME: Add support for different table type - persisted vs in-memory.
 * FIXME: SQL escaping of the names of the tables and columns. Be careful on the capitalization; it is lossy?
 * FIXME: Add support for Cloudlet
 */
public class DbWireRecordStore implements WireEmitter, WireReceiver, ConfigurableComponent
{
    private static final Logger s_logger = LoggerFactory.getLogger(DbWireRecordStore.class);

    private static final String SQL_CREATE_TABLE  = "CREATE TABLE IF NOT EXISTS DR_{0} (timestamp TIMESTAMP);";
    private static final String SQL_ADD_COLUMN    = "ALTER TABLE DR_{0} ADD COLUMN {1} {2};";
    private static final String SQL_DROP_COLUMN   = "ALTER TABLE DR_{0} DROP COLUMN {1};";
    private static final String SQL_INSERT_RECORD = "INSERT INTO DR_{0} ({1}) VALUES ({2});";
    
    private static final String COLUMN_NAME      = "COLUMN_NAME";
    private static final String DATA_TYPE        = "DATA_TYPE";

    private ComponentContext         m_ctx;
    @SuppressWarnings("unused")
	private DbWireRecordStoreOptions m_options;
    private DbService                m_dbService;
    private WireSupport              m_wireSupport;
    
    // ----------------------------------------------------------------
    //
    //   Dependencies
    //
    // ----------------------------------------------------------------

    public void setDbService(DbService dbService) {
        this.m_dbService = dbService;
    }

    public void unsetDbService(DbService dataService) {
        this.m_dbService = null;
    }

    public DbService getDbService() {
        return m_dbService;
    }

    // ----------------------------------------------------------------
    //
    //   Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String,Object> properties) 
    {
        s_logger.info("activate...");

        //
        // save the bundle context and the properties
        m_ctx = componentContext;
        m_wireSupport = new WireSupport(this); 
        		
        // Update properties
        m_options = new DbWireRecordStoreOptions(properties);

        // FIXME: remove test table create and insert        
        try {
			
			List<WireField> fields = new ArrayList<WireField>();
			fields.add( new WireField("c_byte",   new WireValueByte((byte) 1)));
			fields.add( new WireField("c_short",  new WireValueShort((short) 1)));
			fields.add( new WireField("c_int",    new WireValueInteger((int) 1)));
			fields.add( new WireField("c_long",   new WireValueLong((long) 1)));
			fields.add( new WireField("c_double", new WireValueLong((long) 1)));
			fields.add( new WireField("c_bool",   new WireValueBoolean(true)));
			fields.add( new WireField("c_raw",    new WireValueRaw( new byte[]{})));
			fields.add( new WireField("c_string", new WireValueString("a")));

        	WireRecord dataRecord = new WireRecord(new Date(), fields);

        	String tableName = "testTable";			
        	reconcileTable(tableName);

        	reconcileColumns(tableName, dataRecord);			
			insertDataRecord(tableName, dataRecord);			
		} 
        catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public void updated(Map<String,Object> properties)
    {
        s_logger.info("updated...: " + properties);

        // Update properties
        m_options = new DbWireRecordStoreOptions(properties);
    }

    protected void deactivate(ComponentContext componentContext) 
    {
        s_logger.info("deactivate...");
        
        // no need to release the cloud clients as the updated app 
        // certificate is already published due the missing dependency
        // we only need to empty our CloudClient list
        m_dbService  = null;
    }

    
    // ----------------------------------------------------------------
    //
    //   Kura Wire APIs
    //
    // ----------------------------------------------------------------

	@Override
	public String getEmitterPid() {
		return (String) m_ctx.getProperties().get("service.pid");
	}

    @Override
    public synchronized void onWireReceive(WireEnvelope wireEvelope) 
    {
        List<WireRecord> dataRecords = wireEvelope.getRecords();
        for (WireRecord dataRecord : dataRecords) {
        	storeDataRecord(wireEvelope.getEmitterPid(), dataRecord);
        }
        
		// emit the storage event
		m_wireSupport.emit(dataRecords);
    }

    
    // ----------------------------------------------------------------
    //
    //   Private methods
    //
    // ----------------------------------------------------------------
    
    private void reconcileTable(String tableName) throws SQLException 
    {
        String sqlTableName = escapeSql(tableName);
        Connection conn = getConnection();
        try {
        	
	        // check for the table that would collect the data of this emitter        	
	        String catalog = conn.getCatalog();
	        DatabaseMetaData dbMetaData = conn.getMetaData();	
	        ResultSet rsTbls = dbMetaData.getTables(catalog, null, sqlTableName, null);        
	        if (!rsTbls.next()) {
	        	// table does not exist, create it
	        	s_logger.info("Creating table DR_{}...", sqlTableName);
	        	execute(MessageFormat.format(SQL_CREATE_TABLE, sqlTableName));              
	        }
        }
        finally {
        	close(conn);
        }
    }
    
    
    private void reconcileColumns(String tableName, WireRecord dataRecord) throws SQLException 
    {
        String sqlTableName = escapeSql(tableName);
        Connection conn = null;
        ResultSet rsColumns = null;
		Map<String,Integer> columns = new HashMap<String,Integer>();
    	try {
	        // check for the table that would collect the data of this emitter        	
	        conn = getConnection();
	        String catalog = conn.getCatalog();
	        DatabaseMetaData dbMetaData = conn.getMetaData();	
	        rsColumns = dbMetaData.getColumns(catalog, null, "DR_"+sqlTableName, null);
        
    		// map the columns
    		while (rsColumns.next()) {
    			String colName = rsColumns.getString(COLUMN_NAME);
    			int    colType = rsColumns.getInt(DATA_TYPE);
    			columns.put(colName, colType);
    		}
        }
    	finally {
        	close(rsColumns);
    		close(conn);        	
    	}
        
		// reconcile columns
    	List<WireField> dataFields = dataRecord.getFields();
        for (WireField dataField : dataFields) {
            
            String  sqlColName = escapeSql(dataField.getName());
            Integer sqlColType = columns.get(sqlColName);
        	JdbcType  jdbcType = DbDataTypeMapper.getJdbcType(dataField.getValue().getType());
            if (sqlColType == null) {

            	// add column
                execute(MessageFormat.format(SQL_ADD_COLUMN, sqlTableName, sqlColName, jdbcType.getTypeString())); 
            }
            else if (sqlColType != jdbcType.getType()) {
            	
                // drop old column and add new one
                execute(MessageFormat.format(SQL_DROP_COLUMN, sqlTableName, sqlColName)); 
                execute(MessageFormat.format(SQL_ADD_COLUMN, sqlTableName, sqlColName, jdbcType.getTypeString())); 
            }
        }
    }

    
	private void storeDataRecord(String emitterId, WireRecord dataRecord)
	{
		boolean inserted = false;
		int   retryCount = 0;
		do {
			try {

				// store the record
				insertDataRecord(emitterId, dataRecord);
				inserted = true;				
			}
			catch (SQLException e) {
				try {
					reconcileTable(emitterId);
					reconcileColumns(emitterId, dataRecord);
					retryCount++;
				}
				catch (SQLException ee) {
					s_logger.error("Cannot reconcile the database", ee);
				}
			}
		}
		while (!inserted && retryCount < 2);
	}

	
    private void insertDataRecord(String tableName, WireRecord dataRecord) throws SQLException 
    {
        String  sqlTableName = escapeSql(tableName);
    	StringBuilder sbCols = new StringBuilder();
    	StringBuilder sbVals = new StringBuilder();

    	// add the timestamp
    	sbCols.append("TIMESTAMP");
    	sbVals.append("?");        	

    	List<WireField> dataFields = dataRecord.getFields();
        for (WireField dataField : dataFields) {
            String  sqlColName = escapeSql(dataField.getName());
        	sbCols.append(", "+sqlColName);
        	sbVals.append(", ?");        	
        }
        
		s_logger.info("Storing data record from emitter {} into table {}...", tableName, sqlTableName);
        String sqlInsert = MessageFormat.format(SQL_INSERT_RECORD, 
        										sqlTableName, 
        										sbCols.toString(),
        										sbVals.toString());        
        Connection conn = null;
        PreparedStatement stmt = null;
        try {           
            
            conn = getConnection();
            stmt = conn.prepareStatement(sqlInsert);
        	stmt.setTimestamp(1, new Timestamp(dataRecord.getTimestamp().getTime()));            
            for (int i=0; i<dataFields.size(); i++) {

            	WireField dataField = dataFields.get(i);
            	WireDataType   dataType = dataField.getValue().getType();
            	switch (dataType) {
				case BOOLEAN:
	            	stmt.setBoolean(2+i, ((WireValueBoolean) dataField.getValue()).getValue());
					break;
				case BYTE:
	            	stmt.setByte(2+i, ((WireValueByte) dataField.getValue()).getValue());
					break;
				case DOUBLE:
					s_logger.info("Storing double of value {}",((WireValueDouble) dataField.getValue()).getValue());
	            	stmt.setDouble(2+i, ((WireValueDouble) dataField.getValue()).getValue());
					break;
				case INTEGER:
	            	stmt.setInt(2+i, ((WireValueInteger) dataField.getValue()).getValue());
					break;
				case LONG:
	            	stmt.setLong(2+i, ((WireValueLong) dataField.getValue()).getValue());
					break;
				case RAW:
	            	stmt.setBytes(2+i, ((WireValueRaw) dataField.getValue()).getValue());
					break;
				case SHORT:
	            	stmt.setShort(2+i, ((WireValueShort) dataField.getValue()).getValue());
					break;
				case STRING:
	            	stmt.setString(2+i, ((WireValueString) dataField.getValue()).getValue());
					break;
            	}
            }
            stmt.execute();
            conn.commit();
			s_logger.info("Stored double of value");
        }
        catch (SQLException e) {
            rollback(conn);
            throw e;
        }
        finally {
            close(stmt);
            close(conn);
        }        
    }
    
    
    private String escapeSql(String s) {
        // escape all bad SQL characters that should not be allowed in table and column names
        // FIXME: find an implementation and plug it in
    	String s1 = s.replace('.', '_');
    	s1 = s1.replace(' ', '_');
        return s1.toUpperCase();        
    }
    
    
    private Connection getConnection() throws SQLException {        
        return m_dbService.getConnection();
    }
    
    
    private synchronized void execute(String sql, Integer... params) 
    	throws SQLException 
    {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {           
            
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            for (int i=0; i<params.length; i++) {
                stmt.setInt(1+i, params[i]); 
            }
            stmt.execute();
            conn.commit();
        }
        catch (SQLException e) {
            rollback(conn);
            throw e;
        }
        finally {
            close(stmt);
            close(conn);
        }
    }

    
    private void rollback(Connection conn) {
        m_dbService.rollback(conn);
    }
    
    
    private void close(ResultSet... rss) {
        m_dbService.close(rss);
    }

    
    private void close(Statement... stmts) {
        m_dbService.close(stmts);
    }

    
    private void close(Connection conn) {
        m_dbService.close(conn);
    }    

    
    // ----------------------------------------------------------------
    //
    //   Wire APIs
    //
    // ----------------------------------------------------------------

	@Override
	public Object polled(Wire wire) {
		return m_wireSupport.polled(wire);
	}

	
	@Override
	public void consumersConnected(Wire[] wires) {
		m_wireSupport.consumersConnected(wires);
	}

	
	@Override
	public void updated(Wire wire, Object value) {
		m_wireSupport.updated(wire, value);
	}

	
	@Override
	public void producersConnected(Wire[] wires) {
		m_wireSupport.producersConnected(wires);
	}
}
