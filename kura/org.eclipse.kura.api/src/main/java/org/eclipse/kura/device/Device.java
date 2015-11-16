package org.eclipse.kura.device;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireField;
import org.eclipse.kura.wire.WireRecord;

public interface Device extends WireEmitter{
	
	public WireRecord readDataRecord() throws KuraException;
	
	public WireField readDataField(String name) throws KuraException;
	
	public void writeDataField(WireField dataField) throws KuraException;
	
	public DeviceConnection getDeviceConnection();
	
	public int getPollingInterval();
	
	public int getFetchMode();
	
}
