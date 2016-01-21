package org.eclipse.kura.core.wire;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireReceiver;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.ComponentContext;

public class WireUtils {

	public static boolean isEmitter(ComponentContext ctx, String pid){
		try {
			Collection<ServiceReference<WireEmitter>> services = ctx.getBundleContext().getServiceReferences(WireEmitter.class, null);
			for (ServiceReference<?> service : services) {
				if( service.getProperty("service.pid").equals(pid)){
					return true;
				}
			}
		} catch (InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean isReceiver(ComponentContext ctx, String pid){
		try {
			Collection<ServiceReference<WireReceiver>> services = ctx.getBundleContext().getServiceReferences(WireReceiver.class, null);
			for (ServiceReference<?> service : services) {
				if(service.getProperty("service.pid").equals(pid)){
					return true;
				}
			}
		} catch (InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<String> getFactoriesAndInstances(ComponentContext ctx, String factoryPid, Class iface) {
		ArrayList<String> result = new ArrayList<String>();
		// Iterate through the bundles
		for (Bundle b : ctx.getBundleContext().getBundles()) {
			// Search for a possible candidate for the factoryPid
			if (factoryPid.startsWith(b.getSymbolicName())) {
				// Try instantiating the factory. If it fails, move on to next
				// iteration
				try {
					ClassLoader cl = b.adapt(BundleWiring.class).getClassLoader();
					Class<?> clazz = Class.forName(factoryPid, false, cl);
					// If it doesn't fail introspect for the interface
					if (iface.isAssignableFrom(clazz)) {
						// Found a class implementing the interface.
						result.add("FACTORY|" + factoryPid);
					} else {
						// Found the class, but it doesn't implement the
						// interface.
						// Probably another multiton component.
						break;
					}
				} catch (ClassNotFoundException e) {
					// Do nothing. Wrong bundle or error.
					// Should we log something?
				}
			}
		}

		// After the factories, iterate through available services implementing
		// the passed interface
		try {
			Collection<ServiceReference<?>> services = ctx.getBundleContext().getServiceReferences(iface, null);
			for (ServiceReference<?> service : services) {
				result.add("INSTANCE|" + service.getProperty("service.pid"));
			}
		} catch (InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	public static List<String> getEmittersAndConsumers(ComponentContext ctx){
		ArrayList<String> result = new ArrayList<String>();
		
		try {
			Collection<ServiceReference<WireEmitter>> emitters = ctx.getBundleContext().getServiceReferences(WireEmitter.class, null);
			for (ServiceReference<WireEmitter> service : emitters) {
				result.add(service.getProperty("service.pid").toString());
			}
			Collection<ServiceReference<WireReceiver>> consumers = ctx.getBundleContext().getServiceReferences(WireReceiver.class, null);
			for (ServiceReference<WireReceiver> service : consumers) {
				result.add(service.getProperty("service.pid").toString());
			}
		} catch (InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
		
	}

}
