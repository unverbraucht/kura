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

public class WireValueBoolean implements WireValue<Boolean>
{
    private boolean value;
    
    public WireValueBoolean(boolean value) {
        this.value = value;
    }
    
    public WireDataType getType() {
        return WireDataType.BOOLEAN;
    }

    public Boolean getValue() {
        return value;
    }
}
