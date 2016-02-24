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
package org.eclipse.kura.example.wire.device;

import java.util.Random;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.devices.DeviceDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DriverExample implements DeviceDriver {
	private static final Logger s_logger = LoggerFactory.getLogger(DriverExample.class);
	private boolean m_connected = false;
	
	private Random m_random = new Random();
	
	public DriverExample(){
		//Random values
	}
	
	@Override
	public void connect() throws KuraException {
		m_connected = true;
	}

	@Override
	public void disconnect() throws KuraException {
		m_connected = false;
	}

	@Override
	public boolean isConnected() {
		return m_connected;
	}

	@Override
	public String getDeviceFactoryPid() {
		return "org.eclipse.kura.example.wire.device.DeviceExample";
	}
	
	public int readInteger(int address){
		return m_random.nextInt();
	}
	
	public boolean readBoolean(int address){
		return m_random.nextBoolean();
	}
	
	public void writeInteger(int address, int value){
		s_logger.info("Writing integer {} to address {}", value, address);
	}
	
	public void writeBoolean(int address, boolean value){
		s_logger.info("Writing boolean {} to address {}", value, address);		
	}

}
