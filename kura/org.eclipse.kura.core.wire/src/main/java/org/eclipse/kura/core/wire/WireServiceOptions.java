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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Immutable object to capture the configuration of the Wire graph.
 */
public class WireServiceOptions 
{
	public static final String CONF_WIRES = "wires";
	
	private static List<WireConfiguration> m_wireConfigurations;
	
	private WireServiceOptions(List<WireConfiguration> confs) {	
		m_wireConfigurations = confs;
	}	
	
	
	public List<WireConfiguration> getWireConfigurations() {
		return Collections.unmodifiableList(m_wireConfigurations);
	}
	
	
	public static WireServiceOptions newInstance(Map<String,Object> properties) 
		throws JSONException 
	{
		List<WireConfiguration> wireConfs = new CopyOnWriteArrayList<WireConfiguration>();
		Object objWires = properties.get(CONF_WIRES);
		if (objWires instanceof String) {

			String strWires = (String) objWires; 
			JSONArray jsonWires = new JSONArray(strWires);
			for (int i=0; i<jsonWires.length(); i++) {
				JSONObject jsonWire = jsonWires.getJSONObject(i); 				
				wireConfs.add(WireConfiguration.newInstanceFromJson(jsonWire));
			}
		}
		return new WireServiceOptions(wireConfs);
	}

	public List<WireConfiguration> getWires(){
		return m_wireConfigurations;
	}

	public String toJsonString() 
		throws JSONException 
	{
		JSONArray jsonWires = new JSONArray();
		for (WireConfiguration wireConfig : m_wireConfigurations) {
			jsonWires.put(wireConfig.toJson());
		}
		return jsonWires.toString();
	}
	
}
