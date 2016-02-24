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

import java.util.HashMap;
import java.util.Map;

public class DataOutputRow extends DataRow{

	public DataOutputRow(int index, DataPoint dataPoint, Map<String, Object> properties) {
		super(index, dataPoint, properties);
	}
		
	public Map<String, Object> getProperties(){
		HashMap<String, Object> result = new HashMap<String, Object>();
		for(String key : properties.keySet()){
			result.put("o"+String.valueOf(getIndex())+"."+key, properties.get(key));
		}
		return result;
	}
}
