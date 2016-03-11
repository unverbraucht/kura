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
package org.eclipse.kura.core.devices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.core.wire.timer.Timer;
import org.eclipse.kura.wires.WireComponent;
import org.eclipse.kura.wires.WireEmitter;
import org.eclipse.kura.wires.WireEnvelope;
import org.eclipse.kura.wires.WireField;
import org.eclipse.kura.wires.WireReceiver;
import org.eclipse.kura.wires.WireRecord;
import org.eclipse.kura.wires.WireSupport;
import org.eclipse.kura.wires.WireValueBoolean;
import org.eclipse.kura.wires.WireValueByte;
import org.eclipse.kura.wires.WireValueDouble;
import org.eclipse.kura.wires.WireValueInteger;
import org.eclipse.kura.wires.WireValueLong;
import org.eclipse.kura.wires.WireValueRaw;
import org.eclipse.kura.wires.WireValueShort;
import org.eclipse.kura.wires.WireValueString;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WireDevice implements WireComponent, WireEmitter, WireReceiver, SelfConfiguringComponent {

	private static final Logger s_logger = LoggerFactory.getLogger(WireDevice.class);

	protected DeviceDriver m_driver = null;
	private DeviceDriverTracker m_deviceTracker;
	
	protected DeviceChannelDescriptor m_device_descriptor = null;

	protected List<DataOutputRow> m_outputs;
	protected List<DataInputRow> m_inputs;

	private Map<String, Object> m_properties;

	private ComponentContext m_ctx;
	protected WireSupport m_wireSupport;

	public WireDevice() {
		m_wireSupport = new WireSupport(this);
		m_inputs = new ArrayList<DataInputRow>();
		m_outputs = new ArrayList<DataOutputRow>();
	}

	// ----------------------------------------------------------------
	//
	// Dependencies
	//
	// ----------------------------------------------------------------

	public void setDeviceDriver(String driverInstanceName) {
		try {
			m_deviceTracker = new DeviceDriverTracker(m_ctx.getBundleContext(), this, driverInstanceName);
			m_deviceTracker.open();
		} catch (InvalidSyntaxException e) {
			// InvalidSyntax will never be thrown
		}
	}

	protected void setDeviceDriverInstance(DeviceDriver d){
		m_driver = d;
	}
	
	public void setDeviceChannelDescriptor(DeviceChannelDescriptor descriptor) {
		m_device_descriptor = descriptor;
	}

	protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
		s_logger.debug("Activating wire device...");
		m_ctx = componentContext;
		m_properties = properties;

		parseProperties(properties);
		
		afterActivate(componentContext, properties);
	}

	protected void deactivate(ComponentContext componentContext) {
		s_logger.debug("Deactivating wire device...");
		beforeDeactivate(componentContext);

	}

	protected void updated(Map<String, Object> properties) {
		s_logger.debug("Updating wire device...");
		afterUpdated(properties);
	}

	// ----------------------------------------------------------------
	//
	// Wire Producer
	//
	// ----------------------------------------------------------------

	@Override
	public Object polled(Wire wire) {
		return m_wireSupport.polled(wire);
	}

	@Override
	public void consumersConnected(Wire[] wires) {
		m_wireSupport.consumersConnected(wires);
	}

	// ----------------------------------------------------------------
	//
	// Wire Consumer
	//
	// ----------------------------------------------------------------

	@Override
	public void updated(Wire wire, Object value) {
		m_wireSupport.updated(wire, value);
	}

	@Override
	public void producersConnected(Wire[] wires) {
		m_wireSupport.producersConnected(wires);
	}

	// ----------------------------------------------------------------
	//
	// WireReceiver
	//
	// ----------------------------------------------------------------

	@Override
	public void onWireReceive(WireEnvelope wireEvelope) {
		if (wireEvelope.getRecords().get(0).getFields().get(0).getName().equals(Timer.TIMER_EVENT_FIELD_NAME)) {
			poll();
		}
		for (WireRecord wr : wireEvelope.getRecords()) {
			for (WireField wf : wr.getFields()) {
				for (DataOutputRow row : m_outputs) {
					if (row.getDataPoint().getName().equals(wf.getName())) {
						// This will trigger the internal DataPoint listeners
						// which will propagate down to the driver
						row.getDataPoint().updateWireField(wf);
					}
				}
			}
		}
	}

	// ----------------------------------------------------------------
	//
	// WireEmitter
	//
	// ----------------------------------------------------------------

	@Override
	public String getEmitterPid() {
		return getWireDeviceName();
	}

	// ----------------------------------------------------------------
	//
	// SelfConfiguringComponent
	//
	// ----------------------------------------------------------------

	@Override
	public ComponentConfiguration getConfiguration() throws KuraException {
		String componentName = m_ctx.getProperties().get("service.pid").toString();

		Tocd mainOcd = new Tocd();
		mainOcd.setName(getWireDeviceName());
		mainOcd.setDescription(getWireDeviceDescription());
		mainOcd.setId(getWireDeviceName());

		Tad mainAd = new Tad();
		mainAd.setId("device.name");
		mainAd.setName("device.name");
		mainAd.setCardinality(0);
		mainAd.setType(Tscalar.STRING);
		mainAd.setDescription("Device name");
		mainAd.setRequired(true);

		mainOcd.addAD(mainAd);
		Map<String, Object> props = new HashMap<String, Object>();
		
		for (String key : m_properties.keySet()) {
			props.put(key, m_properties.get(key));
		}

		if (m_device_descriptor != null) {

			List<Tad> inputConfiguration = m_device_descriptor.getInputConfiguration();
			for (DataInputRow inputRow : m_inputs) {
				for (Tad attribute : inputConfiguration) {
					Tad newAttribute = cloneAd(attribute, "i" + String.valueOf(inputRow.getIndex()) + ".");
					mainOcd.addAD(newAttribute);
				}
				Map<String, Object> rowProperties = inputRow.getProperties();
				for (String key : rowProperties.keySet()) {
					props.put(key, rowProperties.get(key));
				}
			}

			List<Tad> outputConfiguration = m_device_descriptor.getOutputConfiguration();
			for (DataOutputRow outputRow : m_outputs) {
				for (Tad attribute : outputConfiguration) {
					Tad newAttribute = cloneAd(attribute, "o" + String.valueOf(outputRow.getIndex()) + ".");
					mainOcd.addAD(newAttribute);
				}
				Map<String, Object> rowProperties = outputRow.getProperties();
				for (String key : rowProperties.keySet()) {
					props.put(key, rowProperties.get(key));
				}
			}
		}

		ComponentConfigurationImpl cc = new ComponentConfigurationImpl(componentName, mainOcd, props);
		return cc;
	}

	// ----------------------------------------------------------------
	//
	// Device APIs
	//
	// ----------------------------------------------------------------

	public abstract String getWireDeviceName();

	public abstract String getWireDeviceDescription();

	protected abstract void addInputInternal(DataPoint value, Map<String, Object> properties) throws KuraException;

	protected abstract void addOutputInternal(DataPoint value, Map<String, Object> properties) throws KuraException;

	public abstract void poll();

	public abstract void emit();

	protected abstract void afterActivate(ComponentContext ctx, Map<String, Object> properties);

	protected abstract void afterUpdated(Map<String, Object> properties);

	protected abstract void beforeDeactivate(ComponentContext ctx);

	// ----------------------------------------------------------------
	//
	// private methods
	//
	// ----------------------------------------------------------------

	private DataPoint getDataPointFromProperties(String name, String type) {
		if (type.toUpperCase().equals("INTEGER")) {
			return new DataPoint(new WireField(name, new WireValueInteger(0)));
		}
		if (type.toUpperCase().equals("BOOLEAN")) {
			return new DataPoint(new WireField(name, new WireValueBoolean(false)));
		}
		if (type.toUpperCase().equals("BYTE")) {
			return new DataPoint(new WireField(name, new WireValueByte(Byte.MIN_VALUE)));
		}
		if (type.toUpperCase().equals("DOUBLE")) {
			return new DataPoint(new WireField(name, new WireValueDouble(0d)));
		}
		if (type.toUpperCase().equals("LONG")) {
			return new DataPoint(new WireField(name, new WireValueLong(0l)));
		}
		if (type.toUpperCase().equals("SHORT")) {
			return new DataPoint(new WireField(name, new WireValueShort(Short.MIN_VALUE)));
		}
		if (type.toUpperCase().equals("STRING")) {
			return new DataPoint(new WireField(name, new WireValueString("")));
		}
		return new DataPoint(new WireField(name, new WireValueRaw(new byte[] {})));
	}

	private void parseProperties(Map<String, Object> properties) {
		// Parse inputs
		HashSet<Integer> parsedIndexes = new HashSet<Integer>();
		for (String s : properties.keySet()) {
			try {
				// Parse inputs. Assume a prefix on the form (i\d{1,}\.).* ('i' followed by any number and then '.')
				if (s.matches("(i\\d{1,}\\.).*")) {
					String prefix = s.substring(0, s.indexOf(".")+1);
					int index = Integer.parseInt(prefix.substring(1, prefix.length() - 1));
					// Process if index has not already been parsed
					if (!parsedIndexes.contains(index)) {
						parsedIndexes.add(index);
						Map<String, Object> passedProps = new HashMap<String, Object>();
						for (String innerKey : properties.keySet()) {
							if (innerKey.startsWith(prefix)) {
								passedProps.put(innerKey.replace(prefix, ""), properties.get(innerKey));
							}
						}
						String name = (String) properties.get(prefix + "name");
						String type = (String) properties.get(prefix + "type");
						DataPoint dp = getDataPointFromProperties(name, type);
						addInput(dp, passedProps);
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		// Parse outputs
		parsedIndexes = new HashSet<Integer>();
		for (String s : properties.keySet()) {
			try {
				// Parse outputs. Assume a prefix on the form (o\d{1,}\.).* ('o' followed by any number and then '.')
				if (s.matches("(o\\d{1,}\\.).*")) {
					String prefix = s.substring(0, s.indexOf("."));
					int index = Integer.parseInt(prefix.substring(1, prefix.length() - 1));
					// Process if index has not already been parsed
					if (!parsedIndexes.contains(index)) {
						parsedIndexes.add(index);
						Map<String, Object> passedProps = new HashMap<String, Object>();
						for (String innerKey : properties.keySet()) {
							if (innerKey.startsWith(prefix)) {
								passedProps.put(innerKey.replace(prefix, ""), properties.get(innerKey));
							}
						}
						String name = (String) properties.get(prefix + "name");
						String type = (String) properties.get(prefix + "type");
						DataPoint dp = getDataPointFromProperties(name, type);
						addOutput(dp, passedProps);
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

	}

	private int getNewIndex(List<? extends DataRow> list) {
		int result = 0;
		for (DataRow dr : list) {
			result = Math.max(result, dr.getIndex());
		}
		return result + 1;
	}

	public void addInput(DataPoint value, Map<String, Object> properties) throws KuraException {

		m_inputs.add(new DataInputRow(getNewIndex(m_inputs), value, properties));

		addInputInternal(value, properties);
	}

	public void addOutput(DataPoint value, Map<String, Object> properties) throws KuraException {

		m_outputs.add(new DataOutputRow(getNewIndex(m_outputs), value, properties));

		addOutputInternal(value, properties);
	}

	private Tad cloneAd(Tad oldAd, String prefix) {
		Tad result = new Tad();

		result.setId(prefix + oldAd.getId());
		result.setName(prefix + oldAd.getName());
		result.setCardinality(oldAd.getCardinality());
		result.setType(Tscalar.fromValue(oldAd.getType().value()));
		result.setDescription(oldAd.getDescription());
		result.setDefault(oldAd.getDefault());
		result.setMax(oldAd.getMax());
		result.setMin(oldAd.getMin());
		result.setRequired(oldAd.isRequired());
		for (Option o : oldAd.getOption()) {
			Toption opt = new Toption();
			opt.setLabel(o.getLabel());
			opt.setValue(o.getValue());
			result.getOption().add(opt);
		}

		return result;
	}
}
