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

public class WireValueByte implements WireValue<Byte>
{
    private byte value;
    
    public WireValueByte(byte value) {
        this.value = value;
    }
    
    public WireDataType getType() {
        return WireDataType.BYTE;
    }

    public Byte getValue() {
        return value;
    }
}
