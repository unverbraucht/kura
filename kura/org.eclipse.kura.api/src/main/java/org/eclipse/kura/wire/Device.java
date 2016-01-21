package org.eclipse.kura.wire;

import org.eclipse.kura.KuraException;

public interface Device extends WireEmitter{
	
	public WireRecord readDataRecord() throws KuraException;
	
	public WireField readDataField(String name) throws KuraException;
	
	public void writeDataField(WireField dataField) throws KuraException;
	
	public DeviceConnection getDeviceConnection();
	
	public int getPollingInterval();
	
	public int getFetchMode();
	
}
