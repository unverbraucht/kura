package org.eclipse.kura.protocol.modbus2.impl;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraConnectionStatus;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.protocol.modbus2.ModbusCommEvent;
import org.eclipse.kura.protocol.modbus2.ModbusDataOrder;
import org.eclipse.kura.protocol.modbus2.ModbusFunctionCodes;
import org.eclipse.kura.protocol.modbus2.ModbusProtocolErrorCode;
import org.eclipse.kura.protocol.modbus2.ModbusProtocolException;
import org.eclipse.kura.protocol.modbus2.ModbusTransmissionMode;
import org.eclipse.kura.protocol.modbus2.api.ModbusConnector;
import org.eclipse.kura.protocol.modbus2.api.ModbusProtocolDeviceService;
import org.eclipse.kura.protocol.modbus2.api.ModbusProtocolProperties;
import org.eclipse.kura.protocol.modbus2.api.ModbusProtocolService;
import org.eclipse.kura.protocol.modbus2.connectors.EthernetConnector;
import org.eclipse.kura.protocol.modbus2.connectors.SerialConnector;
import org.eclipse.kura.usb.UsbService;
import org.eclipse.kura.usb.UsbTtyDevice;
import org.eclipse.kura.wire.DeviceConnection;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusProtocolDeviceServiceImpl implements ConfigurableComponent, DeviceConnection, ModbusProtocolDeviceService {

	private Logger s_logger = LoggerFactory.getLogger(ModbusProtocolDeviceServiceImpl.class);

	public static final String PROTOCOL_CONNECTION_TYPE_SERIAL = "SERIAL";
	public static final String SERIAL_232 = "RS232";
	public static final String SERIAL_485 = "RS485";
	public static final String PROTOCOL_CONNECTION_TYPE_ETHER_TCP = "ETHERTCP";

	private String m_instanceName;

	private ModbusProtocolService m_modbusService;
	private UsbService m_usbService;
	private ConnectionFactory m_connectionFactory;

	private String m_connType = null;
	private boolean m_protConfigd = false;
	private int m_txMode;
	private int m_respTout;
	private boolean m_connConfigd = false;
	private ModbusConnector m_comm;

	private ModbusProtocolProperties m_modbus_props;
	
	private boolean m_serial485 = false;

	public ModbusProtocolDeviceServiceImpl() {
		m_instanceName = new StringBuilder("modbus_").append(String.valueOf(System.currentTimeMillis())).toString();
	}

	public void setModbusService(ModbusProtocolService modbusProtocolService) {
		m_modbusService = modbusProtocolService;
	}

	public void unsetModbusService(ModbusProtocolService modbusProtocolService) {
		m_modbusService = null;
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		m_connectionFactory = connectionFactory;
	}

	public void unsetConnectionFactory(ConnectionFactory connectionFactory) {
		m_connectionFactory = null;
	}

	protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
		s_logger.info("Activating ModbusProtocolDeviceService...");

		updated(properties);
		s_logger.info("Activating ModbusProtocolDeviceService... Done.");
	}

	protected void deactivate(ComponentContext componentContext) {
		s_logger.info("Deactivating ModbusProtocolDeviceService...");

		s_logger.info("Deactivating ModbusProtocolDeviceService... Done.");
	}

	protected void updated(Map<String, Object> properties) {
		s_logger.info("Updating ModbusProtocolDeviceService...");
		
		m_modbus_props = new ModbusProtocolProperties(properties);
		
		m_instanceName = m_modbus_props.getInstanceName();

		try {
			configureConnection(m_modbus_props);
		} catch (ModbusProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		s_logger.info("Updating ModbusProtocolDeviceService... Done.");
	}

	@Override
	public String getInstanceName() {
		return m_instanceName;
	}

	@Override
	public void configureConnection(ModbusProtocolProperties props) throws ModbusProtocolException {
		if ((m_connType = props.getSerialMode()) == null)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_CONFIGURATION);

		String txMode;
		String respTimeout;

		m_txMode = ModbusTransmissionMode.RTU_MODE;
		m_respTout = 5000;

		m_protConfigd = true;

		if (m_connConfigd) {
			m_comm.disconnect();
			m_comm = null;
			m_connConfigd = false;
		}

		if (m_connType.equals(ModbusProtocolProperties.MODE_RS232) || m_connType.equals(ModbusProtocolProperties.MODE_RS485)) {
			if (!serialPortExists())
				throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_AVAILABLE);
			m_comm = new SerialConnector(m_connectionFactory, props);
		} else if (m_connType.equals(PROTOCOL_CONNECTION_TYPE_ETHER_TCP))
			m_comm = new EthernetConnector(m_connectionFactory, props);
		else
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_CONFIGURATION);

		m_connConfigd = true;
	}

	@Override
	public int getConnectStatus() {
		if (!m_connConfigd)
			return KuraConnectionStatus.NEVERCONNECTED;
		return m_comm.getConnectStatus();
	}

	@Override
	public boolean[] readCoils(int unitAddr, int dataAddress, int count) throws ModbusProtocolException {
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);

		boolean[] ret = new boolean[count];
		int index = 0;

		byte[] resp;
		/*
		 * construct the command issue and get results
		 */
		byte[] cmd = new byte[6];
		cmd[0] = (byte) unitAddr;
		cmd[1] = (byte) ModbusFunctionCodes.READ_COIL_STATUS;
		cmd[2] = (byte) (dataAddress / 256);
		cmd[3] = (byte) (dataAddress % 256);
		cmd[4] = (byte) (count / 256);
		cmd[5] = (byte) (count % 256);

		/*
		 * send the message and get the response
		 */
		resp = m_comm.msgTransaction(cmd);

		/*
		 * process the response (address & CRC already confirmed)
		 */
		if ((resp.length < 3) || (resp.length < ((resp[2] & 0xff) + 3)))
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
		if ((resp[2] & 0xff) == ((count + 7) / 8)) {
			byte mask = 1;
			int byteOffset = 3;
			for (int j = 0; j < count; j++, index++) {
				// get this point's value
				if ((resp[byteOffset] & mask) == mask)
					ret[index] = true;
				else
					ret[index] = false;
				// advance the mask and offset index
				if ((mask <<= 1) == 0) {
					mask = 1;
					byteOffset++;
				}
			}
		} else
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_ADDRESS);

		return ret;
	}

	@Override
	public boolean[] readDiscreteInputs(int unitAddr, int dataAddress, int count) throws ModbusProtocolException {
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);

		boolean[] ret = new boolean[count];
		int index = 0;

		byte[] resp;
		/*
		 * construct the command issue and get results
		 */
		byte[] cmd = new byte[6];
		cmd[0] = (byte) unitAddr;
		cmd[1] = (byte) ModbusFunctionCodes.READ_INPUT_STATUS;
		cmd[2] = (byte) (dataAddress / 256);
		cmd[3] = (byte) (dataAddress % 256);
		cmd[4] = (byte) (count / 256);
		cmd[5] = (byte) (count % 256);

		/*
		 * send the message and get the response
		 */
		resp = m_comm.msgTransaction(cmd);

		/*
		 * process the response (address & CRC already confirmed)
		 */
		if ((resp.length < 3) || (resp.length < ((resp[2] & 0xff) + 3)))
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
		if ((resp[2] & 0xff) == ((count + 7) / 8)) {
			byte mask = 1;
			int byteOffset = 3;
			for (int j = 0; j < count; j++, index++) {
				// get this point's value
				if ((resp[byteOffset] & mask) == mask)
					ret[index] = true;
				else
					ret[index] = false;
				// advance the mask and offset index
				if ((mask <<= 1) == 0) {
					mask = 1;
					byteOffset++;
				}
			}
		} else
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_ADDRESS);

		return ret;
	}

	@Override
	public void writeSingleCoil(int unitAddr, int dataAddress, boolean data) throws ModbusProtocolException {
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);

		byte[] resp;

		byte[] cmd = new byte[6];
		cmd[0] = (byte) unitAddr;
		cmd[1] = ModbusFunctionCodes.FORCE_SINGLE_COIL;
		cmd[2] = (byte) (dataAddress / 256);
		cmd[3] = (byte) (dataAddress % 256);
		cmd[4] = (data == true) ? (byte) 0xff : (byte) 0;
		cmd[5] = 0;

		/*
		 * send the message and get the response
		 */
		resp = m_comm.msgTransaction(cmd);

		/*
		 * process the response
		 */
		if (resp.length < 6)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
		for (int i = 0; i < 6; i++)
			if (cmd[i] != resp[i])
				throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
	}

	@Override
	public void writeMultipleCoils(int unitAddr, int dataAddress, boolean[] data) throws ModbusProtocolException {
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);

		/*
		 * write multiple boolean values
		 */
		int localCnt = data.length;
		int index = 0;
		byte[] resp;
		/*
		 * construct the command, issue and verify response
		 */
		int dataLength = (localCnt + 7) / 8;
		byte[] cmd = new byte[dataLength + 7];
		cmd[0] = (byte) unitAddr;
		cmd[1] = ModbusFunctionCodes.FORCE_MULTIPLE_COILS;
		cmd[2] = (byte) (dataAddress / 256);
		cmd[3] = (byte) (dataAddress % 256);
		cmd[4] = (byte) (localCnt / 256);
		cmd[5] = (byte) (localCnt % 256);
		cmd[6] = (byte) dataLength;

		// put the data on the command
		byte mask = 1;
		int byteOffset = 7;
		cmd[byteOffset] = 0;
		for (int j = 0; j < localCnt; j++, index++) {
			// get this point's value
			if (data[index])
				cmd[byteOffset] += mask;
			// advance the mask and offset index
			if ((mask <<= 1) == 0) {
				mask = 1;
				byteOffset++;
				cmd[byteOffset] = 0;
			}
		}

		/*
		 * send the message and get the response
		 */
		resp = m_comm.msgTransaction(cmd);

		/*
		 * process the response
		 */
		if (resp.length < 6)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
		for (int j = 0; j < 6; j++)
			if (cmd[j] != resp[j])
				throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
	}

	@Override
	public int[] readHoldingRegisters(int unitAddr, int dataAddress, int count) throws ModbusProtocolException {
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);

		int[] ret = new int[count];
		int index = 0;

		byte[] resp;
		/*
		 * construct the command issue and get results, putting the results away
		 * at index and then incrementing index for the next command
		 */
		byte[] cmd = new byte[6];
		cmd[0] = (byte) unitAddr;
		cmd[1] = (byte) ModbusFunctionCodes.READ_HOLDING_REGS;
		cmd[2] = (byte) (dataAddress / 256);
		cmd[3] = (byte) (dataAddress % 256);
		cmd[4] = 0;
		cmd[5] = (byte) count;

		/*
		 * send the message and get the response
		 */
		resp = m_comm.msgTransaction(cmd);

		/*
		 * process the response (address & CRC already confirmed)
		 */
		if ((resp.length < 3) || (resp.length < ((resp[2] & 0xff) + 3)))
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
		if ((resp[2] & 0xff) == (count * 2)) {
			int byteOffset = 3;
			for (int j = 0; j < count; j++, index++) {
				int val = resp[byteOffset + (ModbusDataOrder.MODBUS_WORD_ORDER_BIG_ENDIAN.charAt(0) - '1')] & 0xff;
				val <<= 8;
				val += resp[byteOffset + (ModbusDataOrder.MODBUS_WORD_ORDER_BIG_ENDIAN.charAt(1) - '1')] & 0xff;

				ret[index] = val;

				byteOffset += 2;
			}
		} else
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_ADDRESS);
		return ret;
	}

	@Override
	public int[] readInputRegisters(int unitAddr, int dataAddress, int count) throws ModbusProtocolException {
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);

		int[] ret = new int[count];
		int index = 0;

		byte[] resp;
		/*
		 * construct the command issue and get results, putting the results away
		 * at index and then incrementing index for the next command
		 */
		byte[] cmd = new byte[6];
		cmd[0] = (byte) unitAddr;
		cmd[1] = (byte) ModbusFunctionCodes.READ_INPUT_REGS;
		cmd[2] = (byte) (dataAddress / 256);
		cmd[3] = (byte) (dataAddress % 256);
		cmd[4] = 0;
		cmd[5] = (byte) count;

		/*
		 * send the message and get the response
		 */
		resp = m_comm.msgTransaction(cmd);

		/*
		 * process the response (address & CRC already confirmed)
		 */
		if ((resp.length < 3) || (resp.length < ((resp[2] & 0xff) + 3)))
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
		if ((resp[2] & 0xff) == (count * 2)) {
			int byteOffset = 3;
			for (int j = 0; j < count; j++, index++) {
				int val = resp[byteOffset + (ModbusDataOrder.MODBUS_WORD_ORDER_BIG_ENDIAN.charAt(0) - '1')] & 0xff;
				val <<= 8;
				val += resp[byteOffset + (ModbusDataOrder.MODBUS_WORD_ORDER_BIG_ENDIAN.charAt(1) - '1')] & 0xff;

				ret[index] = val;

				byteOffset += 2;
			}
		} else
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_ADDRESS);
		return ret;
	}

	@Override
	public void writeSingleRegister(int unitAddr, int dataAddress, int data) throws ModbusProtocolException {
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);

		byte[] cmd = new byte[6];
		cmd[0] = (byte) unitAddr;
		cmd[1] = ModbusFunctionCodes.PRESET_SINGLE_REG;
		cmd[2] = (byte) (dataAddress / 256);
		cmd[3] = (byte) (dataAddress % 256);
		cmd[4] = (byte) (data >> 8);
		cmd[5] = (byte) data;

		/*
		 * send the message and get the response
		 */
		byte[] resp = m_comm.msgTransaction(cmd);

		/*
		 * process the response
		 */
		if (resp.length < 6)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
		for (int i = 0; i < 6; i++)
			if (cmd[i] != resp[i])
				throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
	}

	@Override
	public boolean[] readExceptionStatus(int unitAddr) throws ModbusProtocolException {
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);

		boolean[] ret = new boolean[8];
		int index = 0;

		byte[] resp;
		/*
		 * construct the command issue and get results
		 */
		byte[] cmd = new byte[2];
		cmd[0] = (byte) unitAddr;
		cmd[1] = (byte) ModbusFunctionCodes.READ_EXCEPTION_STATUS;

		/*
		 * send the message and get the response
		 */
		resp = m_comm.msgTransaction(cmd);

		/*
		 * process the response (address & CRC already confirmed)
		 */
		if (resp.length < 3)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
		byte mask = 1;
		for (int j = 0; j < 8; j++, index++) {
			// get this point's value
			if ((resp[2] & mask) == mask)
				ret[index] = true;
			else
				ret[index] = false;
			// advance the mask and offset index
			if ((mask <<= 1) == 0) {
				mask = 1;
			}
		}

		return ret;
	}

	@Override
	public ModbusCommEvent getCommEventCounter(int unitAddr) throws ModbusProtocolException {
		ModbusCommEvent mce = new ModbusCommEvent();
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);

		/*
		 * construct the command issue and get results
		 */
		byte[] cmd = new byte[2];
		cmd[0] = (byte) unitAddr;
		cmd[1] = (byte) ModbusFunctionCodes.GET_COMM_EVENT_COUNTER;

		/*
		 * send the message and get the response
		 */
		byte[] resp;
		resp = m_comm.msgTransaction(cmd);

		/*
		 * process the response (address & CRC already confirmed)
		 */
		if (resp.length < 6)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
		int val = resp[2] & 0xff;
		val <<= 8;
		val += resp[3] & 0xff;
		mce.setStatus(val);
		val = resp[4] & 0xff;
		val <<= 8;
		val += resp[5] & 0xff;
		mce.setEventCount(val);

		return mce;
	}

	@Override
	public ModbusCommEvent getCommEventLog(int unitAddr) throws ModbusProtocolException {
		ModbusCommEvent mce = new ModbusCommEvent();
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);

		/*
		 * construct the command issue and get results
		 */
		byte[] cmd = new byte[2];
		cmd[0] = (byte) unitAddr;
		cmd[1] = (byte) ModbusFunctionCodes.GET_COMM_EVENT_LOG;

		/*
		 * send the message and get the response
		 */
		byte[] resp;
		resp = m_comm.msgTransaction(cmd);

		/*
		 * process the response (address & CRC already confirmed)
		 */
		if ((resp.length < ((resp[2] & 0xff) + 3)) || ((resp[2] & 0xff) > 64 + 7))
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
		int val = resp[3] & 0xff;
		val <<= 8;
		val += resp[4] & 0xff;
		mce.setStatus(val);

		val = resp[5] & 0xff;
		val <<= 8;
		val += resp[6] & 0xff;
		mce.setEventCount(val);

		val = resp[7] & 0xff;
		val <<= 8;
		val += resp[8] & 0xff;
		mce.setMessageCount(val);

		int count = (resp[2] & 0xff) - 4;
		int[] events = new int[count];
		for (int j = 0; j < count; j++) {
			int bval = resp[9 + j] & 0xff;
			events[j] = bval;
		}
		mce.setEvents(events);

		return mce;
	}

	@Override
	public void writeMultipleRegister(int unitAddr, int dataAddress, int[] data) throws ModbusProtocolException {
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);

		int localCnt = data.length;
		/*
		 * construct the command, issue and verify response
		 */
		int dataLength = localCnt * 2;
		byte[] cmd = new byte[dataLength + 7];
		cmd[0] = (byte) unitAddr;
		cmd[1] = ModbusFunctionCodes.PRESET_MULTIPLE_REGS;
		cmd[2] = (byte) (dataAddress / 256);
		cmd[3] = (byte) (dataAddress % 256);
		cmd[4] = (byte) (localCnt / 256);
		cmd[5] = (byte) (localCnt % 256);
		cmd[6] = (byte) dataLength;

		// put the data on the command
		int byteOffset = 7;
		int index = 0;
		for (int j = 0; j < localCnt; j++, index++) {
			cmd[byteOffset + (ModbusDataOrder.MODBUS_WORD_ORDER_BIG_ENDIAN.charAt(0) - '1')] = (byte) (data[index] >> 8);
			cmd[byteOffset + (ModbusDataOrder.MODBUS_WORD_ORDER_BIG_ENDIAN.charAt(1) - '1')] = (byte) data[index];

			byteOffset += 2;
		}

		/*
		 * send the message and get the response
		 */
		byte[] resp = m_comm.msgTransaction(cmd);

		/*
		 * process the response
		 */
		if (resp.length < 6)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
		for (int j = 0; j < 6; j++)
			if (cmd[j] != resp[j])
				throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
	}

	@Override
	public void write(String address, Object value) throws KuraException {
		// TODO Auto-generated method stub

	}

	@Override
	public Object read(String address) throws KuraException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDriverDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAddressSintax() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getHelp() {
		// TODO Auto-generated method stub
		return null;
	}

	// ----------------------------------------------------------------
	//
	// Private methods
	//
	// ----------------------------------------------------------------

	private boolean serialPortExists() {
		if (m_modbus_props== null)
			return false;

		String portName = m_modbus_props.getPort();
		if (portName != null) {
			if (portName.contains("/dev/")) {
				File f = new File(portName);
				if (f.exists()) {
					return true;
				}
			} else {
				List<UsbTtyDevice> utd = m_usbService.getUsbTtyDevices();
				if (utd != null) {
					for (UsbTtyDevice u : utd) {
						if (portName.equals(u.getUsbPort())) {
							// replace device number with tty
							portName = u.getDeviceNode();
							m_modbus_props.setPort(portName);
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	@Override
	public void connect() throws KuraException {
		if (m_comm != null) {
			m_comm.connect();
		} else {
			throw new KuraConnectException("Undefined driver");
		}
	}

	@Override
	public void disconnect() throws KuraException {
		if (m_comm != null) {
			m_comm.disconnect();
		} else {
			throw new KuraConnectException("Undefined driver");
		}
	}

	@Override
	public boolean isConnected() {
		if (m_comm != null) {
			return m_comm.getConnectStatus() == KuraConnectionStatus.CONNECTED;
		} else {
			return false;
		}
	}

}
