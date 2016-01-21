package org.eclipse.kura.protocol.modbus2.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraConnectionStatus;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationCallback;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.protocol.modbus2.ModbusProtocolDeviceInfo;
import org.eclipse.kura.protocol.modbus2.ModbusProtocolException;
import org.eclipse.kura.protocol.modbus2.api.ModbusProtocolDeviceService;
import org.eclipse.kura.protocol.modbus2.api.ModbusProtocolService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.watchdog.WatchdogService;
import org.eclipse.kura.wire.WireEmitter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusProtocolServiceImpl implements ModbusProtocolService {

	private static final Logger s_logger = LoggerFactory.getLogger(ModbusProtocolServiceImpl.class);
	
	private static final String MODBUS_DEVICE_FACTORY_PID="org.eclipse.kura.protocol.modbus2.impl.ModbusDeviceServiceImpl";
	
	private List<String> m_internalDevicePidList = new ArrayList<String>();
	private ComponentContext m_ctx;
	private Map<String,Object> m_properties;
	private ConfigurationService m_configurationService;
	private SystemService m_systemService;
	private WatchdogService m_watchdogService;
	
	private static List<ModbusProtocolDeviceInfo> m_activeConnections;
	
	
	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------

	public ModbusProtocolServiceImpl(){
		m_activeConnections = new ArrayList<ModbusProtocolDeviceInfo>();
	}
	
	public void setConfigurationService(ConfigurationService configurationService){
		m_configurationService = configurationService;
	}
	
	public void unsetConfigurationService(ConfigurationService configurationService){
		m_configurationService = null;
	}
	
	public void setSystemService(SystemService systemService){
		m_systemService = systemService;
	}
	
	public void unsetSystemService(SystemService systemService){
		m_systemService = null;
	}
	
	public void setWatchdogService(WatchdogService watchdogService){
		m_watchdogService = watchdogService;
	}
	
	public void unsetWatchdogService(WatchdogService watchdogService){
		m_watchdogService = null;
	}
	
	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext, Map<String,Object> properties) 
	{
		s_logger.info("Activating ModbusProtocolService...");
		
		m_ctx = componentContext;
		m_properties = properties;
		
		addDeviceDebug();

		s_logger.info("Activating ModbusProtocolService... Done.");
	}
	
	private void addDeviceDebug(){
		new Thread(new Runnable(){

			@Override
			public void run() {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				newDevice();
				
			}}).start();
	}
	
	protected void deactivate(ComponentContext componentContext) 
	{
		s_logger.debug("Deactivating ModbusProtocolService...");
		
		s_logger.debug("Deactivating ModbusProtocolService... Done.");
	}	
	
	
	// ----------------------------------------------------------------
	//
	//   ModbusService APIs
	//
	// ----------------------------------------------------------------


	
	@Override
	public void newDevice() {
		
		createModbusDeviceComponent("ProvaNewDevice");
		
	}

	@Override
	public ModbusProtocolDeviceService getDevice(String name) {
		ModbusProtocolDeviceService result = null;
		synchronized (m_activeConnections) {
			for(ModbusProtocolDeviceInfo i : m_activeConnections){
				if(i.getName().equals(name)){
					return i.getService();
				}
			}
		}
		
		return result;
	}

	@Override
	public ModbusProtocolDeviceService[] listDevices() {
		List<ModbusProtocolDeviceService> resultList = new ArrayList<ModbusProtocolDeviceService>();
		for(ModbusProtocolDeviceInfo i : m_activeConnections){
			resultList.add(i.getService());
		}
		
		return resultList.toArray(new ModbusProtocolDeviceService[0]);
	}

	@Override
	public boolean deleteDevice(String name) {
		ModbusProtocolDeviceInfo dev = null;
		
		synchronized(m_activeConnections){
			for(ModbusProtocolDeviceInfo i : m_activeConnections){
				if(i.getName().equals(name)){
					dev = i;
					break;
				}
			}
		}
		
		if(dev != null){
			try {
				m_configurationService.deleteComponent(dev.getPid());
			} catch (KuraException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			return true;
		}
		
		return false;
	}

	// ----------------------------------------------------------------
	//
	//   Private methods
	//
	// ----------------------------------------------------------------

	private String createModbusDeviceComponent(final String name){
		String newPid = null;
		try {
			ComponentConfiguration compConfig;
			compConfig = m_configurationService.getComponentDefaultConfiguration(MODBUS_DEVICE_FACTORY_PID);
			newPid = m_configurationService.createComponent(MODBUS_DEVICE_FACTORY_PID, compConfig.getConfigurationProperties());
			final String passedPid = newPid;
			
//			synchronized (m_activeConnections) {
//				m_activeConnections.add(new ModbusProtocolDeviceInfo(name, passedPid, getDeviceFromPid(passedPid)));
//			}
			
			m_configurationService.addComponentRegistrationCallback(new ConfigurationCallback() {
				
				@Override
				public String[] getFilter() {
					return new String[]{passedPid};
				}
				
				@Override
				public void componentUnregistered(String pid) {
					// Do nothing
				}
				
				@Override
				public void componentRegistered(String pid) {
					synchronized (m_activeConnections) {
						m_activeConnections.add(new ModbusProtocolDeviceInfo(name, passedPid, getDeviceFromPid(passedPid)));
					}
					m_configurationService.removeComponentRegistrationCallback(this);
				}
			});
		} catch (Exception ex) {
	
		}
		return newPid;
	}
	
	private ModbusProtocolDeviceService getDeviceFromPid(String pid){
		try {
			Collection<ServiceReference<ModbusProtocolDeviceService>> services = m_ctx.getBundleContext().getServiceReferences(ModbusProtocolDeviceService.class, null);
			for (ServiceReference<ModbusProtocolDeviceService> service : services) {
				if( service.getProperty("service.pid").equals(pid)){
					return m_ctx.getBundleContext().getService(service);
				}
			}
		} catch (InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null; 
	}

}
