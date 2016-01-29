package org.eclipse.kura.protocol.modbus2.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.multitons.MultitonRegistrationCallback;
import org.eclipse.kura.multitons.MultitonService;
import org.eclipse.kura.protocol.modbus2.ModbusProtocolDeviceInfo;
import org.eclipse.kura.protocol.modbus2.api.ModbusProtocolDeviceService;
import org.eclipse.kura.protocol.modbus2.api.ModbusProtocolService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.watchdog.WatchdogService;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusProtocolServiceImpl implements ModbusProtocolService, MultitonRegistrationCallback {

	private static final Logger s_logger = LoggerFactory.getLogger(ModbusProtocolServiceImpl.class);

	private static final String MODBUS_DEVICE_FACTORY_PID = "org.eclipse.kura.protocol.modbus2.impl.ModbusProtocolDeviceServiceImpl";

	private List<String> m_internalDevicePidList = new ArrayList<String>();
	private ComponentContext m_ctx;
	private Map<String, Object> m_properties;
	private MultitonService m_multitonService;
	private SystemService m_systemService;
	private WatchdogService m_watchdogService;

	private static List<ModbusProtocolDeviceInfo> m_activeConnections;

	// ----------------------------------------------------------------
	//
	// Dependencies
	//
	// ----------------------------------------------------------------

	public ModbusProtocolServiceImpl() {
		m_activeConnections = new ArrayList<ModbusProtocolDeviceInfo>();
	}

	public void setMultitonService(MultitonService multitonService) {
		m_multitonService = multitonService;	
	}

	public void unsetMultitonService(MultitonService multitonService) {
		m_multitonService = null;
	}

	public void setSystemService(SystemService systemService) {
		m_systemService = systemService;
	}

	public void unsetSystemService(SystemService systemService) {
		m_systemService = null;
	}

	public void setWatchdogService(WatchdogService watchdogService) {
		m_watchdogService = watchdogService;
	}

	public void unsetWatchdogService(WatchdogService watchdogService) {
		m_watchdogService = null;
	}

	// ----------------------------------------------------------------
	//
	// Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
		s_logger.info("Activating ModbusProtocolService...");

		m_ctx = componentContext;
		m_properties = properties;

		m_multitonService.addMultitonServiceRegistrationCallback(this);
		
		checkForExistingInstances();

		//addDeviceDebug();

		s_logger.info("Activating ModbusProtocolService... Done.");
	}

	protected void deactivate(ComponentContext componentContext) {
		s_logger.debug("Deactivating ModbusProtocolService...");

		m_multitonService.removeMultitonServiceRegistrationCallback(this);
		
		s_logger.debug("Deactivating ModbusProtocolService... Done.");
	}

	// ----------------------------------------------------------------
	//
	// ModbusService APIs
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
			for (ModbusProtocolDeviceInfo i : m_activeConnections) {
				if (i.getService().getInstanceName().equals(name)) {
					return i.getService();
				}
			}
		}

		return result;
	}

	@Override
	public ModbusProtocolDeviceService[] listDevices() {
		List<ModbusProtocolDeviceService> resultList = new ArrayList<ModbusProtocolDeviceService>();
		for (ModbusProtocolDeviceInfo i : m_activeConnections) {
			resultList.add(i.getService());
		}

		return resultList.toArray(new ModbusProtocolDeviceService[0]);
	}

	@Override
	public boolean deleteDevice(String name) {
		ModbusProtocolDeviceInfo dev = null;

		synchronized (m_activeConnections) {
			for (ModbusProtocolDeviceInfo i : m_activeConnections) {
				if (i.getService().getInstanceName().equals(name)) {
					dev = i;
					break;
				}
			}
		}

		if (dev != null) {
			m_multitonService.removeMultitonInstance(dev.getPid());
			return true;
		}

		return false;
	}

	// ----------------------------------------------------------------
	//
	// Multiton callbacks
	//
	// ----------------------------------------------------------------

	public void MultitonComponentRegistered(String pid, ServiceReference<?> service){
		System.err.println("MULTITON REGITRATION CALLBACK -> "+pid);
		ModbusProtocolDeviceService modbus_service = (ModbusProtocolDeviceService) m_ctx.getBundleContext().getService(service);
		ModbusProtocolDeviceInfo info= new ModbusProtocolDeviceInfo(pid, modbus_service);
		m_activeConnections.add(info);
	};
	
	public void MultitonComponentUnregistered(String pid, ServiceReference<?> service){
		System.err.println("MULTITON UNREGITRATION CALLBACK -> "+pid);
		ModbusProtocolDeviceService modbus_service = (ModbusProtocolDeviceService) m_ctx.getBundleContext().getService(service);
		ModbusProtocolDeviceInfo info = null;
		synchronized (m_activeConnections) {
			for(ModbusProtocolDeviceInfo i : m_activeConnections){
				if(i.getService().getInstanceName().equals(modbus_service.getInstanceName())){
					info = i;
					break;
				}
			}
			if(info != null){
				m_activeConnections.remove(info);
			}			
		}
	};
	
	public String getFactoryPidFilter(){
		return MODBUS_DEVICE_FACTORY_PID;
	};

	// ----------------------------------------------------------------
	//
	// Private methods
	//
	// ----------------------------------------------------------------

	private void checkForExistingInstances() {
		synchronized (m_activeConnections) {
			Object[] instances = m_multitonService.getRegisteredMultitonInstance(MODBUS_DEVICE_FACTORY_PID);
			for(Object o : instances){
				ModbusProtocolDeviceService instance = (ModbusProtocolDeviceService)o;
				ModbusProtocolDeviceInfo info = new ModbusProtocolDeviceInfo(instance.toString(), instance);
				m_activeConnections.add(info);
			}
		}
	}

	private String createModbusDeviceComponent(final String name) {
		return m_multitonService.newMultitonInstance(MODBUS_DEVICE_FACTORY_PID);
	}

	private void addDeviceDebug() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(0);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				newDevice();
			}
		}).start();
	}

}
