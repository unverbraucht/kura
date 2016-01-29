package org.eclipse.kura.wire;

public interface WireService {

	public String createWireComponent(String factoryPid);
	
	public boolean removeWireComponent(String pid);
	
	public void createWire(String emitterPid, String consumerPid);
	
	public boolean removeWire(String emitterPid, String consumerPid);
	
}
