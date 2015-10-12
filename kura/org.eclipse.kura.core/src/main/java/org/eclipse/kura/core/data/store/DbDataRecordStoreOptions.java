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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbDataRecordStoreOptions 
{
    private static final Logger s_logger = LoggerFactory.getLogger(DbDataRecordStoreOptions.class);
            
    private static final String CONF_EMITTERS         = "data.emitters";
    
    private Map<String,Object> m_properties;
    
    DbDataRecordStoreOptions(Map<String,Object> properties) {
        m_properties = properties;
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
}


