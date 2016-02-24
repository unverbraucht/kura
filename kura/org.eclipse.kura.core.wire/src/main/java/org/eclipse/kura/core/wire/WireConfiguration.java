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

import org.json.JSONException;
import org.json.JSONObject;

public class WireConfiguration {
	private String m_emitterPid;
	private String m_receiverPid;
	private String m_filter;
	private boolean m_created = false;

	WireConfiguration(String emitterPid, String receiverPid, String filter) {
		super();
		this.m_emitterPid = emitterPid;
		this.m_receiverPid = receiverPid;
		this.m_filter = filter;
	}

	WireConfiguration(String emitterPid, String receiverPid, String filter, boolean created) {
		super();
		this.m_emitterPid = emitterPid;
		this.m_receiverPid = receiverPid;
		this.m_filter = filter;
	}

	public static WireConfiguration newInstanceFromJson(JSONObject jsonWire) throws JSONException {
		String emitter = jsonWire.getString("p");
		String receiver = jsonWire.getString("c");
		String filter = jsonWire.optString("f");
		return new WireConfiguration(emitter, receiver, filter);
	}

	public JSONObject toJson() throws JSONException {
		JSONObject jsonWire = new JSONObject();
		jsonWire.put("p", m_emitterPid);
		jsonWire.put("c", m_receiverPid);
		if (m_filter != null && !m_filter.isEmpty()) {
			jsonWire.putOpt("f", m_filter);
		}
		return jsonWire;
	}

	public String getEmitterPid() {
		return m_emitterPid;
	}

	public String getReceiverPid() {
		return m_receiverPid;
	}

	public String getFilter() {
		return m_filter;
	}

	public boolean isCreated() {
		return m_created;
	}

	public void setCreated(boolean created) {
		m_created = created;
	}
	
	public void update(String newEmitterPid, String newReceiverPid){
		m_emitterPid = newEmitterPid;
		m_receiverPid = newReceiverPid;
	}
}
