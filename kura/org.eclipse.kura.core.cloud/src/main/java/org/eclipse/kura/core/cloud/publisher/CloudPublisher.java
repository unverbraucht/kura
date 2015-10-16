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
package org.eclipse.kura.core.cloud.publisher;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.core.cloud.publisher.CloudPublisherOptions.AutoConnectMode;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.DataServiceListener;
import org.eclipse.kura.data.event.DataEvent;
import org.eclipse.kura.data.event.DataEventHandler;
import org.eclipse.kura.data.event.DataEventSupport;
import org.eclipse.kura.data.event.DataField;
import org.eclipse.kura.data.event.DataRecord;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraPosition;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.position.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FIXME: Add option to select the format of the message being published: KuraProtoBuf or JSON
 */
public class CloudPublisher implements DataEventHandler, DataServiceListener, ConfigurableComponent
{
    private static final Logger s_logger = LoggerFactory.getLogger(CloudPublisher.class);

    @SuppressWarnings("unused")
    private ComponentContext      m_ctx;
    private CloudPublisherOptions m_options;

    private CloudService          m_cloudService;
    private CloudClient           m_cloudClient;
    private DataService           m_dataService;
    private DataEventSupport      m_dataEventSupport;
    
    private static CloudPublisherDisconnectManager s_disconnectMamanager;
    
    // ----------------------------------------------------------------
    //
    //   Dependencies
    //
    // ----------------------------------------------------------------

    public void setDataService(DataService dataService) {
        this.m_dataService = dataService;
    }

    public void unsetDataService(DataService dataService) {
        this.m_dataService = null;
    }

    public DataService getDataService() {
        return m_dataService;
    }

    public void setCloudService(CloudService cloudService) {
        this.m_cloudService = cloudService;
    }

    public void unsetCloudService(CloudService cloudService) {
        this.m_cloudService = null;
    }

    public CloudService getCloudService() {
        return m_cloudService;
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
        m_options = new CloudPublisherOptions(properties);

        // create the singleton disconnect manager
        if (s_disconnectMamanager == null) {
        	s_disconnectMamanager = new CloudPublisherDisconnectManager(m_dataService,
        			m_options.getAutoConnectQuieceTimeout());
        }

        // recreate the CloudClient
        try {
            setupCloudClient();
        } catch (KuraException e) {
            s_logger.warn("Cannot setup CloudClient");
        }
        
        // create the subscriptions
        setupSubscriptions();
    }

    public void updated(Map<String,Object> properties)
    {
        s_logger.info("updated...: " + properties);

        // Update properties
        m_options = new CloudPublisherOptions(properties);

        // create the singleton disconnect manager        
        synchronized(s_disconnectMamanager) {
            if (s_disconnectMamanager != null) {
                
            	s_disconnectMamanager.setQuieceTimeout(m_options.getAutoConnectQuieceTimeout());
                
            	int minDelay = m_options.getAutoConnectMode().getDisconnectDelay();
                s_disconnectMamanager.disconnectInMinutes(minDelay);
            }
        }

        // recreate the CloudClient
        try {
            setupCloudClient();
        } catch (KuraException e) {
            s_logger.warn("Cannot setup CloudClient");
        }

        // create the subscriptions
        setupSubscriptions();
    }

    protected void deactivate(ComponentContext componentContext) 
    {
        s_logger.info("deactivate...");

        // close the client
        closeCloudClient();
        
        // close the disconnect manager
        synchronized(s_disconnectMamanager) {
            if (s_disconnectMamanager != null) {
                s_disconnectMamanager.stop();
            }
            s_disconnectMamanager = null;
        }
        
        // no need to release the cloud clients as the updated app 
        // certificate is already published due the missing dependency
        // we only need to empty our CloudClient list
        m_dataService  = null;
        m_cloudService = null;
    }

    
    // ----------------------------------------------------------------
    //
    //   DataEvent Handler
    //
    // ----------------------------------------------------------------

    @Override
    public void handleDataEvent(DataEvent dataEvent) 
    {
    	s_logger.info("Receiving event {}", dataEvent.getTopic());
        try {
            
            // Open connection if necessary
            startPublishing();
            
            // Publish received data records
            List<DataRecord> dataRecords = dataEvent.getRecords();
            for (DataRecord dataRecord : dataRecords) {
            
                // prepare the topic
                String appTopic = m_options.getPublishingTopic();                
             
                // prepare the payload
                KuraPayload kuraPayload = buildKuraPayload(dataRecord);
                
                // publish the payload
                m_cloudClient.publish(appTopic, 
                                      kuraPayload, 
                                      m_options.getPublishingQos(), 
                                      m_options.getPublishingRetain(),
                                      m_options.getPublishingPriority());
            }
    
            // Close connection if necessary
            stopPublishing();
        }
        catch (KuraException e) {
            s_logger.error("Could not publish DataRecords", e);
        }
    }

    
    // ----------------------------------------------------------------
    //
    //   Private methods
    //
    // ----------------------------------------------------------------

    private void setupCloudClient() throws KuraException
    {
        closeCloudClient();
        
        // create the new CloudClient for the specified application 
        String    appId = m_options.getPublishingApplication();
        m_cloudClient   = m_cloudService.newCloudClient(appId);
    }

    private void closeCloudClient()
    {
        if (m_cloudClient != null) {
            m_cloudClient.release();
            m_cloudClient = null;
        }
    }
    
    private void setupSubscriptions()
    {
    	if(m_dataEventSupport != null){
	        m_dataEventSupport.unsubscribeAll();
	        for (String emitterId : m_options.getSubscribedEmitters()) {
	        	s_logger.info("Subscribed to emitter {}", emitterId);	            
	        	m_dataEventSupport.subscribe(emitterId);
	        }
    	} else{
    		//Handle null dataEventSupport. Is that even a possibility?
    		//Throw exception?
    	}
    }
    
    private void startPublishing() throws KuraException 
    {
        if (m_cloudClient == null) {
            throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, "CloudClient not available");
        }
        
        if (!AutoConnectMode.AUTOCONNECT_MODE_OFF.equals(m_options.getAutoConnectMode()) &&
            !m_dataService.isAutoConnectEnabled() &&
            !m_dataService.isConnected()) {

            //
            // FIXME: this connect should be a connectWithRetry
            // While the CloudPublisher is active the connection should be in retry mode
            // m_dataService.connectAndStayConnected();
            m_dataService.connect();
        }
    }

    private void stopPublishing() 
    {
        if (m_cloudClient == null) {
            throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, "CloudClient not available");
        }
        
        if (m_dataService.isConnected() &&
            !m_dataService.isAutoConnectEnabled()) {
            AutoConnectMode autoConnMode = m_options.getAutoConnectMode();
            switch (autoConnMode) {
            case AUTOCONNECT_MODE_OFF:
            case AUTOCONNECT_MODE_ON_AND_STAY:
                // nothing to do. Connection is either not opened or should not be closed.
                break;
            default: 
                int minDelay = m_options.getAutoConnectMode().getDisconnectDelay();
                s_disconnectMamanager.disconnectInMinutes(minDelay);
                break;
            }
        }        
    }

    private KuraPayload buildKuraPayload(DataRecord dataRecord) 
    {
        KuraPayload kuraPayload = new KuraPayload();
        
        if (dataRecord.getTimestamp() != null) {
            kuraPayload.setTimestamp(dataRecord.getTimestamp());
        }
        
        if (dataRecord.getPosition() != null) {
            kuraPayload.setPosition(buildKuraPosition(dataRecord.getPosition()));
        }

        for (DataField dataField: dataRecord.getFields()) {
            Object value = null;
            switch (dataField.getValue().getType()){
            case STRING:
                value = dataField.getValue().getValue();
                break;
            case DOUBLE:
                value = dataField.getValue().getValue();
                break;
            case INTEGER:
                value = dataField.getValue().getValue();
                break;
            case LONG:
                value = dataField.getValue().getValue();
                break;
            case BOOLEAN:
                value = dataField.getValue().getValue();
                break;
            case RAW:
                value = dataField.getValue().getValue();
                break;
            case BYTE:
                value = ((Byte) dataField.getValue().getValue()).intValue();
                break;
            case SHORT:
                value = ((Short) dataField.getValue().getValue()).intValue();
                break;
            }
            kuraPayload.addMetric(dataField.getName(), value);
        }

        return kuraPayload;
    }

    private KuraPosition buildKuraPosition(Position position) 
    {
        KuraPosition kuraPosition = new KuraPosition();
        if (position.getLatitude() != null) {
            kuraPosition.setLatitude(position.getLatitude().getValue());
        }
        if (position.getLongitude()!= null) {
            kuraPosition.setLongitude(position.getLongitude().getValue());
        }
        if (position.getAltitude() != null) {
            kuraPosition.setAltitude(position.getAltitude().getValue());
        }
        if (position.getSpeed() != null) {
            kuraPosition.setSpeed(position.getSpeed().getValue());
        }
        if (position.getTrack() != null) {
            kuraPosition.setHeading(position.getTrack().getValue());
        }
        return kuraPosition;
    }

    
    // ----------------------------------------------------------------
    //
    //   Data Service Methods
    //
    // ----------------------------------------------------------------

    @Override
    public void onConnectionEstablished() {
    }

    @Override
    public void onDisconnecting() {
        // somebody is calling disconnect, so we stop our timer if any
        s_disconnectMamanager.stop();
    }

    @Override
    public void onDisconnected() {
        // somebody is calling disconnect, so we stop our timer if any
        s_disconnectMamanager.stop();
    }

    @Override
    public void onConnectionLost(Throwable cause) {
        // nothing to do here; this is managed by the DataService 
    }

    @Override
    public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
    }

    @Override
    public void onMessagePublished(int messageId, String topic) {
    }

    @Override
    public void onMessageConfirmed(int messageId, String topic) {
    }
    
}
