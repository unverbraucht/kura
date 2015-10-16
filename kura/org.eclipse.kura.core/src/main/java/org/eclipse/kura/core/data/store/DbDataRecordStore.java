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
package org.eclipse.kura.core.data.store;

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
import org.eclipse.kura.core.data.store.DbDataTypeMapper.JdbcType;
import org.eclipse.kura.data.event.DataEvent;
import org.eclipse.kura.data.event.DataEventEmitter;
import org.eclipse.kura.data.event.DataEventHandler;
import org.eclipse.kura.data.event.DataEventSupport;
import org.eclipse.kura.data.event.DataField;
import org.eclipse.kura.data.event.DataRecord;
import org.eclipse.kura.data.event.DataType;
import org.eclipse.kura.data.event.DataValueBoolean;
import org.eclipse.kura.data.event.DataValueByte;
import org.eclipse.kura.data.event.DataValueDouble;
import org.eclipse.kura.data.event.DataValueInteger;
import org.eclipse.kura.data.event.DataValueLong;
import org.eclipse.kura.data.event.DataValueRaw;
import org.eclipse.kura.data.event.DataValueShort;
import org.eclipse.kura.data.event.DataValueString;
import org.eclipse.kura.db.DbService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FIXME: Add index on timestamp
 * FIXME: Verify timestamp resolution to milliseconds
 * FIXME: Add support for period cleanup of the data records collected!
 * FIXME: Add support for different table type - persisted vs in-memory.
 * FIXME: SQL escaping of the names of the tables and columns. Be careful on the capitalization; it is lossy?
 */
public class DbDataRecordStore implements DataEventEmitter, DataEventHandler, ConfigurableComponent
{
    private static final Logger s_logger = LoggerFactory.getLogger(DbDataRecordStore.class);

    private static final String SQL_CREATE_TABLE  = "CREATE TABLE IF NOT EXISTS DR_{0} (timestamp TIMESTAMP);";
    private static final String SQL_ADD_COLUMN    = "ALTER TABLE DR_{0} ADD COLUMN {1} {2};";
    private static final String SQL_DROP_COLUMN   = "ALTER TABLE DR_{0} DROP COLUMN {1};";
    private static final String SQL_INSERT_RECORD = "INSERT INTO DR_{0} ({1}) VALUES ({2});";
    
    private static final String COLUMN_NAME      = "COLUMN_NAME";
    private static final String DATA_TYPE        = "DATA_TYPE";
    
    
    @SuppressWarnings("unused")
    private ComponentContext         m_ctx;
    private DbDataRecordStoreOptions m_options;

    private DbService                m_dbService;
    private DataEventSupport         m_dataEventSupport;
    
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
        m_dataEventSupport = new DataEventSupport(this); 
        		
        // Update properties
        m_options = new DbDataRecordStoreOptions(properties);

        // create the subscriptions
        setupSubscriptions();
        
        // test table
        try {
			
			List<DataField> fields = new ArrayList<DataField>();
			fields.add( new DataField("c_byte",   new DataValueByte((byte) 1)));
			fields.add( new DataField("c_short",  new DataValueShort((short) 1)));
			fields.add( new DataField("c_int",    new DataValueInteger((int) 1)));
			fields.add( new DataField("c_long",   new DataValueLong((long) 1)));
			fields.add( new DataField("c_double", new DataValueLong((long) 1)));
			fields.add( new DataField("c_bool",   new DataValueBoolean(true)));
			fields.add( new DataField("c_raw",    new DataValueRaw( new byte[]{})));
			fields.add( new DataField("c_string", new DataValueString("a")));

        	DataRecord dataRecord = new DataRecord(new Date(), fields);

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
        m_options = new DbDataRecordStoreOptions(properties);

        // create the subscriptions
        setupSubscriptions();
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
    //   DataEvent APIs
    //
    // ----------------------------------------------------------------

	@Override
	public String getEmitterId() {
		return m_options.getEmitterId();
	}

    @Override
    public synchronized void handleDataEvent(DataEvent dataEvent) 
    {
        List<DataRecord> dataRecords = dataEvent.getRecords();
        for (DataRecord dataRecord : dataRecords) {
        	storeDataRecord(dataEvent.getEmitterId(), dataRecord);
        }        
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
    
    
    private void reconcileColumns(String tableName, DataRecord dataRecord) throws SQLException 
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
    	List<DataField> dataFields = dataRecord.getFields();
        for (DataField dataField : dataFields) {
            
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

    
	private void storeDataRecord(String emitterId, DataRecord dataRecord)
	{
		boolean inserted = false;
		int   retryCount = 0;
		do {
			try {

				// store the record
				insertDataRecord(emitterId, dataRecord);
				inserted = true;
				
				// emit the storage event
				m_dataEventSupport.emit( new DataEvent(getEmitterId(), dataRecord));
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

	
    private void insertDataRecord(String tableName, DataRecord dataRecord) throws SQLException 
    {
        String  sqlTableName = escapeSql(tableName);
    	StringBuilder sbCols = new StringBuilder();
    	StringBuilder sbVals = new StringBuilder();

    	// add the timestamp
    	sbCols.append("TIMESTAMP");
    	sbVals.append("?");        	

    	List<DataField> dataFields = dataRecord.getFields();
        for (DataField dataField : dataFields) {
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

            	DataField dataField = dataFields.get(i);
            	DataType   dataType = dataField.getValue().getType();
            	switch (dataType) {
				case BOOLEAN:
	            	stmt.setBoolean(2+i, ((DataValueBoolean) dataField.getValue()).getValue());
					break;
				case BYTE:
	            	stmt.setByte(2+i, ((DataValueByte) dataField.getValue()).getValue());
					break;
				case DOUBLE:
					s_logger.info("Storing double of value {}",((DataValueDouble) dataField.getValue()).getValue());
	            	stmt.setDouble(2+i, ((DataValueDouble) dataField.getValue()).getValue());
					break;
				case INTEGER:
	            	stmt.setInt(2+i, ((DataValueInteger) dataField.getValue()).getValue());
					break;
				case LONG:
	            	stmt.setLong(2+i, ((DataValueLong) dataField.getValue()).getValue());
					break;
				case RAW:
	            	stmt.setBytes(2+i, ((DataValueRaw) dataField.getValue()).getValue());
					break;
				case SHORT:
	            	stmt.setShort(2+i, ((DataValueShort) dataField.getValue()).getValue());
					break;
				case STRING:
	            	stmt.setString(2+i, ((DataValueString) dataField.getValue()).getValue());
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


    private void setupSubscriptions()
    {
        m_dataEventSupport.unsubscribeAll();
        for (String emitterId : m_options.getSubscribedEmitters()) {
            m_dataEventSupport.subscribe(emitterId);
        }
    }
    
    
    private String escapeSql(String s) {
        // escape all bad SQL characters that should not be allowed in table and column names
        // FIXME: find an implementation and plug it in
        return s.toUpperCase();        
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
}
