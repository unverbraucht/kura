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

@SuppressWarnings("rawtypes")
public class WireField 
{
    private String    name;
    private WireValue value;
    
    public WireField(String name,
                     WireValue value) {
        this.name  = name;
        this.value = value;
        
    }

    public String getName() {
        return name;
    }

    public WireValue getValue() {
        return value;
    }
}
