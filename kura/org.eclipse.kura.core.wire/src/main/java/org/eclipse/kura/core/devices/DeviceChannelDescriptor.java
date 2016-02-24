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
import java.util.List;

import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;


public abstract class DeviceChannelDescriptor {
	
	private List<Tad> defaultElements;
	
	public DeviceChannelDescriptor(){
		defaultElements = new ArrayList<Tad>();
		Tad name = new Tad();
		name.setId("name");
		name.setName("name");
		name.setType(Tscalar.STRING);
		name.setDefault("field name");
		name.setDescription("Name of the point");
		name.setCardinality(0);
		name.setRequired(true);
		defaultElements.add(name);
		Tad type = new Tad();
		type.setName("type");
		type.setId("type");
		type.setDescription("Primitive type of the point");
		type.setType(Tscalar.STRING);
		type.setRequired(true);
		type.setDefault("String");
		Toption OBoolean = new Toption();
		OBoolean.setValue("Boolean");
		OBoolean.setLabel("Boolean");
		type.getOption().add(OBoolean);
		Toption OByte = new Toption();
		OByte.setValue("Byte");
		OByte.setLabel("Byte");
		type.getOption().add(OByte);
		Toption ODouble = new Toption();
		ODouble.setValue("Double");
		ODouble.setLabel("Double");
		type.getOption().add(ODouble);
		Toption OInteger = new Toption();
		OInteger.setValue("Integer");
		OInteger.setLabel("Integer");
		type.getOption().add(OInteger);
		Toption OLong = new Toption();
		OLong.setValue("Long");
		OLong.setLabel("Long");
		type.getOption().add(OLong);
		Toption ORaw = new Toption();
		ORaw.setValue("Raw");
		ORaw.setLabel("Raw bytes");
		type.getOption().add(ORaw);
		Toption OShort = new Toption();
		OShort.setValue("Short");
		OShort.setLabel("Short");
		type.getOption().add(OShort);
		Toption OString = new Toption();
		OString.setValue("String");
		OString.setLabel("String");
		type.getOption().add(OString);
		defaultElements.add(type);

	}
	
	public List<Tad> getInputConfiguration(){
		return defaultElements;
	}
	
	public List<Tad> getOutputConfiguration(){
		return defaultElements;
	}
	
}
