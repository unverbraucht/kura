package org.eclipse.kura.example.wire.modbus;

import java.util.Properties;

import org.eclipse.kura.KuraConnectionStatus;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.protocol.modbus.ModbusProtocolDeviceService;
import org.eclipse.kura.protocol.modbus.ModbusProtocolException;
import org.eclipse.kura.wire.DeviceConnection;

public class ModbusConnection implements DeviceConnection {

	/**
	 * Protocol address syntax
	 * 
	 * <Function code>/<Slave address>/<Data address>/<Count>
	 */

	private static final int FUNCTION_CODE_WRITE_SINGLE_COIL = 0;
	private static final int FUNCTION_CODE_WRITE_SINGLE_REGISTER = 1;
	private static final int FUNCTION_CODE_WRITE_MULTIPLE_COILS = 2;
	private static final int FUNCTION_CODE_WRITE_MULTIPLE_REGISTERS = 3;

	private static final int FUNCTION_CODE_READ_COILS = 4;
	private static final int FUNCTION_CODE_READ_DISCRETE_INPUTS = 5;
	private static final int FUNCTION_CODE_READ_HOLDING_REGISTERS = 6;
	private static final int FUNCTION_CODE_READ_INPUT_REGISTERS = 7;

	private static ModbusProtocolDeviceService m_modbusDevice;

	private Properties m_connectionProps;

	public ModbusConnection(Properties props, ModbusProtocolDeviceService service) {
		m_connectionProps = props;
		m_modbusDevice = service;
	}

	@Override
	public void connect() throws KuraException {
		try {
			m_modbusDevice.configureConnection(m_connectionProps);
			m_modbusDevice.connect();
		} catch (ModbusProtocolException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	@Override
	public void disconnect() throws KuraException {
		try {
			m_modbusDevice.disconnect();
		} catch (ModbusProtocolException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	@Override
	public boolean isConnected() {
		int status = m_modbusDevice.getConnectStatus();
		return status == KuraConnectionStatus.CONNECTED;
	}

	@Override
	public void write(String address, Object value) throws KuraException {
		ModbusAddress modbusAddress = new ModbusAddress(address);

		try {
			switch (modbusAddress.getFunctionCode()) {
			case FUNCTION_CODE_WRITE_SINGLE_COIL:
				m_modbusDevice.writeSingleCoil(
						modbusAddress.getSlaveAddress(),
						modbusAddress.getDataAddress(), 
						(Boolean) value);
				return;
			case FUNCTION_CODE_WRITE_SINGLE_REGISTER:
				m_modbusDevice.writeSingleRegister(
						modbusAddress.getSlaveAddress(),
						modbusAddress.getDataAddress(), 
						(Integer) value);
				return;
			case FUNCTION_CODE_WRITE_MULTIPLE_COILS:
				m_modbusDevice.writeMultipleCoils(
						modbusAddress.getSlaveAddress(),
						modbusAddress.getDataAddress(), 
						(boolean[]) value);
				return;
			case FUNCTION_CODE_WRITE_MULTIPLE_REGISTERS:
				m_modbusDevice.writeMultipleRegister(
						modbusAddress.getSlaveAddress(),
						modbusAddress.getDataAddress(), 
						(int[]) value);
				return;
			}
		} catch (ModbusProtocolException ex) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, ex);
		} catch (ClassCastException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR,
					"Invalid data!");
		}

	}

	@Override
	public Object read(String address) throws KuraException {
		ModbusAddress modbusAddress = new ModbusAddress(address);

		try {
			switch (modbusAddress.getFunctionCode()) {
			case FUNCTION_CODE_READ_COILS:
				return m_modbusDevice.readCoils(
						modbusAddress.getSlaveAddress(),
						modbusAddress.getDataAddress(),
						modbusAddress.getCount());
			case FUNCTION_CODE_READ_DISCRETE_INPUTS:
				return m_modbusDevice.readDiscreteInputs(
						modbusAddress.getSlaveAddress(),
						modbusAddress.getDataAddress(),
						modbusAddress.getCount());
			case FUNCTION_CODE_READ_HOLDING_REGISTERS:
				return m_modbusDevice.readHoldingRegisters(
						modbusAddress.getSlaveAddress(),
						modbusAddress.getDataAddress(),
						modbusAddress.getCount());
			case FUNCTION_CODE_READ_INPUT_REGISTERS:
				return m_modbusDevice.readInputRegisters(
						modbusAddress.getSlaveAddress(),
						modbusAddress.getDataAddress(),
						modbusAddress.getCount());
			}
		} catch (ModbusProtocolException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}

		throw new KuraException(KuraErrorCode.INTERNAL_ERROR,
				"Unsupported function code");

	}

	@Override
	public String getDriverDescription() {
		return "Example Driver for ModBus";
	}

	@Override
	public String getAddressSintax() {
		return "<Function code>/<Slave address>/<Data address>/<Count>";
	}

	@Override
	public String getHelp() {
		return null;
	}

}
