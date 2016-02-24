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
package org.eclipse.kura.wires;

import java.util.List;

import org.osgi.service.wireadmin.BasicEnvelope;

public class WireEnvelope extends BasicEnvelope
{
	public WireEnvelope(String emitterPid, List<WireRecord> wireRecords)
	{
		super(wireRecords, emitterPid, null);
	}
	
	public String getEmitterPid() {
		return (String) getIdentification();
	}

	@SuppressWarnings("unchecked")
	public List<WireRecord> getRecords() {
		return (List<WireRecord>) getValue();
	}
}
