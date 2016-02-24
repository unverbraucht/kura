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
package org.eclipse.kura.example.wire.self;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.wires.WireComponent;
import org.eclipse.kura.wires.WireEmitter;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelfConfiguringWire implements SelfConfiguringComponent, WireComponent, WireEmitter{
	private static final Logger s_logger = LoggerFactory.getLogger(SelfConfiguringWire.class);
	
	private Map<String, Object> m_properties;
	private ComponentContext m_ctx;
	
	protected void activate(ComponentContext ctx, Map<String, Object> properties) {
		s_logger.info("Activating self configuring wire...");
		m_ctx = ctx;
	}

	protected void deactivate(ComponentContext ctx) {
		s_logger.info("Deactivating self configuring wire...");
	}

	protected void updated(Map<String, Object> properties) {
		s_logger.info("Updating self configuring wire...");
	}

	@Override
	public ComponentConfiguration getConfiguration() throws KuraException {
		Object asd = m_ctx.getProperties().get("service.pid");
		String pid = asd.toString();
		
		Tocd theOcd = new Tocd();
		
		theOcd.setDescription("Description");
		theOcd.setName("Name");
		theOcd.setId("SelfConfiguringWire");
		
		Tad thaAd = new Tad();
		thaAd.setName("Attribute");
		thaAd.setId("Attribute");
		thaAd.setType(Tscalar.INTEGER);
		thaAd.setCardinality(0);
		thaAd.setRequired(true);
		thaAd.setDefault("1");
		
		theOcd.addAD(thaAd);
		
		m_properties = new HashMap<String, Object>();
		// Put the json configuration into the properties so to persist them
		// in the snapshot
		m_properties.put("Attribute", 1);
		ComponentConfigurationImpl cc = new ComponentConfigurationImpl(pid, theOcd, m_properties);
		return cc;

	}

	@Override
	public void consumersConnected(Wire[] arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object polled(Wire arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getEmitterPid() {
		// TODO Auto-generated method stub
		return null;
	}

}
