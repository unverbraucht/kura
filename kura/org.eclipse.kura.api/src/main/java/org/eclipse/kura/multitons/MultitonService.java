package org.eclipse.kura.multitons;

import org.eclipse.kura.KuraException;

public interface MultitonService {
	
	public Object[] getRegisteredMultitonInstance(String factoryPid);
	
	public String newMultitonInstance(String factoryPid, boolean takeSnapshot, String instanceName) throws KuraException;
	
	public boolean removeMultitonInstance(String pid);
	
	public void addMultitonServiceRegistrationCallback(MultitonRegistrationCallback callback);
	
	public void removeMultitonServiceRegistrationCallback(MultitonRegistrationCallback callback);
}
