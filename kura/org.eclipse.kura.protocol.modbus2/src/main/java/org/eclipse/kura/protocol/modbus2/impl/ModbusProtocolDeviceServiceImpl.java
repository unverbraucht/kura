package org.eclipse.kura.protocol.modbus2.impl;

import java.util.Map;
import java.util.Properties;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.protocol.modbus2.ModbusCommEvent;
import org.eclipse.kura.protocol.modbus2.ModbusProtocolException;
import org.eclipse.kura.protocol.modbus2.api.ModbusProtocolDeviceService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.wire.DeviceConnection;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusProtocolDeviceServiceImpl implements ConfigurableComponent, DeviceConnection, ModbusProtocolDeviceService {

	private Logger s_logger = LoggerFactory.getLogger(ModbusProtocolDeviceServiceImpl.class);
	
	private String m_instanceName;
	
	private SystemService m_systemService;
	
	public ModbusProtocolDeviceServiceImpl(){
		m_instanceName = new StringBuilder("modbus_").append(String.valueOf(System.currentTimeMillis())).toString();
	}
		
	protected void activate(ComponentContext componentContext, Map<String,Object> properties) 
	{
		s_logger.info("Activating ModbusProtocolDeviceService...");

		s_logger.info("Activating ModbusProtocolDeviceService... Done.");
	}
	
	
	protected void deactivate(ComponentContext componentContext) 
	{
		s_logger.info("Deactivating ModbusProtocolDeviceService...");

		s_logger.info("Deactivating ModbusProtocolDeviceService... Done.");
	}	
	
	protected void updated(Map<String,Object> properties) 
	{
		s_logger.info("Updating ModbusProtocolDeviceService...");

		s_logger.info("Updating ModbusProtocolDeviceService... Done.");
	}	
	
	@Override
	public String getInstanceName() {
		return m_instanceName;
	}

	@Override
	public void configureConnection(Properties connectionConfig) throws ModbusProtocolException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getConnectStatus() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void connect() {
		
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean[] readCoils(int unitAddr, int dataAddress, int count) throws ModbusProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean[] readDiscreteInputs(int unitAddr, int dataAddress, int count) throws ModbusProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void writeSingleCoil(int unitAddr, int dataAddress, boolean data) throws ModbusProtocolException {
		// TODO Auto-generated method stub

	}

	@Override
	public void writeMultipleCoils(int unitAddr, int dataAddress, boolean[] data) throws ModbusProtocolException {
		// TODO Auto-generated method stub

	}

	@Override
	public int[] readHoldingRegisters(int unitAddr, int dataAddress, int count) throws ModbusProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] readInputRegisters(int unitAddr, int dataAddress, int count) throws ModbusProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void writeSingleRegister(int unitAddr, int dataAddress, int data) throws ModbusProtocolException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean[] readExceptionStatus(int unitAddr) throws ModbusProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ModbusCommEvent getCommEventCounter(int unitAddr) throws ModbusProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ModbusCommEvent getCommEventLog(int unitAddr) throws ModbusProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void writeMultipleRegister(int unitAddr, int dataAddress, int[] data) throws ModbusProtocolException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void write(String address, Object value) throws KuraException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object read(String address) throws KuraException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDriverDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAddressSintax() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getHelp() {
		// TODO Auto-generated method stub
		return null;
	}

}
