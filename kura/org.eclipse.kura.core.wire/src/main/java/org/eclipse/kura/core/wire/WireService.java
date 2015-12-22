package org.eclipse.kura.core.wire;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.event.ListSelectionEvent;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationCallback;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireReceiver;
import org.json.JSONException;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.wireadmin.Wire;
import org.osgi.service.wireadmin.WireAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WireService implements SelfConfiguringComponent, ConfigurationCallback {
	private static final Logger s_logger = LoggerFactory.getLogger(WireService.class);

	private static final String PROP_PRODUCER_PID = "wireadmin.producer.pid";
	private static final String PROP_CONSUMER_PID = "wireadmin.consumer.pid";
	private static final String PROP_PID = "org.eclipse.kura.core.wire.WireService";

	private ComponentContext m_ctx;

	private WireAdmin m_wireAdmin;
	private ConfigurationService m_configService;
	private WireServiceOptions m_options;

	// Debug variables
	private String m_cloudPubPid;
	private String m_devExamplePid;
	private String m_devExamplePid2;
	private String m_modbusExamplePid;

	private List<WireConfiguration> m_wireConfig;
	private Map<String, Object> m_properties;

	private boolean shouldPersist = false;

	private String[] configurationCallbackFilter;
	private Set<String> all_wires_pids;

	// ----------------------------------------------------------------
	//
	// Dependencies
	//
	// ----------------------------------------------------------------

	public WireService() {
		m_wireConfig = new ArrayList<WireConfiguration>();
		all_wires_pids = new HashSet<String>();
	}

	public void setWireAdmin(WireAdmin wireAdmin) {
		this.m_wireAdmin = wireAdmin;
	}

	public void unsetWireAdmin(WireAdmin wireAdmin) {
		this.m_wireAdmin = null;
	}

	public WireAdmin getWireAdmin() {
		return m_wireAdmin;
	}

	public void setConfigurationService(ConfigurationService configService) {
		this.m_configService = configService;
	}

	public void unsetConfigurationService(ConfigurationService configService) {
		this.m_configService = null;
	}

	public ConfigurationService getConfigurationService() {
		return m_configService;
	}

	// ----------------------------------------------------------------
	//
	// Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext, Map<String, Object> properties) throws ComponentException {
		s_logger.info("activate...");

		// save the bundle context and the properties
		m_ctx = componentContext;
		try {
			m_configService.addComponentRegistrationCallback(this);
			m_options = WireServiceOptions.newInstance(properties);
			m_properties = properties;
			createWires();
		} catch (JSONException jsone) {
			throw new ComponentException(jsone);
		}

		// initDebugData();

	}

	private void initDebugData() {

		try {
			all_wires_pids = new HashSet<String>();
			all_wires_pids.add("org.eclipse.kura.core.wire.WireService");

			m_cloudPubPid = createComponent("org.eclipse.kura.core.wire.cloud.publisher.CloudPublisher");

			String dbStorePubPid = createComponent("org.eclipse.kura.core.wire.store.DbWireRecordStore");

			String dbRecordFilterPubPid = createComponent("org.eclipse.kura.core.wire.store.DbWireRecordFilter");

			String exampleDevicePubPid = createComponent("org.eclipse.kura.example.wire.device.DeviceExample");

			String heaterPid = "org.eclipse.kura.demo.heater.Heater";
			// Wire wire = m_wireAdmin.createWire(heaterPid, m_cloudPubPid,
			// null);
			// s_logger.info("Created Wire between {} and {}.", heaterPid,
			// m_cloudPubPid);
			// s_logger.info("Wire connected status: {}", wire.isConnected());

			createNewWire(heaterPid, dbStorePubPid);
			createNewWire(exampleDevicePubPid, dbStorePubPid);
			createNewWire(dbRecordFilterPubPid, m_cloudPubPid);

			Wire[] wires = m_wireAdmin.getWires(null);
			System.err.println("wires: " + wires);

			for (Wire w : wires) {
				String prod = w.getProperties().get(PROP_PRODUCER_PID).toString();
				String cons = w.getProperties().get(PROP_CONSUMER_PID).toString();
				WireConfiguration wc = new WireConfiguration(prod, cons, null);
				m_wireConfig.add(wc);
			}

			configurationCallbackFilter = all_wires_pids.toArray(new String[] {});

			shouldPersist = true;
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

	}

	private Wire createNewWire(String producerPid, String consumerPid) {
		Wire wire1 = m_wireAdmin.createWire(producerPid, consumerPid, null);
		s_logger.info("Created Wire between {} and {}.", producerPid, consumerPid);
		s_logger.info("Wire connected status: {}", wire1.isConnected());
		return wire1;
	}

	private String createComponent(String factoryPid) {
		String newPid = null;
		try {
			ComponentConfiguration compConfig;
			compConfig = m_configService.getComponentDefaultConfiguration(factoryPid);
			newPid = m_configService.createComponent(factoryPid, compConfig.getConfigurationProperties());
			all_wires_pids.add(newPid);
			s_logger.info("Created {} instance with pid {}", factoryPid, newPid);
		} catch (Exception ex) {

		}
		return newPid;
	}

	public void updated(Map<String, Object> properties) {
		s_logger.info("updated...: " + properties);

		for (String s : properties.keySet()) {
			System.out.println(s + " = " + properties.get(s).toString());
		}
		Object emitterPid = properties.get("emitter.pids");
		Object consumerPid = properties.get("consumer.pids");

		if (emitterPid != null && consumerPid != null) {
			if (!emitterPid.toString().equals("NONE") && !consumerPid.toString().equals("NONE")) {
				String emitterString = createComponentFromProperty(emitterPid.toString());
				String consumerString = createComponentFromProperty(consumerPid.toString());
				createNewWire(emitterString, consumerString);

				WireConfiguration wc = new WireConfiguration(emitterString, consumerString, null);
				m_wireConfig.add(wc);

				persistWires();
			}
		}

		Object wiresDelete = properties.get("delete.wires");
		if (wiresDelete != null) {
			if (!wiresDelete.toString().equals("NONE")) {
				int index = Integer.valueOf(wiresDelete.toString());
				WireConfiguration wc = m_wireConfig.get(index);
				removeWire(wc);

				persistWires();
			}
		}
		
		Object instancesDelete = properties.get("delete.instances");
		if(instancesDelete != null){
			if(!instancesDelete.toString().equals("NONE")){
				//First delete the wires
				WireConfiguration[] copy = m_wireConfig.toArray(new WireConfiguration[]{});
				for(WireConfiguration wc : copy){
					if(wc.getProducerPid().equals(instancesDelete.toString()) ||
					   wc.getConsumerPid().equals(instancesDelete.toString())){
						removeWire(wc);
					}
				}
				//Then delete the instance
				try {
					m_configService.deleteComponent(instancesDelete.toString());
				} catch (KuraException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				persistWires();
			}
		}

	}

	private String createComponentFromProperty(String value) {
		String[] tokens = value.split("\\|");
		if (tokens[0].equals("FACTORY")) {
			return createComponent(tokens[1]);
		} else {
			return tokens[1];
		}
	}

	protected void deactivate(ComponentContext componentContext) {
		s_logger.info("deactivate...");
		m_configService.removeComponentRegistrationCallback(this);
	}

	// ----------------------------------------------------------------
	//
	// Kura Wire APIs
	//
	// ----------------------------------------------------------------

	// ----------------------------------------------------------------
	//
	// Private methods
	//
	// ----------------------------------------------------------------
	private void createWires() {

		// TODO: remove existing wires?

		Map<String, String> multitonsMap = m_configService.getMultitonPidsMap();

		all_wires_pids.add("org.eclipse.kura.core.wire.WireService");
		configurationCallbackFilter = all_wires_pids.toArray(new String[] {});

		for (WireConfiguration conf : m_options.getWireConfigurations()) {
			String producer = conf.getProducerPid();
			try {
				String p = conf.getProducerPid();
				Object po = multitonsMap.get(p);
				producer = po.toString();
			} catch (Exception ex) {

			}
			String consumer = conf.getConsumerPid();
			try {
				String c = conf.getConsumerPid();
				Object co = multitonsMap.get(c);
				consumer = co.toString();
			} catch (Exception ex) {

			}

			m_wireAdmin.createWire(producer, consumer, null);
			m_wireConfig.add(new WireConfiguration(producer, consumer, null));
		}
	}

	private void persistWires() {
		List<WireConfiguration> list = m_options.getWires();
		list.clear();
		for (WireConfiguration w : m_wireConfig) {
			list.add(w);
		}
		HashMap<String, Object> new_props = new HashMap<String, Object>(m_properties);
		try {
			String jsonString = m_options.toJsonString();
			new_props.put(WireServiceOptions.CONF_WIRES, jsonString);
			m_configService.updateConfiguration("org.eclipse.kura.core.wire.WireService", new_props, shouldPersist);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KuraException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public WireConfiguration createWire(String producerPid, String consumerPid) {
		m_wireAdmin.createWire(producerPid, consumerPid, null);
		WireConfiguration conf = new WireConfiguration(producerPid, consumerPid, null);
		m_wireConfig.add(conf);
		return conf;
	}

	public Wire removeWire(WireConfiguration theWire) {
		try {
			Wire[] list = m_wireAdmin.getWires(null);
			for (Wire w : list) {
				String prod = w.getProperties().get(PROP_PRODUCER_PID).toString();
				String cons = w.getProperties().get(PROP_CONSUMER_PID).toString();
				if (prod.equals(theWire.getProducerPid()) && cons.equals(theWire.getConsumerPid())) {
					m_wireAdmin.deleteWire(w);
					m_wireConfig.remove(theWire);
					return w;
				}
			}
		} catch (InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public WireConfiguration removeWire(Wire theWire) {
		m_wireAdmin.deleteWire(theWire);
		for (WireConfiguration c : m_wireConfig) {
			if (c.getProducerPid().equals(theWire.getProperties().get(PROP_PRODUCER_PID))
					&& c.getConsumerPid().equals(theWire.getProperties().get(PROP_CONSUMER_PID))) {
				m_wireConfig.remove(c);
				return c;
			}
		}
		return null;
	}

	@Override
	public void componentRegistered(String pid) {
		all_wires_pids.remove(pid);
		if (all_wires_pids.size() == 0) {
			persistWires();
			shouldPersist = false;
		}
	}

	@Override
	public void componentUnregistered(String pid) {
		// Do nothing
	}

	@Override
	public String[] getFilter() {
		return configurationCallbackFilter;
	}

	@Override
	public ComponentConfiguration getConfiguration() throws KuraException {

		Tocd WiresOCD = new Tocd();
		WiresOCD.setId("WireService");
		WiresOCD.setName("Wire Service");
		WiresOCD.setDescription("Create a new Wire");
		Tad jsonAD = new Tad();
		jsonAD.setId("wires");
		jsonAD.setName("wires");
		jsonAD.setType(Tscalar.STRING);
		jsonAD.setCardinality(1);
		jsonAD.setRequired(true);
		jsonAD.setDefault("[]");
		jsonAD.setDescription("JSON description of a wire. The format has a producer PID, a consumer PID and, optionally, a set of properties for the wire.");
		// WiresOCD.addAD(jsonAD);

		// Create an option element for each producer factory
		ArrayList<String> emittersOptions = new ArrayList<String>();
		Set<String> factoryPids = m_configService.getComponentFactoryPids();

		for (String factoryPid : factoryPids) {
			emittersOptions.addAll(searchForInterface(factoryPid, WireEmitter.class));
		}

		Tad emitterTad = new Tad();
		emitterTad.setId("emitter.pids");
		emitterTad.setName("emitter.pids");
		emitterTad.setType(Tscalar.STRING);
		emitterTad.setCardinality(0);
		emitterTad.setRequired(false);
		Toption defaultOpt = new Toption();
		defaultOpt.setLabel("No new multiton");
		defaultOpt.setValue("NONE");
		emitterTad.getOption().add(defaultOpt);
		StringBuilder sb = new StringBuilder();
		for (String emitterOption : emittersOptions) {
			Toption opt = new Toption();
			opt.setLabel(emitterOption);
			opt.setValue(emitterOption);
			emitterTad.getOption().add(opt);
			sb.append(" ,");
		}
		emitterTad.setDefault(sb.toString());
		emitterTad.setDescription("Choose a WireEmitter");
		WiresOCD.addAD(emitterTad);

		// Create an option element for each producer factory
		ArrayList<String> consumersOptions = new ArrayList<String>();

		for (String factoryPid : factoryPids) {
			consumersOptions.addAll(searchForInterface(factoryPid, WireReceiver.class));
		}

		Tad consumerTad = new Tad();
		consumerTad.setId("consumer.pids");
		consumerTad.setName("consumer.pids");
		consumerTad.setType(Tscalar.STRING);
		consumerTad.setCardinality(0);
		consumerTad.setRequired(false);
		consumerTad.getOption().add(defaultOpt);
		sb = new StringBuilder();
		for (String consumerOption : consumersOptions) {
			Toption opt = new Toption();
			opt.setLabel(consumerOption);
			opt.setValue(consumerOption);
			consumerTad.getOption().add(opt);
			sb.append(" ,");
		}
		consumerTad.setDefault(sb.toString());
		sb = new StringBuilder("Choose a WireConsumer<br /><br /><b>Active wires:</b><br />");

		sb.append("<table style=\"width:100%; border: 1px solid black;\">");

		sb.append("<tr><td><b>Emitter</b></td><td><b>Consumer</b></td></tr>");

		for (WireConfiguration wc : m_wireConfig) {
			sb.append("<tr><td>").append(wc.getProducerPid()).append("</td><td>").append(wc.getConsumerPid()).append("</td></tr>");
		}

		sb.append("</table>");
		consumerTad.setDescription(sb.toString());
		WiresOCD.addAD(consumerTad);

		Tad wiresTad = new Tad();
		wiresTad.setName("delete.wires");
		wiresTad.setId("delete.wires");
		wiresTad.setType(Tscalar.STRING);
		wiresTad.setCardinality(0);
		wiresTad.setRequired(true);
		wiresTad.setDefault("Do not delete any wire");
		for (int i = 0; i < m_wireConfig.size(); i++) {
			WireConfiguration wc = m_wireConfig.get(i);
			Toption o = new Toption();
			o.setLabel("P:" + wc.getProducerPid() + " - C:" + wc.getConsumerPid());
			o.setValue(String.valueOf(i));
			wiresTad.getOption().add(o);
		}
		Toption o = new Toption();
		o.setLabel("Do not delete any wire");
		o.setValue("NONE");
		wiresTad.getOption().add(o);
		wiresTad.setDescription("Select a Wire from the list. It will be deleted when submitting the changes.");
		WiresOCD.addAD(wiresTad);

		Tad servicesTad = new Tad();
		servicesTad.setName("delete.instances");
		servicesTad.setId("delete.instances");
		servicesTad.setType(Tscalar.STRING);
		servicesTad.setCardinality(0);
		servicesTad.setRequired(true);
		servicesTad.setDefault("Do not delete any instance");
		Toption opt = new Toption();
		opt.setLabel("Do not delete any instance");
		opt.setValue("NONE");
		servicesTad.getOption().add(opt);

		for(String s : getEmittersAndConsumers()){
			o = new Toption();
			o.setLabel(s);
			o.setValue(s);
			servicesTad.getOption().add(o);
		}
		
		servicesTad.setDescription("Select an Instance from the list. The instance and all connected Wires will be deledet when submitting the changes.");
		WiresOCD.addAD(servicesTad);

		try {
			m_properties = new HashMap<String, Object>();
			m_properties.put("wires", m_options.toJsonString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ComponentConfigurationImpl cc = new ComponentConfigurationImpl(PROP_PID, WiresOCD, m_properties);
		return cc;
	}

	private List<String> searchForInterface(String factoryPid, Class iface) {
		ArrayList<String> result = new ArrayList<String>();
		// Iterate through the bundles
		for (Bundle b : m_ctx.getBundleContext().getBundles()) {
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
			Collection<ServiceReference<?>> services = m_ctx.getBundleContext().getServiceReferences(iface, null);
			for (ServiceReference<?> service : services) {
				result.add("INSTANCE|" + service.getProperty("service.pid"));
			}
		} catch (InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	private List<String> getEmittersAndConsumers(){
		ArrayList<String> result = new ArrayList<String>();
		
		try {
			Collection<ServiceReference<WireEmitter>> emitters = m_ctx.getBundleContext().getServiceReferences(WireEmitter.class, null);
			for (ServiceReference<WireEmitter> service : emitters) {
				result.add(service.getProperty("service.pid").toString());
			}
			Collection<ServiceReference<WireReceiver>> consumers = m_ctx.getBundleContext().getServiceReferences(WireReceiver.class, null);
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
