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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
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
 * FIXME: Remove refresh rate parameter and add a new DataEventTimer service
 * FIXME: Add support for Cloudlet 
 */
public class DbDataRecordFilter implements DataEventEmitter, DataEventHandler, ConfigurableComponent
{
    private static final Logger s_logger = LoggerFactory.getLogger(DbDataRecordFilter.class);    
    
    @SuppressWarnings("unused")
    private ComponentContext        m_ctx;
    private DbDataRecordFilterOptions m_options;

    private DbService               m_dbService;
    private DataEventSupport        m_dataEventSupport;
    
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
        m_options = new DbDataRecordFilterOptions(properties);

        // create the subscriptions
        setupSubscriptions();
        
        try {
			List<DataRecord> records = refreshDataView();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public void updated(Map<String,Object> properties)
    {
        s_logger.info("updated...: " + properties);

        // Update properties
        m_options = new DbDataRecordFilterOptions(properties);

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
    	// FIXME
    }
    
    
    // ----------------------------------------------------------------
    //
    //   Private methods
    //
    // ----------------------------------------------------------------
    
    private void setupSubscriptions()
    {
        m_dataEventSupport.unsubscribeAll();
        for (String emitterId : m_options.getSubscribedEmitters()) {
            m_dataEventSupport.subscribe(emitterId);
        }
    }

    
    private List<DataRecord> refreshDataView() 
    	throws SQLException 
    {
    	Date now = new Date();
    	List<DataRecord> dataRecords = new ArrayList<DataRecord>();
    	Connection conn = null;
        Statement  stmt = null;
        ResultSet  rset = null;
        String sqlView = m_options.getSqlView();
        try {           
            
            conn = getConnection();
            stmt = conn.createStatement();
            rset = stmt.executeQuery(sqlView);
            if (rset != null) {
                while (rset.next()) {
                             
                	List<DataField> dataFields = new ArrayList<DataField>();
                	ResultSetMetaData rmet = rset.getMetaData();
                	for (int i=1; i<=rmet.getColumnCount(); i++) {
                		
                		String fieldName = rmet.getColumnLabel(i);
                		if (fieldName == null) {
                			fieldName = rmet.getColumnName(i);
                		}
                		
                		DataField dataField = null;

                		int jdbcType = rmet.getColumnType(i);
                		DataType dataType = DbDataTypeMapper.getDataType(jdbcType);
                		switch (dataType) {
						case BOOLEAN:
							boolean boolValue = rset.getBoolean(i);
							dataField = new DataField(fieldName, new DataValueBoolean(boolValue)); 
							break;
						case BYTE:
							byte byteValue = rset.getByte(i);
							dataField = new DataField(fieldName, new DataValueByte(byteValue)); 
							break;
						case DOUBLE:
							double doubleValue = rset.getDouble(i);
							dataField = new DataField(fieldName, new DataValueDouble(doubleValue)); 
							break;
						case INTEGER:
							int intValue = rset.getInt(i);
							dataField = new DataField(fieldName, new DataValueInteger(intValue)); 
							break;
						case LONG:
							long longValue = rset.getLong(i);
							dataField = new DataField(fieldName, new DataValueLong(longValue)); 
							break;
						case RAW:
							byte[] bytesValue = rset.getBytes(i);
							dataField = new DataField(fieldName, new DataValueRaw(bytesValue)); 
							break;
						case SHORT:
							short shortValue = rset.getShort(i);
							dataField = new DataField(fieldName, new DataValueShort(shortValue)); 
							break;
						case STRING:
							String stringValue = rset.getString(i);
							dataField = new DataField(fieldName, new DataValueString(stringValue)); 
							break;                			
                		}
                    	dataFields.add(dataField);
                	}
                	dataRecords.add( new DataRecord(now, dataFields));
                }
            }
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            close(rset);
            close(stmt);
            close(conn);
        }
        
        return dataRecords;
    }
    
    
    private Connection getConnection() throws SQLException {        
        return m_dbService.getConnection();
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
