package org.eclipse.kura.protocol.modbus2;

import org.eclipse.kura.protocol.modbus2.api.ModbusProtocolDeviceService;

public class ModbusProtocolDeviceInfo {
	
	private String name;
	private String pid;
	private ModbusProtocolDeviceService service;
	
	
	public ModbusProtocolDeviceInfo(String name, String pid, ModbusProtocolDeviceService service) {
		super();
		this.name = name;
		this.pid = pid;
		this.service = service;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
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
