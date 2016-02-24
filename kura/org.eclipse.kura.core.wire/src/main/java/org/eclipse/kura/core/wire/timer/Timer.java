/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.core.wire.timer;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.wires.WireEmitter;
import org.eclipse.kura.wires.WireField;
import org.eclipse.kura.wires.WireRecord;
import org.eclipse.kura.wires.WireSupport;
import org.eclipse.kura.wires.WireValueString;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Timer implements WireEmitter, ConfigurableComponent {

	private static final Logger s_logger = LoggerFactory.getLogger(Timer.class);
	
	private static final String PROP_TIMER_NAME = "name";
	private static final String PROP_INTERVAL = "interval";
	
	public static final String TIMER_EVENT_FIELD_NAME="KuraTimerEvent";
	
	private Future<?> m_tickHandle;
	private ExecutorService m_tickExecutor;
	
	private Map<String, Object> m_properties;
	private WireSupport m_wireSupport;
	
	private String m_name;
	private int m_interval;

    // ----------------------------------------------------------------
    //
    //   Dependencies
    //
    // ----------------------------------------------------------------

	public Timer(){
		m_tickExecutor = Executors.newSingleThreadExecutor();
		m_wireSupport = new WireSupport(this);
	}
    // ----------------------------------------------------------------
    //
    //   Activation APIs
    //
    // ----------------------------------------------------------------

	protected void activate(ComponentContext ctx, Map<String, Object> properties) {
		s_logger.info("Activating Wire timer...");

		m_properties = properties;

		doUpdate();
	}

	protected void deactivate(ComponentContext ctx) {
		s_logger.info("Deactivating Wire timer...");
		
		if (m_tickHandle != null) {
			m_tickHandle.cancel(true);
		}

		m_tickExecutor.shutdown();
	}

	protected void updated(Map<String, Object> properties) {
		s_logger.info("Updating Wire timer...");
		
		m_properties = properties;

		doUpdate();
	}

    // ----------------------------------------------------------------
    //
    //   Kura Wire APIs
    //
    // ----------------------------------------------------------------

	@Override
	public void consumersConnected(Wire[] wires) {
		m_wireSupport.consumersConnected(wires);
	}

	@Override
	public Object polled(Wire wire) {
		return m_wireSupport.polled(wire);
	}

	@Override
	public String getEmitterPid() {
		return this.getClass().toString();
	}

	// ----------------------------------------------------------------
    //
    //   Private methods
    //
    // ----------------------------------------------------------------

	private void doUpdate(){
		m_name = m_properties.get(PROP_TIMER_NAME).toString();
		m_interval = (Integer)m_properties.get(PROP_INTERVAL);
		
		if (m_tickHandle != null) {
			m_tickHandle.cancel(true);
		}
		
		m_tickHandle = m_tickExecutor.submit(new Runnable(){

			@Override
			public void run() {
				
				while (true){
					try {
						Thread.sleep(m_interval);
					} catch (InterruptedException e) {
					}
					m_wireSupport.emit(new WireRecord(new WireField(TIMER_EVENT_FIELD_NAME, new WireValueString(m_name))));
				}
			}});
	}
}
