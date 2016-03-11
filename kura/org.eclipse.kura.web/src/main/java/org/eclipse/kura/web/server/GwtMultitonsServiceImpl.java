package org.eclipse.kura.web.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.multitons.MultitonService;
import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtMultitonsService;
import org.eclipse.kura.wires.WireEmitter;
import org.eclipse.kura.wires.WireReceiver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtMultitonsServiceImpl extends OsgiRemoteServiceServlet implements GwtMultitonsService {

	private static final Logger s_logger = LoggerFactory.getLogger(GwtMultitonsServiceImpl.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = -5137949662762233281L;

	@Override
	public List<String> getAvailableFactories(GwtXSRFToken xsrfToken) throws GwtKuraException {
		
		checkXSRFToken(xsrfToken);
		
		//MultitonService m_multitonService = ServiceLocator.getInstance().getService(MultitonService.class);
		ConfigurationService m_configService = ServiceLocator.getInstance().getService(ConfigurationService.class);
		
		List<String> result = new ArrayList<String>(); 
		Set<String> factories = m_configService.getComponentFactoryPids();
		result.addAll(factories);
		
		return result;
		
	}

	@Override
	public void newInstance(GwtXSRFToken xsrfToken, String factoryPid, String instanceName) throws GwtKuraException {
		checkXSRFToken(xsrfToken);
		
		ConfigurationService m_configService = ServiceLocator.getInstance().getService(ConfigurationService.class);
		try {
			m_configService.newConfigurableComponent(factoryPid, null, true, instanceName);
		} catch (KuraException e) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	@Override
	public List<String> getRegisteredInstances(GwtXSRFToken xsrfToken) throws GwtKuraException {
		checkXSRFToken(xsrfToken);
		
		ConfigurationService m_configService = ServiceLocator.getInstance().getService(ConfigurationService.class);
		MultitonService m_multitonService = ServiceLocator.getInstance().getService(MultitonService.class);
		List<String> result = new ArrayList<String>();
		for (String f : m_configService.getComponentFactoryPids()){
			for(Object o : m_multitonService.getRegisteredMultitonInstance(f)){
				result.add(o.toString());
			}
		}
		
		return result;
	}

	@Override
	public void deleteInstance(GwtXSRFToken xsrfToken, String servicePid) throws GwtKuraException {
		checkXSRFToken(xsrfToken);
		
		ConfigurationService m_configService = ServiceLocator.getInstance().getService(ConfigurationService.class);
		try {
			m_configService.deleteConfigurableComponent(servicePid);
		} catch (KuraException e) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	@Override
	public List<String> getRegisteredEmitters() throws GwtKuraException {
		BundleContext bundleContext = Console.getBundleContext();
		List<String> result = new ArrayList<String>();
		if(bundleContext != null){
			try {
				Collection<ServiceReference<WireEmitter>> srs = bundleContext.getServiceReferences(WireEmitter.class, null);
				for(ServiceReference<WireEmitter> sr : srs){
					if(sr != null){
						result.add(sr.getProperty("service.pid").toString());
					}
				}
			} catch (InvalidSyntaxException e) {
				throw GwtKuraException.internalError(e, "Could not retrieve services.");
			}
		}
		return result;
	}

	@Override
	public List<String> getRegisteredReceivers() throws GwtKuraException {
		BundleContext bundleContext = Console.getBundleContext();
		List<String> result = new ArrayList<String>();
		if(bundleContext != null){
			try {
				Collection<ServiceReference<WireReceiver>> srs = bundleContext.getServiceReferences(WireReceiver.class, null);
				for(ServiceReference<WireReceiver> sr : srs){
					if(sr != null){
						result.add(sr.getProperty("service.pid").toString());
					}
				}
			} catch (InvalidSyntaxException e) {
				throw GwtKuraException.internalError(e, "Could not retrieve services.");
			}
		}
		return result;
	}

}
