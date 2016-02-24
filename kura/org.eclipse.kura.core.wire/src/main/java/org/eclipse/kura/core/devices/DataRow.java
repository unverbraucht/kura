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

import java.util.Map;

public abstract class DataRow {
	private int index;
	private DataPoint dataPoint;
	protected Map<String,Object> properties;
	
	public DataRow(int index, DataPoint dataPoint, Map<String, Object> properties) {
		this.index = index;
		this.dataPoint = dataPoint;
		this.properties = properties;
	}
	
	public int getIndex(){
		return index;
	}
	
	public DataPoint getDataPoint() {
		return dataPoint;
	}
	
	public void setDataPoint(DataPoint dataPoint) {
		this.dataPoint = dataPoint;
	}
	
	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}
	
	public abstract Map<String, Object> getProperties();
}
