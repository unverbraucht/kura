/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.demo.heater;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.wires.WireEmitter;
import org.eclipse.kura.wires.WireField;
import org.eclipse.kura.wires.WireRecord;
import org.eclipse.kura.wires.WireSupport;
import org.eclipse.kura.wires.WireValueDouble;
import org.eclipse.kura.wires.WireValueInteger;
import org.eclipse.kura.wires.WireValueString;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Heater implements ConfigurableComponent, WireEmitter  
{	
	private static final Logger s_logger = LoggerFactory.getLogger(Heater.class);
	
	// Publishing Property Names
	private static final String   MODE_PROP_NAME           = "mode";
	private static final String   MODE_PROP_PROGRAM        = "Program";
	private static final String   MODE_PROP_MANUAL         = "Manual";
	private static final String   MODE_PROP_VACATION       = "Vacation";

	private static final String   PROGRAM_SETPOINT_NAME    = "program.setPoint";
	private static final String   MANUAL_SETPOINT_NAME     = "manual.setPoint";
	
	private static final String   TEMP_INITIAL_PROP_NAME   = "temperature.initial";
	private static final String   TEMP_INCREMENT_PROP_NAME = "temperature.increment";
	private static final String   PUBLISH_RATE_PROP_NAME   = "sample.rate";
	
	private ScheduledExecutorService m_worker;
	private ScheduledFuture<?>       m_handle;
	
	private ComponentContext         m_ctx;
	private float                    m_temperature;
	private Map<String, Object>      m_properties;
	private WireSupport              m_wireSupport;
	
	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------
	
	public Heater() 
	{
		super();
		m_worker = Executors.newSingleThreadScheduledExecutor();
	}
		
	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext, Map<String,Object> properties) 
	{
		s_logger.info("Activating Heater...");
		
		m_ctx = componentContext;
		m_properties = properties;
		m_wireSupport = new WireSupport(this);

		for (String s : properties.keySet()) {
			s_logger.info("Activate - "+s+": "+properties.get(s));
		}
		
		try  {
			
			// Don't subscribe because these are handled by the default 
			// subscriptions and we don't want to get messages twice			
			doUpdate(false);
		}
		catch (Exception e) {
			s_logger.error("Error during component activation", e);
			throw new ComponentException(e);
		}
		s_logger.info("Activating Heater... Done.");
	}
	
	
	protected void deactivate(ComponentContext componentContext) 
	{
		s_logger.debug("Deactivating Heater...");

		// shutting down the worker and cleaning up the properties
		m_worker.shutdown();
		
		s_logger.debug("Deactivating Heater... Done.");
	}	
	
	
	public void updated(Map<String,Object> properties)
	{
		s_logger.info("Updated Heater...");

		// store the properties received
		m_properties = properties;
		for (String s : properties.keySet()) {
			s_logger.info("Update - "+s+": "+properties.get(s));
		}
		
		// try to kick off a new job
		doUpdate(true);
		s_logger.info("Updated Heater... Done.");
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
	
		
	// ----------------------------------------------------------------
	//
	//   Private Methods
	//
	// ----------------------------------------------------------------

	/**
	 * Called after a new set of properties has been configured on the service
	 */
	private void doUpdate(boolean onUpdate) 
	{
		// cancel a current worker handle if one if active
		if (m_handle != null) {
			m_handle.cancel(true);
		}
		
		if (!m_properties.containsKey(TEMP_INITIAL_PROP_NAME) ||
		    !m_properties.containsKey(PUBLISH_RATE_PROP_NAME)) {
			s_logger.info("Update Heater - Ignore as properties do not contain TEMP_INITIAL_PROP_NAME and PUBLISH_RATE_PROP_NAME.");
			return;
		}
		
		// reset the temperature to the initial value
		if (!onUpdate) {
			m_temperature = (Float) m_properties.get(TEMP_INITIAL_PROP_NAME);
		}
		
		// schedule a new worker based on the properties of the service
		int pubrate = (Integer) m_properties.get(PUBLISH_RATE_PROP_NAME);
		m_handle = m_worker.scheduleAtFixedRate(new Runnable() {		
			@Override
			public void run() {
				Thread.currentThread().setName(getClass().getSimpleName());
				doPublish();
			}
		}, 0, pubrate, TimeUnit.SECONDS);
	}
	
	
	/**
	 * Called at the configured rate to publish the next temperature measurement.
	 */
	private void doPublish() 
	{				
		// Increment the simulated temperature value
		String    mode = (String)  m_properties.get(MODE_PROP_NAME);
		float setPoint = 0;
		float tempIncr = (Float) m_properties.get(TEMP_INCREMENT_PROP_NAME);
		if (MODE_PROP_PROGRAM.equals(mode)) {
			setPoint = (Float) m_properties.get(PROGRAM_SETPOINT_NAME);
		}
		else if (MODE_PROP_MANUAL.equals(mode)) {
			setPoint = (Float) m_properties.get(MANUAL_SETPOINT_NAME);
		}
		else if (MODE_PROP_VACATION.equals(mode)) {
			setPoint = 6.0F;			
		}
		if (m_temperature + tempIncr < setPoint) {
			m_temperature += tempIncr;
		}
		else {
			m_temperature -= 4*tempIncr;
		}

		WireRecord dataRecord = new WireRecord(new WireField("temperatureInternal", new WireValueDouble(m_temperature)),
											   new WireField("randomInt", new WireValueInteger((new Random()).nextInt())),
											   new WireField("name", 	  new WireValueString("aaa"+(new Random()).nextInt())),
										       new WireField("randomInt2", new WireValueInteger((new Random()).nextInt())));

    	s_logger.info("Emitting WireRecord with temperatureInternal {}...", m_temperature);
    	m_wireSupport.emit(dataRecord);
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
		s_logger.info("Got connected Wires {}"+wires.length);
		m_wireSupport.consumersConnected(wires);
	}
}
