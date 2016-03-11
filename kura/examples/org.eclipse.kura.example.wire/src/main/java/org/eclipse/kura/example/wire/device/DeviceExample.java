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
package org.eclipse.kura.example.wire.device;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.devices.DataPoint;
import org.eclipse.kura.core.devices.WireDevice;
import org.eclipse.kura.wires.WireField;
import org.eclipse.kura.wires.WireRecord;
import org.eclipse.kura.wires.WireValueBoolean;
import org.eclipse.kura.wires.WireValueInteger;
import org.osgi.service.component.ComponentContext;

public class DeviceExample extends WireDevice {

	private List<ExampleInput> m_inputs;

	public DeviceExample() {
		m_inputs = new ArrayList<ExampleInput>();
	}

	@Override
	public String getWireDeviceName() {
		return "Example device";
	}

	@Override
	public String getWireDeviceDescription() {
		return "Example wire device";
	}

	@Override
	protected void afterActivate(ComponentContext ctx, Map<String, Object> properties) {

		super.setDeviceChannelDescriptor(new ExampleDeviceDescriptor());
		super.setDeviceDriver("Driver di esempio");
		
		//addDebugData();
		
	}
	
	private void addDebugData(){
		HashMap<String, Object> pointProps = new HashMap<String, Object>();
		pointProps.put("address", 1020);
		try {
			addInput(new DataPoint(new WireField("Prova 1", new WireValueInteger(123))), pointProps);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		pointProps = new HashMap<String, Object>();
		pointProps.put("address", 2345);
		try {
			addInput(new DataPoint(new WireField("Prova 2", new WireValueBoolean(false))), pointProps);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
	}

	@Override
	protected void afterUpdated(Map<String, Object> properties) {

	}

	@Override
	protected void beforeDeactivate(ComponentContext ctx) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addInputInternal(DataPoint value, Map<String, Object> properties) throws KuraException {
		try {
			int address = (Integer) properties.get("address");
			if (value.getValue() instanceof WireValueInteger) {
					m_inputs.add(new ExampleIntegerInput(value, address));
			} else if (value.getValue() instanceof WireValueBoolean) {
					m_inputs.add(new ExampleBooleanInput(value, address));
			} else {
				throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID);
			}
		} catch (Exception ex) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_UNDEFINED);
		}
	}

	@Override
	public void addOutputInternal(DataPoint value, Map<String, Object> properties) throws KuraException {
		// TODO Auto-generated method stub

	}

	@Override
	public void poll() {
		for (ExampleInput i : m_inputs) {
			i.poll();
		}
		emit();
	}

	@Override
	public void emit() {
		if (m_inputs.size() > 0) {
			WireField[] fields = new WireField[m_inputs.size()];
			for (int i=0; i<m_inputs.size(); i++) {
				WireField wf = m_inputs.get(i).getWireField();
				fields[i]=wf;
			}
			WireRecord wr = new WireRecord(fields);
			m_wireSupport.emit(wr);
		}
	}

	abstract class ExampleEndpoint {
		protected DataPoint point;
		protected int address;

		public ExampleEndpoint(DataPoint point, int address) {
			this.point = point;
			this.address = address;
		}

		public WireField getWireField() {
			return new WireField(point.getName(), point.getValue());
		}
	}

	abstract class ExampleInput extends ExampleEndpoint {
		public ExampleInput(DataPoint point, int address) {
			super(point, address);
		}

		protected abstract void poll();
	}

	private class ExampleIntegerInput extends ExampleInput {
		public ExampleIntegerInput(DataPoint point, int address) {
			super(point, address);
		}

		@Override
		protected void poll() {
			int value = ((DriverExample) m_driver).readInteger(address);
			WireField wf = new WireField(point.getName(), new WireValueInteger(value));
			point.updateWireField(wf);
		}
	}

	private class ExampleBooleanInput extends ExampleInput {
		public ExampleBooleanInput(DataPoint point, int address) {
			super(point, address);
		}

		@Override
		protected void poll() {
			boolean value = ((DriverExample) m_driver).readBoolean(address);
			WireField wf = new WireField(point.getName(), new WireValueBoolean(value));
			point.updateWireField(wf);
		}
	}

}
