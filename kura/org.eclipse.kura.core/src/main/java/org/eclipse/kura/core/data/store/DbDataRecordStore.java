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
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.data.event.DataEvent;
import org.eclipse.kura.data.event.DataEventHandler;
import org.eclipse.kura.data.event.DataEventSupport;
import org.eclipse.kura.data.event.DataField;
import org.eclipse.kura.data.event.DataRecord;
import org.eclipse.kura.data.event.DataType;
import org.eclipse.kura.db.DbService;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbDataRecordStore implements DataEventHandler
{
    private static final Logger s_logger = LoggerFactory.getLogger(DbDataRecordStore.class);

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

        // Update properties
        m_options = new DbDataRecordStoreOptions(properties);

        // create the subscriptions
        setupSubscriptions();
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
    //   DataEvent Handler
    //
    // ----------------------------------------------------------------

    @Override
    public synchronized void handleDataEvent(DataEvent dataEvent) 
    {
        // FIXME: make sure to perform a solid SQL character escaping 
        String tableName = escapeSql(dataEvent.getEmitterId());
        try {
            
            // check for the table that would collect the data of this emitter
            Connection conn = getConnection();
            DatabaseMetaData dbMetaData = conn.getMetaData();            
            
            String catalog = null;
            String schemaPattern = null;

            // FIXME: double check this pattern 
            String tableNamePattern = tableName;
            String[] types = null;
            ResultSet rsTbls = dbMetaData.getTables(catalog, schemaPattern, tableNamePattern, types);
            if (!rsTbls.next()) {
                // table does not exist, create it
                // FIXME: make sure to perform a solid SQL character escaping 
                execute("CREATE TABLE IF NOT EXISTS "+tableName+"(createdOn TIMESTAMP);");                
            }
            
            //
            // Check for the columns contained in this table
            catalog = null;
            schemaPattern = null;

            // FIXME: double check these patterns 
            tableNamePattern = tableName;
            String columnNamePattern = "*";
            ResultSet rsCols = dbMetaData.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern);
            
            Map<String,Integer> columns = new HashMap<String,Integer>();
            while (rsCols.next()) {
                String colName = rsCols.getString("COLUMN_NAME");
                int    colType = rsCols.getInt("DATA_TYPE");
                columns.put(colName, colType);
            }
            
            List<DataRecord> dataRecords = dataEvent.getRecords();            
            for (DataRecord dataRecord : dataRecords) {
                for (DataField dataField : dataRecord.getFields()) {
                    
                    String colName = escapeSql(dataField.getName());
                    String colType = getSqlTypeString(dataField.getValue().getType());
                    Integer colSqlType = columns.get(colName);
                    if (colSqlType == null) {
                        // add column
                        execute("ALTER TABLE "+tableName+" ADD COLUMN "+colName+" "+colType); 
                    }
                    else if (colSqlType != getSqlType(dataField.getValue().getType())) {
                        // drop old column and add new one
                        execute("ALTER TABLE "+tableName+" DROP COLUMN "+colName); 
                        execute("ALTER TABLE "+tableName+" ADD COLUMN "+colName+" "+colType); 
                    }
                    
                    
                }
            }
            
            // FIXME close ResulSet
        }
        catch (Exception e) {
            s_logger.error("Could not store DataRecords", e);
        }        
    }

    
    // ----------------------------------------------------------------
    //
    //   Private methods
    //
    // ----------------------------------------------------------------
    
    private Integer getSqlType(DataType type) 
    {
        switch (type) {
        case BYTE:
            return Types.TINYINT; 

        case SHORT:
            return Types.SMALLINT;

        case INTEGER:
            return Types.INTEGER;

        case LONG:
            return Types.BIGINT;

        case DOUBLE:
            return Types.DOUBLE;

        case FLOAT:
            //HSQLDB: The bit-precision of FLOAT can be defined but it is ignored and the default bit-precision of 64 is used.
            return Types.DOUBLE;

        case BOOLEAN:
            return Types.BOOLEAN;

        case RAW:
            return Types.BINARY;
           

        case STRING:
            return Types.VARCHAR;

        }
        return null;
    }

    private String getSqlTypeString(DataType type) 
    {
//        switch (type) {
//        case BOOLEAN:
//            return Types.BOOLEAN;
//
//        case BYTE:
//            return Types.BINARY;
//
//        case DOUBLE:
//            return Types.DOUBLE;
//
//        case FLOAT:
//            return Types.FLOAT;
//
//        case INTEGER:
//            return Types.INTEGER;
//
//        case LONG:
//            return Types.BIGINT;
//
//        case RAW:
//            return Types.BINARY;
//           
//        case SHORT:
//            return Types.SMALLINT;
//
//        case STRING:
//            return Types.VARCHAR;
//
//        }
        return null;
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
        return s;        
    }
    
    private Connection getConnection() throws SQLException {        
        return m_dbService.getConnection();
    }
    
    private synchronized void execute(String sql, Integer... params) throws KuraStoreException 
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
            throw new KuraStoreException(e, "Cannot execute query");
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
