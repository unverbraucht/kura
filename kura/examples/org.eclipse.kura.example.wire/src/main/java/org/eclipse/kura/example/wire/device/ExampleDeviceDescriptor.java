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
import java.util.List;

import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.core.devices.DeviceChannelDescriptor;

public class ExampleDeviceDescriptor extends DeviceChannelDescriptor {

	private List<Tad> definition;
	
	public ExampleDeviceDescriptor() {
		super();
		definition = new ArrayList<Tad>();
		
		Tad indirizzo = new Tad();
		indirizzo.setId("address");
		indirizzo.setName("address");
		indirizzo.setCardinality(0);
		indirizzo.setType(Tscalar.INTEGER);
		indirizzo.setDefault("1");
		indirizzo.setDescription("Point address");
		indirizzo.setRequired(true);
		definition.add(indirizzo);
		super.getInputConfiguration().addAll(definition);
	}
	
}
