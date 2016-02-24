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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.osgi.service.wireadmin.Consumer;
import org.osgi.service.wireadmin.Producer;
import org.osgi.service.wireadmin.Wire;

public class WireSupport implements Producer, Consumer
{
    private WireComponent m_wireSupporter;
    private List<Wire>    m_outgoingWires;
    private List<Wire>    m_incomingWires;
    
    public WireSupport(WireComponent wireSupporter)
    {
    	m_outgoingWires = new ArrayList<Wire>();
    	m_incomingWires = new ArrayList<Wire>();
        this.m_wireSupporter = wireSupporter;
    }
    

    public synchronized void emit(WireRecord... wireRecords) 
    {
    	emit(Arrays.asList(wireRecords));
    }
    
    
    public synchronized void emit(List<WireRecord> wireRecords) 
    {
    	if (m_wireSupporter instanceof WireEmitter) {    		
    		String emitterPid = ((WireEmitter) m_wireSupporter).getEmitterPid();
    		WireEnvelope wei = new WireEnvelope(emitterPid, wireRecords);  
        	for (Wire wire : m_outgoingWires) {
        		wire.update(wei);
        	}
    	}
    	else {
    		// no-op if this supported is not an emitter
    	}
    }
        
    
    public List<Wire> getOutgoingWires() {
    	return Collections.unmodifiableList(m_outgoingWires);
    }

    
    public List<Wire> getIncomingWires() {
    	return Collections.unmodifiableList(m_incomingWires);
    }

    
    // ----------------------------------------------------------------
    //
    //   Wire Producer APIs
    //
    // ----------------------------------------------------------------
    
	@Override
	public synchronized Object polled(Wire wire) {
		// return the latest emitted record		
		return wire.getLastValue();
	}

	@Override
	public synchronized void consumersConnected(Wire[] wires) {
		m_outgoingWires = Arrays.asList(wires);
	}
	

    // ----------------------------------------------------------------
    //
    //   Wire Consumer APIs
    //
    // ----------------------------------------------------------------

	@Override
	public void updated(Wire wire, Object value) {
        if (value instanceof WireEnvelope) {
            if (m_wireSupporter instanceof WireReceiver) {
                ((WireReceiver) m_wireSupporter).onWireReceive((WireEnvelope) value);
            }
        }
	}

	@Override
	public void producersConnected(Wire[] wires) {
		m_incomingWires = Arrays.asList(wires); 
	}
}
