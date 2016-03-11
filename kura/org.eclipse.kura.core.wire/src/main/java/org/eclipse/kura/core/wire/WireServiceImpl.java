/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.core.wire;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.multitons.MultitonRegistrationCallback;
import org.eclipse.kura.multitons.MultitonService;
import org.eclipse.kura.wires.WireEmitter;
import org.eclipse.kura.wires.WireReceiver;
import org.eclipse.kura.wires.WireService;
import org.json.JSONException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.wireadmin.Wire;
import org.osgi.service.wireadmin.WireAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WireServiceImpl implements SelfConfiguringComponent, WireService {
	private static final Logger s_logger = LoggerFactory.getLogger(WireServiceImpl.class);

	private static final String PROP_PRODUCER_PID = "wireadmin.producer.pid";
	private static final String PROP_CONSUMER_PID = "wireadmin.consumer.pid";
	private static final String PROP_PID = "org.eclipse.kura.core.wire.WireServiceImpl";

	private ComponentContext m_ctx;

	private WireAdmin m_wireAdmin;
	private ConfigurationService m_configService;
	private MultitonService m_multitonService;

	private static WireServiceOptions m_options;

	private List<WireConfiguration> m_wireConfig;
	private Map<String, Object> m_properties;

	// private boolean shouldPersist = false;

	private WireSeviceTracker m_serviceTracker;

	// ----------------------------------------------------------------
	//
	// Dependencies
	//
	// ----------------------------------------------------------------

	public WireServiceImpl() {
		m_wireConfig = new ArrayList<WireConfiguration>();
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

	public void setMultitonService(MultitonService multitonService) {
		m_multitonService = multitonService;
	}

	public void unsetMultitonService(MultitonService multitonService) {
		m_multitonService = null;
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
			
			m_options = WireServiceOptions.newInstance(properties);
			m_properties = properties;
			
			for(WireConfiguration conf : m_options.getWireConfigurations()){
				m_wireConfig.add(conf);
			}

			m_serviceTracker = new WireSeviceTracker(m_ctx.getBundleContext(), this);
			m_serviceTracker.open();

			createWires();
			
			// debug();
			
		} catch (JSONException jsone) {
			throw new ComponentException(jsone);
			// } catch (KuraException e) {
			// e.printStackTrace();
		} catch (InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void debug(){

		try {
			m_multitonService.newMultitonInstance("org.eclipse.kura.example.wire.device.DriverExample", true, "Driver di esempio");
		} catch (KuraException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void initDebugData() {

		try {
			String m_cloudPubPid = createWireComponent("org.eclipse.kura.core.wire.cloud.publisher.CloudPublisher", "Cloud Publisher");

			String dbStorePubPid = createWireComponent("org.eclipse.kura.core.wire.store.DbWireRecordStore", "Record Store");

			String dbRecordFilterPubPid = createWireComponent("org.eclipse.kura.core.wire.store.DbWireRecordFilter", "Record Filter");

			String exampleDevicePubPid = createWireComponent("org.eclipse.kura.example.wire.device.DeviceExample", "Device Example");

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

		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

	}

	private Wire createNewWire(String emitterPid, String receiverPid) {
		Wire wire1 = m_wireAdmin.createWire(emitterPid, receiverPid, null);
		s_logger.info("Created Wire between {} and {}.", emitterPid, receiverPid);
		s_logger.info("Wire connected status: {}", wire1.isConnected());
		return wire1;
	}

	public void updated(Map<String, Object> properties) {
		s_logger.info("updated...: " + properties);

		try { // Avoid reiterate calls to update. TODO: Remove in final version

			for (String s : properties.keySet()) {
				System.out.println(s + " = " + properties.get(s).toString());
			}
			final Object emitterPid = properties.get("emitter.pids");
			final Object receiverPid = properties.get("receiver.pids");
			
			String emitterName = null;
			try{
				emitterName = (String)properties.get("emitter.name");
			}catch(Exception ex){}
			String receiverName = null;
			try{
				receiverName = (String)properties.get("receiver.name");
			}catch(Exception ex){}

			// NEW WIRE
			if (emitterPid != null && receiverPid != null) {
				if (!emitterPid.toString().equals("NONE") && !receiverPid.toString().equals("NONE")) {
					final String emitterString = createComponentFromProperty(emitterPid.toString(), emitterName);
					final String receiverString = createComponentFromProperty(receiverPid.toString(), receiverName);
					WireConfiguration wc = new WireConfiguration(emitterString, receiverString, null, false);
					m_wireConfig.add(wc);
					createWires();
					if (emitterPid.toString().startsWith("INSTANCE") && receiverPid.toString().startsWith("INSTANCE")) {
						m_configService.snapshot();
					}
				}
			}

			// DELETE EXISTING WIRE
			Object wiresDelete = properties.get("delete.wires");
			if (wiresDelete != null) {
				if (!wiresDelete.toString().equals("NONE")) {

					int index = Integer.valueOf(wiresDelete.toString());
					WireConfiguration wc = m_wireConfig.get(index);
					removeWire(wc.getEmitterPid(), wc.getReceiverPid());
				}
			}

			// DELETE EMITTER/RECEIVER INSTANCE
			Object instancesDelete = properties.get("delete.instances");
			if (instancesDelete != null) {
				if (!instancesDelete.toString().equals("NONE")) {
					removeWireComponent(instancesDelete.toString());
				}
			}
		} catch (Exception ex) {
			s_logger.error("Error during WireServiceImpl update! Something went wrong...", ex);
		}
	}

	private String createComponentFromProperty(String value, String name) {
		String[] tokens = value.split("\\|");
		if (tokens[0].equals("FACTORY")) {
			return createWireComponent(tokens[1], name);
		} else {
			return tokens[1];
		}
	}

	protected void deactivate(ComponentContext componentContext) {
		s_logger.info("deactivate...");

	}

	// ----------------------------------------------------------------
	//
	// Kura Wire APIs
	//
	// ----------------------------------------------------------------

	@Override
	public String createWireComponent(final String factoryPid, String name) {
		String newPid = null;
		try {
			newPid = m_configService.newConfigurableComponent(factoryPid, null, false, name);
		} catch (Exception ex) {

		}
		return newPid;
	}

	@Override
	public boolean removeWireComponent(String pid) {
		// Search for wires using the pid we are going to delete
		removePidRelatedWires(pid);

		// Then delete the instance
		try {
			m_configService.deleteConfigurableComponent(pid);
		} catch (KuraException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		persistWires(true);
		return true;
	}

	@Override
	public void createWire(String emitterPid, String receiverPid) {
		m_wireAdmin.createWire(emitterPid, receiverPid, null);
		WireConfiguration conf = new WireConfiguration(emitterPid, receiverPid, null);
		m_wireConfig.add(conf);
	}

	@Override
	public boolean removeWire(String emitterPid, String receiverPid) {
		try {
			WireConfiguration theWire = null;
			for (WireConfiguration conf : m_wireConfig) {
				if (conf.getEmitterPid().equals(emitterPid) && conf.getReceiverPid().equals(receiverPid)) {
					theWire = conf;
					break;
				}
			}
			if (theWire != null) {
				Wire[] list = m_wireAdmin.getWires(null);
				for (Wire w : list) {
					String prod = w.getProperties().get(PROP_PRODUCER_PID).toString();
					String cons = w.getProperties().get(PROP_CONSUMER_PID).toString();
					if (prod.equals(theWire.getEmitterPid()) && cons.equals(theWire.getReceiverPid())) {
						m_wireAdmin.deleteWire(w);
						m_wireConfig.remove(theWire);
						persistWires(true);
						return true;
					}
				}
			}
		} catch (InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	// ----------------------------------------------------------------
	//
	// Private methods
	//
	// ----------------------------------------------------------------

	private boolean removePidRelatedWires(String pid) {
		boolean atLeatOneRemoved = false;
		WireConfiguration[] copy = m_wireConfig.toArray(new WireConfiguration[] {});
		for (WireConfiguration wc : copy) {
			if (wc.getEmitterPid().equals(pid) || wc.getReceiverPid().equals(pid)) {
				// if found, delete the wire
				removeWire(wc.getEmitterPid(), wc.getReceiverPid());
				atLeatOneRemoved = true;
			}
		}
		return atLeatOneRemoved;
	}

	private boolean wireAlreadyCreated(String emitter, String receiver) {
		for (WireConfiguration wc : m_wireConfig) {
			if (wc.getEmitterPid().equals(emitter) && wc.getReceiverPid().equals(receiver) && wc.isCreated()) {
				return true;
			}
		}
		return false;
	}
	
	private WireConfiguration getWireConfiguration(String emitter, String receiver){
		for (WireConfiguration wc : m_wireConfig) {
			if (wc.getEmitterPid().equals(emitter) && wc.getReceiverPid().equals(receiver)) {
				return wc;
			}
		}
		WireConfiguration wc = new WireConfiguration(emitter, receiver, null);
		m_wireConfig.add(wc);
		return wc;		
	}
	
	private void updatePidNamesInList(String oldEmitter, String oldReceiver){
		String newEmitter = m_configService.getCurrentComponentPid(oldEmitter);
		String newReceiver = m_configService.getCurrentComponentPid(oldReceiver);
		
		if(newEmitter != oldEmitter || newReceiver != oldReceiver){
			synchronized (m_wireConfig) {
				for(WireConfiguration wc : m_wireConfig){
					if(wc.getEmitterPid() == oldEmitter || wc.getReceiverPid() == oldReceiver){
						wc.update(newEmitter, newReceiver);
					}
				}
			}
		}
	}

	protected synchronized void createWires() {

		// TODO: remove existing wires?

		ArrayList<WireConfiguration> cloned = new ArrayList<WireConfiguration>();
		for(WireConfiguration wc : m_wireConfig){
			cloned.add(new WireConfiguration(wc.getEmitterPid(), wc.getReceiverPid(), wc.getFilter(), wc.isCreated()));
		}

		for (WireConfiguration conf : cloned) {

			updatePidNamesInList(conf.getEmitterPid(), conf.getReceiverPid());
			String emitter = m_configService.getCurrentComponentPid(conf.getEmitterPid());
			String receiver = m_configService.getCurrentComponentPid(conf.getReceiverPid());
			boolean emitterFound = false;
			boolean receiverFound = false;

			for (String s : m_serviceTracker.getWireEmitters()) {
				if (s.equals(emitter)) {
					emitterFound = true;
					break;
				}
			}

			for (String s : m_serviceTracker.getWireReceivers()) {
				if (s.equals(receiver)) {
					receiverFound = true;
					break;
				}
			}

			if (emitterFound && receiverFound) {
				if (!wireAlreadyCreated(emitter, receiver)) {
					s_logger.info("Creating wire between {} and {}", emitter, receiver);
					m_wireAdmin.createWire(emitter, receiver, null);
					WireConfiguration wc = getWireConfiguration(emitter, receiver);
					wc.setCreated(true);
					persistWires(false);
				}
			}
		}
	}
	
	private void persistWires(boolean shouldPersist) {
		List<WireConfiguration> list = m_options.getWires();
		list.clear();
		for (WireConfiguration w : m_wireConfig) {
			list.add(w);
		}
		HashMap<String, Object> new_props = new HashMap<String, Object>(m_properties);
		try {
			String jsonString = m_options.toJsonString();
			new_props.put(WireServiceOptions.CONF_WIRES, jsonString);
			m_configService.updateConfiguration("org.eclipse.kura.core.wire.WireServiceImpl", new_props, shouldPersist);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KuraException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public ComponentConfiguration getConfiguration() throws KuraException {

		Tocd WiresOCD = new Tocd();
		WiresOCD.setId("WireServiceImpl");
		WiresOCD.setName("Wire Service");
		WiresOCD.setDescription("Create a new Wire");

		// Icon loading doesn't work for SelfConfiguringComponents

		// Ticon icon = new Ticon();
		// icon.setResource("OSGI-INF/chart.png");
		// icon.setSize(new BigInteger("32"));
		// WiresOCD.setIcon(icon);

		// Following code is for creating a AD for the json configuration.
		// We don't need to show it on the WebUI.

		// Tad jsonAD = new Tad();
		// jsonAD.setId("wires");
		// jsonAD.setName("wires");
		// jsonAD.setType(Tscalar.STRING);
		// jsonAD.setCardinality(1);
		// jsonAD.setRequired(true);
		// jsonAD.setDefault("[]");
		// jsonAD.setDescription("JSON description of a wire. The format has a producer PID, a consumer PID and, optionally, a set of properties for the wire.");
		// WiresOCD.addAD(jsonAD);

		// Create an option element for each producer factory

		ArrayList<String> emittersOptions = new ArrayList<String>();
		Set<String> factoryPids = m_configService.getComponentFactoryPids();

		for (String factoryPid : factoryPids) {
			emittersOptions.addAll(WireUtils.getFactoriesAndInstances(m_ctx, factoryPid, WireEmitter.class));
		}

		Tad emitterTad = new Tad();
		emitterTad.setId("emitter.pids");
		emitterTad.setName("emitter.pids");
		emitterTad.setType(Tscalar.STRING);
		emitterTad.setCardinality(0);
		emitterTad.setRequired(true);
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

		Tad emitterName= new Tad();
		emitterName.setId("emitter.name");
		emitterName.setName("emitter.name");
		emitterName.setType(Tscalar.STRING);
		emitterName.setCardinality(0);
		emitterName.setRequired(false);
		emitterName.setDefault("");
		emitterName.setDescription("multiton.instance.name for the resulting component. If left null it will equal service.pid");
		WiresOCD.addAD(emitterName);
		
		// Create an option element for each producer factory
		ArrayList<String> receiversOptions = new ArrayList<String>();

		for (String factoryPid : factoryPids) {
			receiversOptions.addAll(WireUtils.getFactoriesAndInstances(m_ctx, factoryPid, WireReceiver.class));
		}

		Tad receiverTad = new Tad();
		receiverTad.setId("receiver.pids");
		receiverTad.setName("receiver.pids");
		receiverTad.setType(Tscalar.STRING);
		receiverTad.setCardinality(0);
		receiverTad.setRequired(true);
		receiverTad.getOption().add(defaultOpt);
		sb = new StringBuilder();
		for (String receiverOption : receiversOptions) {
			Toption opt = new Toption();
			opt.setLabel(receiverOption);
			opt.setValue(receiverOption);
			receiverTad.getOption().add(opt);
			sb.append(" ,");
		}
		receiverTad.setDefault(sb.toString());
		receiverTad.setDescription("Choose a WireReceiver");
		WiresOCD.addAD(receiverTad);

		Tad receiverName= new Tad();
		receiverName.setId("receiver.name");
		receiverName.setName("receiver.name");
		receiverName.setType(Tscalar.STRING);
		receiverName.setCardinality(0);
		receiverName.setRequired(false);
		receiverName.setDefault("");
		// Build the description String of the receiver.pids element so to also
		// show a table of all
		// the active wires.
		sb = new StringBuilder("multiton.instance.name for the resulting component. If left null it will equal service.pid<br /><br /><b>Active wires:</b><br />");

		sb.append("<table style=\"width:100%; border: 1px solid black;\">");

		sb.append("<tr><td><b>Emitter</b></td><td><b>Receiver</b></td></tr>");

		for (WireConfiguration wc : m_wireConfig) {
			sb.append("<tr><td>").append(wc.getEmitterPid()).append("</td><td>").append(wc.getReceiverPid()).append("</td></tr>");
		}

		sb.append("</table>");
		receiverName.setDescription(sb.toString());
		WiresOCD.addAD(receiverName);

		Tad wiresTad = new Tad();
		wiresTad.setName("delete.wires");
		wiresTad.setId("delete.wires");
		wiresTad.setType(Tscalar.STRING);
		wiresTad.setCardinality(0);
		wiresTad.setRequired(true);
		wiresTad.setDefault("NONE");
		for (int i = 0; i < m_wireConfig.size(); i++) {
			WireConfiguration wc = m_wireConfig.get(i);
			Toption o = new Toption();
			o.setLabel("P:" + wc.getEmitterPid() + " - C:" + wc.getReceiverPid());
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
		servicesTad.setDefault("NONE");
		Toption opt = new Toption();
		opt.setLabel("Do not delete any instance");
		opt.setValue("NONE");
		servicesTad.getOption().add(opt);

		for (String s : WireUtils.getEmittersAndReceivers(m_ctx)) {
			o = new Toption();
			o.setLabel(s);
			o.setValue(s);
			servicesTad.getOption().add(o);
		}

		servicesTad.setDescription("Select an Instance from the list. The instance and all connected Wires will be deledet when submitting the changes.");
		WiresOCD.addAD(servicesTad);

		try {
			m_properties = new HashMap<String, Object>();
			// Put the json configuration into the properties so to persist them
			// in the snapshot
			m_properties.put("wires", m_options.toJsonString());
			m_properties.put("delete.instances", "NONE");
			m_properties.put("delete.wires", "NONE");
			m_properties.put("receiver.pids", "NONE");
			m_properties.put("emitter.pids", "NONE");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ComponentConfigurationImpl cc = new ComponentConfigurationImpl(PROP_PID, WiresOCD, m_properties);
		return cc;
	}

}
