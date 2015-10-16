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

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.kura.data.DataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudPublisherDisconnectManager
{
    private static final Logger s_logger = LoggerFactory.getLogger(CloudPublisherDisconnectManager.class);

    private static final String TIMER_NAME = "CloudPublisherDisconnectManager";
    
    private DataService m_dataService;
    private Timer       m_timer;
    private long        m_quieceTimeout;
    private long        m_nextExecutionTime;
    
    public CloudPublisherDisconnectManager(DataService dataService,
                                           long quieceTimeout) 
    {
        m_dataService   = dataService;
        m_quieceTimeout = quieceTimeout;
        m_nextExecutionTime = 0;
    }
    
    public long getQuieceTimeout() {
        return m_quieceTimeout;
    }

    public void setQuieceTimeout(long quieceTimeout) {
        this.m_quieceTimeout = quieceTimeout;
    }

    public synchronized void disconnectInMinutes(int minutes) 
    {
        // check if the required timeout is longer than the one already scheduled
        long remainingDelay = m_nextExecutionTime - System.currentTimeMillis(); 
        long  requiredDelay = (long) minutes * 60 * 1000;
        if (requiredDelay > remainingDelay) {            
            scheduleNewTimer(requiredDelay);
        }
    }
    
    public synchronized void stop() 
    {
        // cancel existing timer
        if (m_timer != null) {
            m_timer.cancel();            
        }

        s_logger.info("Stopped.");
    }

    // ----------------------------------------------------------------
    //
    //   Private methods
    //
    // ----------------------------------------------------------------

    private void scheduleNewTimer(long delay)     
    {
        // cancel existing timer
        if (m_timer != null) {
            m_timer.cancel();
        }
        
        // calculate next execution
        s_logger.info("Scheduling disconnect in {} msec...", delay);
        m_nextExecutionTime = System.currentTimeMillis() + delay;

        // start new timer
        m_timer = new Timer(TIMER_NAME);
        m_timer.schedule(new TimerTask() {

			@Override
			public void run() {
		        // disconnect
		        try {
		            m_dataService.disconnect(m_quieceTimeout);            
		        }
		        catch (Exception e) {
		            s_logger.warn("Error disconnecting", e);
		        }
		        
		        // cleanup
		        m_timer = null;
		        m_nextExecutionTime = 0;
			}
        	
        }, new Date(m_nextExecutionTime));
    }
}
