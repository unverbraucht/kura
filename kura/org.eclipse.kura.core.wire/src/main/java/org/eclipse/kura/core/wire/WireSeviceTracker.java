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
package org.eclipse.kura.core.wire;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.kura.wires.WireEmitter;
import org.eclipse.kura.wires.WireReceiver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WireSeviceTracker extends ServiceTracker{

	private static final Logger s_logger = LoggerFactory.getLogger(WireSeviceTracker.class);

	private WireServiceImpl m_wireService;
	
	private List<String> m_wireEmitters;
	private List<String> m_wireReceivers;
	
	@SuppressWarnings("unchecked")
	public WireSeviceTracker(BundleContext context, WireServiceImpl wireService) 
		throws InvalidSyntaxException 
	{
		//super(context, context.createFilter("(" + Constants.SERVICE_EXPORTED_INTERFACES + "="+WireComponent.class.getName()+")"), null); // This works but we track everything
		super(context, context.createFilter("(" + Constants.OBJECTCLASS + "=*)"), null);
		m_wireEmitters = new ArrayList<String>();
		m_wireReceivers = new ArrayList<String>();
		
		m_wireService = wireService;
	}

	@Override
	public void open() {
		super.open();
		
		try {
			Collection<ServiceReference<WireEmitter>> emitterRefs = context.getServiceReferences(WireEmitter.class, null);
			for(ServiceReference<?> ref : emitterRefs){
				m_wireEmitters.add((String) ref.getProperty("service.pid"));
			}
			Collection<ServiceReference<WireReceiver>> receiverRefs = context.getServiceReferences(WireReceiver.class, null);
			for(ServiceReference<?> ref : receiverRefs){
				m_wireReceivers.add((String) ref.getProperty("service.pid"));
			}			
		} catch (InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object addingService(ServiceReference reference) {
		Object service = super.addingService(reference);
		boolean flag = false;

		if(service instanceof WireEmitter){
			m_wireEmitters.add((String) reference.getProperty("service.pid"));
			System.err.println("REGISTRATO WIRE EMITTER:"+(String) reference.getProperty("service.pid"));
			flag = true;
		}

		if(service instanceof WireReceiver){
			m_wireReceivers.add((String) reference.getProperty("service.pid"));
			System.err.println("REGISTRATO WIRE RECEIVER:"+(String) reference.getProperty("service.pid"));
			flag = true;
		}

		if(flag){
			m_wireService.createWires();
		}
		
		return service;
	}
	
	public List<String> getWireEmitters(){
		return Collections.unmodifiableList(m_wireEmitters);
	}
	
	public List<String> getWireReceivers(){
		return Collections.unmodifiableList(m_wireReceivers);
	}
	
	
}
