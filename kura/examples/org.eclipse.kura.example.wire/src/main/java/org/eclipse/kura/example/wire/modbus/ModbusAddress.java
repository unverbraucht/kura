package org.eclipse.kura.example.wire.modbus;

import java.util.IllegalFormatException;
import java.util.IllegalFormatWidthException;

public class ModbusAddress {

	private final int functionCode;
	private final int slaveAddress;
	private final int dataAddress;
	private final int count;
	
	public ModbusAddress(String address) throws IllegalFormatException, NumberFormatException{
		String[] tokens = address.split("/");
		if(tokens.length<4){
			throw new IllegalFormatWidthException(4);
		}
		
		functionCode = Integer.parseInt(tokens[0]);
		slaveAddress = Integer.parseInt(tokens[1]);
		dataAddress = Integer.parseInt(tokens[2]);
		count = Integer.parseInt(tokens[3]);
	}

	public int getFunctionCode() {
		return functionCode;
	}

	public int getSlaveAddress() {
		return slaveAddress;
	}

	public int getDataAddress() {
		return dataAddress;
	}

	public int getCount() {
		return count;
	}
	
}
