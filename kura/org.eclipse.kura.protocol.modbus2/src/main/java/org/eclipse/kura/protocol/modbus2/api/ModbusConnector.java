package org.eclipse.kura.protocol.modbus2.api;

import org.eclipse.kura.protocol.modbus2.ModbusProtocolException;

public abstract class ModbusConnector {
	abstract public void connect();

	abstract public void disconnect() throws ModbusProtocolException;

	abstract public int getConnectStatus();

	abstract public byte[] msgTransaction(byte[] msg)
			throws ModbusProtocolException;
}
