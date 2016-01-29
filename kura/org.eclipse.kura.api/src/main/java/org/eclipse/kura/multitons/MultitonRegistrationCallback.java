package org.eclipse.kura.multitons;

import org.osgi.framework.ServiceReference;

public interface MultitonRegistrationCallback {

	public void MultitonComponentRegistered(String pid, ServiceReference<?> service);
	
	public void MultitonComponentUnregistered(String pid, ServiceReference<?> service);
	
	public String getFactoryPidFilter();
	
}
