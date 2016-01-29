package org.eclipse.kura.multitons;

public interface MultitonService {
	
	public Object[] getRegisteredMultitonInstance(String factoryPid);
	
	public String newMultitonInstance(String factoryPid);
	
	public boolean removeMultitonInstance(String pid);
	
	public void addMultitonServiceRegistrationCallback(MultitonRegistrationCallback callback);
	
	public void removeMultitonServiceRegistrationCallback(MultitonRegistrationCallback callback);
}
