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
package org.eclipse.kura.protocol.modbus2.wires;

import java.util.Map;

public class ModbusWireConfiguration {
	
	private static final String PROP_DRIVER_INSTANCE = "instanceName";
	private static final String PROP_SLAVE_ADDRESS = "slaveAddress";
	private static final String PROP_DATA_ADDRESS = "dataAddress";
	private static final String PROP_DATA_LENGTH = "dataLength";
	private static final String PROP_OPERATION = "operation";
	private static final String PROP_MODE = "operatingMode";
	private static final String PROP_POLLING = "pollingInterval";
	private static final String PROP_METRIC_NAME = "metricName";

	private String driverInstance = "";
	private int slaveAddress = 0;
	private int dataAddress = 0;
	private int dataLength = 0;
	private int operation = 0;
	private int mode = 0;
	private int polling = 10000;
	private String metricName = "";
	
	public ModbusWireConfiguration(Map<String, Object> properties){
		try{
			driverInstance = properties.get(PROP_DRIVER_INSTANCE).toString();
			metricName = properties.get(PROP_METRIC_NAME).toString();
			operation = (Integer)properties.get(PROP_OPERATION);
			slaveAddress = (Integer)properties.get(PROP_SLAVE_ADDRESS);
			dataAddress = (Integer)properties.get(PROP_DATA_ADDRESS);
			dataLength = (Integer)properties.get(PROP_DATA_LENGTH);
			mode = (Integer)properties.get(PROP_MODE);
			polling = (Integer) properties.get(PROP_POLLING);
		}catch(Exception e){
			//Error retrieving configuration. Throw exception?
		}
	}

	public String getMetricName() {
		return metricName;
	}

	public int getMode() {
		return mode;
	}

	public int getPolling() {
		return polling;
	}

	public String getDriverInstance() {
		return driverInstance;
	}

	public int getSlaveAddress() {
		return slaveAddress;
	}

	public int getDataAddress() {
		return dataAddress;
	}

	public int getDataLength() {
		return dataLength;
	}

	public int getOperation() {
		return operation;
	}
	
	
}
