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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.kura.KuraConnectionStatus;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.multitons.MultitonRegistrationCallback;
import org.eclipse.kura.multitons.MultitonService;
import org.eclipse.kura.protocol.modbus2.ModbusProtocolException;
import org.eclipse.kura.protocol.modbus2.api.ModbusProtocolDeviceService;
import org.eclipse.kura.wires.WireEmitter;
import org.eclipse.kura.wires.WireEnvelope;
import org.eclipse.kura.wires.WireField;
import org.eclipse.kura.wires.WireReceiver;
import org.eclipse.kura.wires.WireRecord;
import org.eclipse.kura.wires.WireSupport;
import org.eclipse.kura.wires.WireValueBoolean;
import org.eclipse.kura.wires.WireValueInteger;
import org.eclipse.kura.wires.WireValueString;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusReadWire implements WireEmitter, WireReceiver, ConfigurableComponent, MultitonRegistrationCallback {

	private static final Logger s_logger = LoggerFactory.getLogger(ModbusReadWire.class);

	private static final int OPERATION_READ_HOLDING_REGISTERS = 0;
	private static final int OPERATION_INPUT_REGISTERS = 1;
	private static final int OPERATION_READ_DISCRETE_INPUTS = 2;
	private static final int OPERATION_COILS = 3;
	private static final int OPERATION_EXCEPTION_STATUS = 4;

	private WireSupport m_wireSupport;

	private ModbusProtocolDeviceService m_connection;
	private MultitonService m_MultitonService;

	private Map<String, Object> m_properties;
	private ModbusWireConfiguration m_config;
	
	private Future<?> m_pollingHandle;
	private ExecutorService m_pollingExecutor;

	private ComponentContext m_ctx;
	
	public ModbusReadWire() {
		m_wireSupport = new WireSupport(this);
		m_pollingExecutor = Executors.newSingleThreadExecutor();
	}

	// ----------------------------------------------------------------
	//
	// Dependencies
	//
	// ----------------------------------------------------------------

	public void setMultitonService(MultitonService multitonService) {
		m_MultitonService = multitonService;
	}

	public void unsetMultitonService(MultitonService multitonService) {
		m_MultitonService = null;
	}

	protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
		s_logger.info("Activating ModbusReadWire...");

		m_ctx = componentContext;
		updated(properties);

		s_logger.info("Activating ModbusReadWire... Done.");
	}

	protected void deactivate(ComponentContext componentContext) {
		s_logger.info("Deactivating ModbusReadWire...");

		if (m_pollingHandle != null) {
			m_pollingHandle.cancel(true);
		}

		m_pollingExecutor.shutdown();
		
		if(m_connection != null){
			try {
				m_connection.disconnect();
			} catch (KuraException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		s_logger.info("Deactivating ModbusReadWire... Done.");
	}

	protected void updated(Map<String, Object> properties) {
		s_logger.info("Updating ModbusReadWire...");

		m_properties = properties;
		m_config = new ModbusWireConfiguration(m_properties);
		
		Object[] drivers = m_MultitonService.getRegisteredMultitonInstance("org.eclipse.kura.protocol.modbus2.impl.ModbusProtocolDeviceServiceImpl");
		for(Object d : drivers){
			ModbusProtocolDeviceService ds = (ModbusProtocolDeviceService)d;
			if(ds.getInstanceName() .equals(m_config.getDriverInstance())){
				m_connection = ds;
			}
		}

		if (m_pollingHandle != null) {
			m_pollingHandle.cancel(true);
		}
		
		if(m_config.getMode() == 0){
			m_pollingHandle = m_pollingExecutor.submit(m_runnable);
		}
		
		s_logger.info("Updating ModbusReadWire... Done.");
	}

	// ----------------------------------------------------------------
	//
	// Wire Emitter
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
		return "Modbus Channel Wire";
	}

	// ----------------------------------------------------------------
	//
	// Wire Receiver
	//
	// ----------------------------------------------------------------

	@Override
	public void producersConnected(Wire[] wires) {
		m_wireSupport.producersConnected(wires);
	}

	@Override
	public void updated(Wire wire, Object value) {
		m_wireSupport.updated(wire, value);
	}

	@Override
	public void onWireReceive(WireEnvelope wireEvelope) {
		m_wireSupport.emit(readData());
	}

	// ----------------------------------------------------------------
	//
	// Wire Device
	//
	// ----------------------------------------------------------------

//	@Override
//	public WireRecord readDataRecord() throws KuraException {
//		return readData();
//	}
//
//	@Override
//	public WireField readDataField(String name) throws KuraException {
//		WireRecord wr = readData();
//		for(WireField f : wr.getFields()){
//			if(f.getName().equals(name)){
//				return f;
//			}
//		}
//		throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
//	}
//
//	@Override
//	public void writeDataField(WireField dataField) throws KuraException {
//		// Do nothing. This is only a reader
//
//	}

//	@Override
//	public DeviceDriver getDeviceConnection() {
//		return (DeviceDriver) m_connection;
//	}
//
//	@Override
//	public int getPollingInterval() {
//		return m_config.getPolling();
//	}
//
//	@Override
//	public int getFetchMode() {
//		return m_config.getMode();
//	}

	// ----------------------------------------------------------------
	//
	// Private methods
	//
	// ----------------------------------------------------------------

	private WireRecord readData() {
		if(m_connection == null){
			emitException(new KuraException(KuraErrorCode.NOT_CONNECTED));
			return null;
		}
		if(m_connection.getConnectStatus() != KuraConnectionStatus.CONNECTED){
			try {
				m_connection.connect();
			} catch (KuraException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			switch (m_config.getOperation()) {
			case OPERATION_READ_HOLDING_REGISTERS:
				int[] hr_result = m_connection.readHoldingRegisters(m_config.getSlaveAddress(), m_config.getDataAddress(), m_config.getDataLength());
				if (hr_result.length == 1) {
					return new WireRecord(new WireField(m_config.getMetricName(), new WireValueInteger(hr_result[0])));
				} else {
					WireField[] records = new WireField[hr_result.length];
					for (int i = 0; i < hr_result.length; i++) {
						records[i] = new WireField(m_config.getMetricName() + "_" + String.valueOf(i), new WireValueInteger(hr_result[i]));
					}
					return new WireRecord(records);
				}
			case OPERATION_INPUT_REGISTERS:
				int[] ir_result = m_connection.readInputRegisters(m_config.getSlaveAddress(), m_config.getDataAddress(), m_config.getDataLength());
				if (ir_result.length == 1) {
					return new WireRecord(new WireField(m_config.getMetricName(), new WireValueInteger(ir_result[0])));
				} else {
					WireField[] records = new WireField[ir_result.length];
					for (int i = 0; i < ir_result.length; i++) {
						records[i] = new WireField(m_config.getMetricName() + "_" + String.valueOf(i), new WireValueInteger(ir_result[i]));
					}
					return new WireRecord(records);
				}
			case OPERATION_READ_DISCRETE_INPUTS:
				boolean[] di_result = m_connection.readDiscreteInputs(m_config.getSlaveAddress(), m_config.getDataAddress(), m_config.getDataLength());
				if(di_result.length == 1){
					return new WireRecord(new WireField(m_config.getMetricName(), new WireValueBoolean(di_result[0])));
				}else{
					WireField[] records = new WireField[di_result.length];
					for (int i = 0; i < di_result.length; i++) {
						records[i] = new WireField(m_config.getMetricName() + "_" + String.valueOf(i), new WireValueBoolean(di_result[i]));
					}
					return new WireRecord(records);					
				}
			case OPERATION_COILS:
				boolean[] c_result = m_connection.readCoils(m_config.getSlaveAddress(), m_config.getDataAddress(), m_config.getDataLength());
				if(c_result.length == 1){
					return new WireRecord(new WireField(m_config.getMetricName(), new WireValueBoolean(c_result[0])));
				}else{
					WireField[] records = new WireField[c_result.length];
					for (int i = 0; i < c_result.length; i++) {
						records[i] = new WireField(m_config.getMetricName() + "_" + String.valueOf(i), new WireValueBoolean(c_result[i]));
					}
					return new WireRecord(records);					
				}
			case OPERATION_EXCEPTION_STATUS:
				boolean[] e_result = m_connection.readExceptionStatus(m_config.getSlaveAddress());
				if(e_result.length == 1){
					return new WireRecord(new WireField(m_config.getMetricName(), new WireValueBoolean(e_result[0])));
				}else{
					WireField[] records = new WireField[e_result.length];
					for (int i = 0; i < e_result.length; i++) {
						records[i] = new WireField(m_config.getMetricName() + "_" + String.valueOf(i), new WireValueBoolean(e_result[i]));
					}
					return new WireRecord(records);					
				}
			}
		} catch (ModbusProtocolException e) {
			emitException(e);
		}
		return null;
	}

	private void emitException(Exception ex) {
		m_wireSupport.emit(new WireRecord(new WireField("Exception", new WireValueString(ex.getMessage()))));
	}
	
	private Runnable m_runnable = new Runnable() {
		@Override
		public void run() {
			try {
				while (true) {
//					int pi = getPollingInterval();
//					if (pi != -1) {
//						Thread.sleep(pi);
//
//						try {
//							WireRecord wr = readDataRecord();
//							m_wireSupport.emit(wr);
//						} catch (Exception ex) {
//							emitException(ex);
//						}
//					} else {
						Thread.sleep(5000);
//					}
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Throwable t) {
				s_logger.error("SOMETHING WENT WRONG!", t);
			}
		}
	};
	
	// ----------------------------------------------------------------
	//
	// Multiton Registration Callback
	//
	// ----------------------------------------------------------------

	@Override
	public String getFactoryPidFilter() {
		return "org.eclipse.kura.protocol.modbus2.impl.ModbusProtocolDeviceServiceImpl";
	}
	
	@Override
	public void MultitonComponentUnregistered(String pid, ServiceReference<?> service) {
		ModbusProtocolDeviceService s = (ModbusProtocolDeviceService)m_ctx.getBundleContext().getService(service);
		if (s.getInstanceName() == m_connection.getInstanceName()){
			m_connection = null;
		}
	}
	
	@Override
	public void MultitonComponentRegistered(String pid, ServiceReference<?> service) {
		ModbusProtocolDeviceService s = (ModbusProtocolDeviceService)m_ctx.getBundleContext().getService(service);
		System.err.println("FACTORY REGISTERED! - "+s.getInstanceName());
		if((m_connection == null) && (s.getInstanceName().equals(m_config.getDriverInstance()))){
			m_connection = s;
		}
	}
}
