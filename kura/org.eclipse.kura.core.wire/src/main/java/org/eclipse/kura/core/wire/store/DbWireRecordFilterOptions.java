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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbWireRecordFilterOptions 
{
    @SuppressWarnings("unused")
	private static final Logger s_logger = LoggerFactory.getLogger(DbWireRecordFilterOptions.class);
            
    private static final String CONF_EMITTER_ID   = "emitter.id";
    private static final String CONF_EMITTERS     = "data.emitters";
    private static final String CONF_SQL_VIEW     = "sql.view";
    private static final String CONF_REFRESH_RATE = "refresh.rate";
    
    private Map<String,Object> m_properties;
    
    DbWireRecordFilterOptions(Map<String,Object> properties) {
        m_properties = properties;
    }
    
    /**
     * Returns the ID to be used for this emitter.
     * @return
     */
    public String getEmitterId() 
    {
        String emitterId = null; 
        if (m_properties != null &&
            m_properties.get(CONF_EMITTER_ID) != null &&
            m_properties.get(CONF_EMITTER_ID) instanceof String) {
        	emitterId = (String) m_properties.get(CONF_EMITTER_ID);
        }
        return emitterId;
    }   
    
    /**
     * Returns the emitters to be used for message publishing.
     * @return
     */
    public String[] getSubscribedEmitters() 
    {
        String[] emitteres = {}; 
        if (m_properties != null &&
            m_properties.get(CONF_EMITTERS) != null &&
            m_properties.get(CONF_EMITTERS) instanceof String) {
            String emittersStr = (String) m_properties.get(CONF_EMITTERS);
            emitteres = emittersStr.split(",");
        }
        return emitteres;
    }   

    /**
     * Returns the SQL to be executed for this view
     * @return
     */
    public String getSqlView() 
    {
        String sqlView = null; 
        if (m_properties != null &&
            m_properties.get(CONF_SQL_VIEW) != null &&
            m_properties.get(CONF_SQL_VIEW) instanceof String) {
        	sqlView = (String) m_properties.get(CONF_SQL_VIEW);
        }
        return sqlView;
    }   

    /**
     * Returns the rate of refresh for this view.
     * @return
     */
    public int getRefreshRate() 
    {
        int refreshRate = 0; 
        if (m_properties != null &&
            m_properties.get(CONF_SQL_VIEW) != null &&
            m_properties.get(CONF_SQL_VIEW) instanceof Integer) {
        	refreshRate = (Integer) m_properties.get(CONF_REFRESH_RATE);
        }
        return refreshRate;
    }   
}


