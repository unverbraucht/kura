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
package org.eclipse.kura.core.devices;

import java.util.Collection;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

@SuppressWarnings("rawtypes")
public class DeviceDriverTracker extends ServiceTracker{

	private String m_driverPid;
	private BundleContext m_ctx;
	private WireDevice m_wireDevice;
	
	@SuppressWarnings("unchecked")
	public DeviceDriverTracker(BundleContext context, WireDevice device, String driverPid) throws InvalidSyntaxException {
		super(context, context.createFilter("(" + Constants.OBJECTCLASS + "=*)"), null);
		m_driverPid = driverPid;
		m_ctx = context;
		m_wireDevice = device;
	}

	@Override
	public void open() {
		super.open();
		try {
			Collection<ServiceReference<DeviceDriver>> driverRefs = context.getServiceReferences(DeviceDriver.class, null);
			for(ServiceReference<DeviceDriver> ref : driverRefs){
				if(ref.getProperty("instance.name").equals(m_driverPid)){
					System.err.println("Driver found in open!!");
					m_wireDevice.setDeviceDriverInstance(context.getService(ref));
				}
			}
		} catch (InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object addingService(ServiceReference reference) {
		Object service = super.addingService(reference);
		
		if(service instanceof DeviceDriver){
			if(reference.getProperty("instance.name").equals(m_driverPid)){
				System.err.println("Driver found in addingService!!");
				m_wireDevice.setDeviceDriverInstance((DeviceDriver)service);
			}
		}
		return service;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void removedService(ServiceReference reference, Object service) {
		super.removedService(reference, service);
		if(service instanceof DeviceDriver){
			if(reference.getProperty("instance.name").equals(m_driverPid)){
				m_wireDevice.setDeviceDriverInstance(null);
			}
		}
	}
	
}
