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
package org.eclipse.kura.protocol.modbus2.api;

import org.eclipse.kura.protocol.modbus2.ModbusProtocolException;

public abstract class ModbusConnector {
	abstract public void connect();

	abstract public void disconnect() throws ModbusProtocolException;

	abstract public int getConnectStatus();

	abstract public byte[] msgTransaction(byte[] msg)
			throws ModbusProtocolException;
}
