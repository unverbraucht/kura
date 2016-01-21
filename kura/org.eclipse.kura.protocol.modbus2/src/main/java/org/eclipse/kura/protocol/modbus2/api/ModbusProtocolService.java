package org.eclipse.kura.protocol.modbus2.api;

public interface ModbusProtocolService {
	
	public void newDevice();
	
	public ModbusProtocolDeviceService getDevice(String name);
	
	
	public ModbusProtocolDeviceService[] listDevices();
	
	public boolean deleteDevice(String name);
	
}
