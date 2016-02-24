package org.eclipse.kura.core.multitons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.multitons.MultitonRegistrationCallback;
import org.eclipse.kura.multitons.MultitonService;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultitonServiceImpl implements MultitonService, ServiceListener {

	private static final Logger s_logger = LoggerFactory.getLogger(MultitonService.class);

	private ConfigurationService m_configurationService;
	private ComponentContext m_ctx;

	private List<MultitonRegistrationCallback> m_callbacks;

	public void setConfigurationService(ConfigurationService configurationService) {
		m_configurationService = configurationService;
	}

	public void unsetConfigurationService(ConfigurationService configurationService) {
		m_configurationService = null;
	}

	public MultitonServiceImpl() {
		m_callbacks = new CopyOnWriteArrayList<MultitonRegistrationCallback>();
	}

	protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
		s_logger.info("Activating MultitonService...");
		m_ctx = componentContext;

		m_ctx.getBundleContext().addServiceListener(this);

		s_logger.info("Activating MultitonService... Done.");
	}

	protected void deactivate(ComponentContext componentContext) {
		s_logger.info("Deactivating MultitonService...");

		m_ctx.getBundleContext().removeServiceListener(this);

		s_logger.info("Deactivating MultitonService... Done.");
	}

	@Override
	public Object[] getRegisteredMultitonInstance(String factoryPid) {
		List<Object> result = new ArrayList<Object>();

		try {
			ServiceReference<?>[] services = m_ctx.getBundleContext().getServiceReferences(factoryPid, null);
			if (services != null) {
				for (ServiceReference<?> service : services) {
					Object factoryPidProp = service.getProperty("service.factoryPid");
					if (factoryPidProp != null) {
						if (factoryPidProp.toString().equals(factoryPid)) {
							result.add(m_ctx.getBundleContext().getService(service));
						}
					}
				}
			}
		} catch (InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result.toArray();
	}

	@Override
	public void addMultitonServiceRegistrationCallback(MultitonRegistrationCallback callback) {
		m_callbacks.add(callback);
	}

	@Override
	public void removeMultitonServiceRegistrationCallback(MultitonRegistrationCallback callback) {
		m_callbacks.remove(callback);
	}

	@Override
	public boolean removeMultitonInstance(String pid) {
		try {
			m_configurationService.deleteConfigurableComponent(pid);
			return true;
		} catch (KuraException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public String newMultitonInstance(String factoryPid) {

		String newPid = null;
		ComponentConfiguration compConfig;
		try {
			compConfig = m_configurationService.getComponentDefaultConfiguration(factoryPid);
			newPid = m_configurationService.newConfigurableComponent(factoryPid, compConfig.getConfigurationProperties());
		} catch (KuraException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return newPid;
	}

	@Override
	public void serviceChanged(ServiceEvent serviceEvent) {

		if (serviceEvent.getType() != ServiceEvent.REGISTERED && serviceEvent.getType() != ServiceEvent.UNREGISTERING) {
			return;
		}

		Object factoryPidProp = serviceEvent.getServiceReference().getProperty("service.factoryPid");
		if (factoryPidProp != null) {
			synchronized (m_callbacks) {
				for (MultitonRegistrationCallback c : m_callbacks) {
					try {
						if (serviceEvent.getServiceReference().getProperty("service.factoryPid").equals(c.getFactoryPidFilter())) {
							if (serviceEvent.getType() == ServiceEvent.REGISTERED) {
								c.MultitonComponentRegistered(serviceEvent.getServiceReference().getProperty("service.pid").toString(),
										serviceEvent.getServiceReference());
							} else {
								c.MultitonComponentUnregistered(serviceEvent.getServiceReference().getProperty("service.pid").toString(),
										serviceEvent.getServiceReference());
							}
						}
					} catch (NullPointerException ex) {
						// Do nothing. Multiton without "service.pid" property?
					}
				}
			}
		}
	}
}
