package org.eclipse.kura.core.wire;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationCallback;
import org.eclipse.kura.configuration.ConfigurationService;
import org.json.JSONException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.wireadmin.Wire;
import org.osgi.service.wireadmin.WireAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WireService implements ConfigurableComponent, ConfigurationCallback {
	private static final Logger s_logger = LoggerFactory.getLogger(WireService.class);

	private static final String PROP_PRODUCER_PID = "wireadmin.producer.pid";
	private static final String PROP_CONSUMER_PID = "wireadmin.consumer.pid";

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

//		initDebugData();

	}

	private void initDebugData() {
				
		try {
			all_wires_pids = new HashSet<String>();
			all_wires_pids.add("org.eclipse.kura.core.wire.WireService");

			m_cloudPubPid = createComponentAndWire("org.eclipse.kura.core.wire.cloud.publisher.CloudPublisher");

			String dbStorePubPid = createComponentAndWire("org.eclipse.kura.core.wire.store.DbWireRecordStore");
			
			String dbRecordFilterPubPid = createComponentAndWire("org.eclipse.kura.core.wire.store.DbWireRecordFilter");

			String exampleDevicePubPid = createComponentAndWire("org.eclipse.kura.example.wire.device.DeviceExample");
			
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
	
	private Wire createNewWire(String producerPid, String consumerPid){
		Wire wire1 = m_wireAdmin.createWire(producerPid, consumerPid, null);
		s_logger.info("Created Wire between {} and {}.", producerPid, consumerPid);
		s_logger.info("Wire connected status: {}", wire1.isConnected());
		return wire1;
	}
	
	private String createComponentAndWire(String factoryPid){
		String newPid = null;
		try {
			ComponentConfiguration compConfig;
			compConfig = m_configService.getComponentDefaultConfiguration(factoryPid);
			newPid = m_configService.createComponent(factoryPid, compConfig.getConfigurationProperties());
			all_wires_pids.add(newPid);
			s_logger.info("Created {} instance with pid {}", factoryPid , newPid);			
		}catch(Exception ex){
			
		}
		return newPid;
	}


	public void updated(Map<String, Object> properties) {
		s_logger.info("updated...: " + properties);
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
		configurationCallbackFilter = all_wires_pids.toArray(new String[]{});
		
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

}
