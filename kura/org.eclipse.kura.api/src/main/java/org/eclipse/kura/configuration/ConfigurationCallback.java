package org.eclipse.kura.configuration;

public interface ConfigurationCallback {
	
	public void componentRegistered(String pid);
	
	public void componentUnregistered(String pid);
	
	public String[] getFilter();
	
}
