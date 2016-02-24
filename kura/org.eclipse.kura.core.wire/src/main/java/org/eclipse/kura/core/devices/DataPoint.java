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

import org.eclipse.kura.wires.WireField;
import org.eclipse.kura.wires.WireValue;

public class DataPoint {
	
	private WireField m_wireField;
	private List<ChangeListener> m_listeners;
	
	public DataPoint(WireField m_wireField) {
		super();
		m_listeners = new ArrayList<ChangeListener>();
		this.m_wireField = m_wireField;
	}
	
	public void addChangeListener(ChangeListener listener){
		m_listeners.add(listener);
	}
	
	public void removeChangeListener(ChangeListener listener){
		m_listeners.remove(listener);
	}
	
	public void updateWireField(WireField newField){
		if(!newField.getValue().equals(m_wireField.getValue())){
			m_wireField = newField;
			for(ChangeListener c : m_listeners){
				c.stateChanged(new ChangeEvent(this));
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	public WireValue getValue(){
		return m_wireField.getValue();
	}
	
	public String getName(){
		return m_wireField.getName();
	}
	
}
