package org.eclipse.kura.wire;

import org.eclipse.kura.KuraException;


public interface DeviceConnection {
	
	public void connect() throws KuraException;
	
	public void disconnect() throws KuraException;
	
	public boolean isConnected();
	
	public void write(String address, Object value) throws KuraException;
	public Object read(String address) throws KuraException;
	
	public String getDriverDescription();
	public String getAddressSintax();
	public String getHelp();
}
