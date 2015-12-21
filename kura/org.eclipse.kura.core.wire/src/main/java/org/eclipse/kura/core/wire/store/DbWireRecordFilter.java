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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
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
 * FIXME: Remove refresh rate parameter and add a new DataEventTimer service
 * FIXME: Add support for Cloudlet 
 */
public class DbWireRecordFilter implements WireEmitter, WireReceiver, ConfigurableComponent
{
    private static final Logger s_logger = LoggerFactory.getLogger(DbWireRecordFilter.class);    
    
    private ComponentContext          m_ctx;
    private DbWireRecordFilterOptions m_options;
    private DbService                 m_dbService;
    private WireSupport        		  m_wireSupport;
    
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
        m_options = new DbWireRecordFilterOptions(properties);

        // FIXME: remove test code
//        try {
//			List<WireRecord> records = refreshDataView();
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}        
    }

    public void updated(Map<String,Object> properties)
    {
        s_logger.info("updated...: " + properties);

        // Update properties
        m_options = new DbWireRecordFilterOptions(properties);
        try {
			List<WireRecord> records = refreshDataView();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}        
        
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
    	s_logger.error("wireEnvelope received!");
    	// FIXME: add implementation for onWireReceive 
    }
    
    
    // ----------------------------------------------------------------
    //
    //   Private methods
    //
    // ----------------------------------------------------------------
    
    private List<WireRecord> refreshDataView() 
    	throws SQLException 
    {
    	Date now = new Date();
    	List<WireRecord> dataRecords = new ArrayList<WireRecord>();
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
                             
                	List<WireField> dataFields = new ArrayList<WireField>();
                	ResultSetMetaData rmet = rset.getMetaData();
                	for (int i=1; i<=rmet.getColumnCount(); i++) {
                		
                		String fieldName = rmet.getColumnLabel(i);
                		if (fieldName == null) {
                			fieldName = rmet.getColumnName(i);
                		}
                		
                		WireField dataField = null;

                		int jdbcType = rmet.getColumnType(i);
                		WireDataType dataType = DbDataTypeMapper.getDataType(jdbcType);
                		switch (dataType) {
						case BOOLEAN:
							boolean boolValue = rset.getBoolean(i);
							dataField = new WireField(fieldName, new WireValueBoolean(boolValue)); 
							break;
						case BYTE:
							byte byteValue = rset.getByte(i);
							dataField = new WireField(fieldName, new WireValueByte(byteValue)); 
							break;
						case DOUBLE:
							double doubleValue = rset.getDouble(i);
							dataField = new WireField(fieldName, new WireValueDouble(doubleValue)); 
							break;
						case INTEGER:
							int intValue = rset.getInt(i);
							dataField = new WireField(fieldName, new WireValueInteger(intValue)); 
							break;
						case LONG:
							long longValue = rset.getLong(i);
							dataField = new WireField(fieldName, new WireValueLong(longValue)); 
							break;
						case RAW:
							byte[] bytesValue = rset.getBytes(i);
							dataField = new WireField(fieldName, new WireValueRaw(bytesValue)); 
							break;
						case SHORT:
							short shortValue = rset.getShort(i);
							dataField = new WireField(fieldName, new WireValueShort(shortValue)); 
							break;
						case STRING:
							String stringValue = rset.getString(i);
							dataField = new WireField(fieldName, new WireValueString(stringValue)); 
							break;                			
                		}
                    	dataFields.add(dataField);
                	}
                	dataRecords.add( new WireRecord(now, dataFields));
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
