package org.eclipse.kura.protocol.modbus2;

import org.eclipse.kura.protocol.modbus2.api.ModbusProtocolDeviceService;

public class ModbusProtocolDeviceInfo {
	
	private String pid;
	private ModbusProtocolDeviceService service;
	
	
	public ModbusProtocolDeviceInfo(String pid, ModbusProtocolDeviceService service) {
		super();
		this.pid = pid;
		this.service = service;
	}
	
	public String getPid() {
		return pid;
	}
	
	public void setPid(String pid) {
		this.pid = pid;
	}
	
	public ModbusProtocolDeviceService getService() {
		return service;
	}
	
	public void setService(ModbusProtocolDeviceService service) {
		this.service = service;
	}
	
	
}
