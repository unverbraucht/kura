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
package org.eclipse.kura.core.wire.cloud.publisher;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudPublisherOptions 
{
    private static final Logger s_logger = LoggerFactory.getLogger(CloudPublisherOptions.class);
        
	public static enum AutoConnectMode {
	    AUTOCONNECT_MODE_OFF(-2),
        AUTOCONNECT_MODE_ON_AND_STAY(-1),
	    AUTOCONNECT_MODE_ON_AND_OFF(0),
	    AUTOCONNECT_MODE_ON_AND_STAY_1_MIN(1),
	    AUTOCONNECT_MODE_ON_AND_STAY_5_MIN(5),
	    AUTOCONNECT_MODE_ON_AND_STAY_10_MIN(10),
	    AUTOCONNECT_MODE_ON_AND_STAY_15_MIN(15),
	    AUTOCONNECT_MODE_ON_AND_STAY_30_MIN(30),
	    AUTOCONNECT_MODE_ON_AND_STAY_60_MIN(60);
	    
	    private int disconnectDelay;
	    
	    private AutoConnectMode(int disconnectDelay) {
	        this.disconnectDelay = disconnectDelay;
	    }
	    
	    public int getDisconnectDelay() {
	        return this.disconnectDelay;
	    }
	};
	private static final AutoConnectMode  DEFAULT_AUTOCONNECT_MODE = AutoConnectMode.AUTOCONNECT_MODE_ON_AND_OFF;

    private static final String  DEFAULT_APPLICATION    = "PUB";
	private static final String  DEFAULT_TOPIC          = "EVENT";
    private static final int     DEFAULT_PRIORITY       = 7;
	private static final int     DEFAULT_QOS            = 0;
	private static final boolean DEFAULT_RETAIN         = false;
    private static final int     DEFAULT_QUIECE_TIMEOUT = 1000;
	
    private static final String CONF_EMITTERS         = "data.emitters";
    private static final String CONF_AUTOCONNECT_MODE = "autoconnect.mode";
    private static final String CONF_QUIECE_TIMEOUT   = "autoconnect.quiceTimeout";
    private static final String CONF_APPLICATION      = "publish.application";
    private static final String CONF_TOPIC            = "publish.topic";
    private static final String CONF_PRIORITY         = "publish.priority";
    private static final String CONF_QOS              = "publish.qos";
    private static final String CONF_RETAIN           = "publish.retain";
	
	private Map<String,Object> m_properties;
	
	CloudPublisherOptions(Map<String,Object> properties) {
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

    /**
     * Returns the topic to be used for message publishing.
     * @return
     */
    public String getPublishingApplication() 
    {
        String publishingApp = DEFAULT_APPLICATION;
        if (m_properties != null &&
            m_properties.get(CONF_APPLICATION) != null &&
            m_properties.get(CONF_APPLICATION) instanceof String) {
            publishingApp = (String) m_properties.get(CONF_APPLICATION);
        }
        return publishingApp;
    }   

    /**
     * Returns the topic to be used for message publishing.
     * @return
     */
    public String getPublishingTopic() 
    {
        String publishingTopic = DEFAULT_TOPIC;
        if (m_properties != null &&
            m_properties.get(CONF_TOPIC) != null &&
            m_properties.get(CONF_TOPIC) instanceof String) {
            publishingTopic = (String) m_properties.get(CONF_TOPIC);
        }
        return publishingTopic;
    }   

    /**
     * Returns the priority to be used for message publishing.
     * @return
     */
    public int getPublishingPriority() 
    {
        int publishingPriority = DEFAULT_PRIORITY;
        if (m_properties != null &&
            m_properties.get(CONF_PRIORITY) != null &&
            m_properties.get(CONF_PRIORITY) instanceof Integer) {
            publishingPriority = (Integer) m_properties.get(CONF_PRIORITY);
        }
        return publishingPriority;
    }   

    /**
     * Returns the QoS to be used for message publishing.
     * @return
     */
    public int getPublishingQos() 
    {
        int publishingQos = DEFAULT_QOS;
        if (m_properties != null &&
            m_properties.get(CONF_QOS) != null &&
            m_properties.get(CONF_QOS) instanceof Integer) {
            publishingQos = (Integer) m_properties.get(CONF_QOS);
        }
        return publishingQos;
    }   

    /**
     * Returns the retain to be used for message publishing.
     * @return
     */
    public boolean getPublishingRetain() 
    {
        boolean publishingRetain = DEFAULT_RETAIN;
        if (m_properties != null &&
            m_properties.get(CONF_RETAIN) != null &&
            m_properties.get(CONF_RETAIN) instanceof Integer) {
            publishingRetain = (Boolean) m_properties.get(CONF_RETAIN);
        }
        return publishingRetain;
    }

    /**
     * Returns the retain to be used for message publishing.
     * @return
     */
    public AutoConnectMode getAutoConnectMode() 
    {
        AutoConnectMode autoConnectMode = DEFAULT_AUTOCONNECT_MODE;
        if (m_properties != null &&
            m_properties.get(CONF_AUTOCONNECT_MODE) != null &&
            m_properties.get(CONF_AUTOCONNECT_MODE) instanceof String) {
            String autoconnectModeValue = (String)m_properties.get(CONF_AUTOCONNECT_MODE);
            try {
                autoConnectMode = AutoConnectMode.valueOf(autoconnectModeValue);
            }
            catch(IllegalArgumentException iea) {
                s_logger.warn("Invalid autoconnect mode:"+autoconnectModeValue, iea);
            }
        }
        return autoConnectMode;
    }


    /**
     * Returns the QoS to be used for message publishing.
     * @return
     */
    public int getAutoConnectQuieceTimeout() 
    {
        int quieceTimeout = DEFAULT_QUIECE_TIMEOUT;
        if (m_properties != null &&
            m_properties.get(CONF_QUIECE_TIMEOUT) != null &&
            m_properties.get(CONF_QUIECE_TIMEOUT) instanceof Integer) {
            quieceTimeout = (Integer) m_properties.get(CONF_QUIECE_TIMEOUT);
        }
        return quieceTimeout;
    }   
}


