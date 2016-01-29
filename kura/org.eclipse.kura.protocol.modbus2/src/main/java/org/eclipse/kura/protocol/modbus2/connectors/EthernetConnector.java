package org.eclipse.kura.protocol.modbus2.connectors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.eclipse.kura.KuraConnectionStatus;
import org.eclipse.kura.protocol.modbus2.Crc16;
import org.eclipse.kura.protocol.modbus2.ModbusFunctionCodes;
import org.eclipse.kura.protocol.modbus2.ModbusProtocolErrorCode;
import org.eclipse.kura.protocol.modbus2.ModbusProtocolException;
import org.eclipse.kura.protocol.modbus2.ModbusTransmissionMode;
import org.eclipse.kura.protocol.modbus2.api.ModbusConnector;
import org.eclipse.kura.protocol.modbus2.api.ModbusProtocolProperties;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EthernetConnector extends ModbusConnector {

	private static final Logger s_logger = LoggerFactory.getLogger(EthernetConnector.class);

	InputStream inputStream;
	OutputStream outputStream;
	Socket socket;
	int port;
	String ipAddress;
	boolean connected = false;
	boolean m_connConfigd = false;
	
	int m_txMode;
	int m_respTimeout;

	public EthernetConnector(ConnectionFactory connFactory, ModbusProtocolProperties props)
			throws ModbusProtocolException {
		s_logger.debug("Configure TCP connection");
		
		port = Integer.parseInt(props.getPort());
		ipAddress = props.getIpAddress();
		m_txMode = 0;
		m_respTimeout = 5000;
		
		m_connConfigd = true;
		socket = new Socket();
	}

	public void connect() {
		if (!m_connConfigd) {
			s_logger.error("Can't connect, port not configured");
		} else {
			if (!connected) {
				try {
					socket = new Socket(ipAddress, port);
					try {
						inputStream = socket.getInputStream();
						outputStream = socket.getOutputStream();
						connected = true;
						s_logger.info("TCP connected");
					} catch (IOException e) {
						disconnect();
						s_logger.error("Failed to get socket streams: " + e);
					}
				} catch (IOException e) {
					s_logger.error("Failed to connect to remote: " + e);
				}
			}
		}
	}

	public void disconnect() {
		if (m_connConfigd) {
			if (connected) {
				try {
					if (!socket.isInputShutdown())
						socket.shutdownInput();
					if (!socket.isOutputShutdown())
						socket.shutdownOutput();
					socket.close();
				} catch (IOException eClose) {
					s_logger.error("Error closing TCP: "
							+ eClose);
				}
				inputStream = null;
				outputStream = null;
				connected = false;
				socket = null;
			}
		}
	}

	public int getConnectStatus() {
		if (connected)
			return KuraConnectionStatus.CONNECTED;
		else if (m_connConfigd)
			return KuraConnectionStatus.DISCONNECTED;
		else
			return KuraConnectionStatus.NEVERCONNECTED;
	}

	public byte[] msgTransaction(byte[] msg)
			throws ModbusProtocolException {
		byte[] cmd = null;

		if(m_txMode == ModbusTransmissionMode.RTU_MODE){
			cmd = new byte[msg.length+2];
			for(int i=0; i<msg.length; i++)
				cmd[i]=msg[i];
			// Add crc calculation to end of message
			int crc = Crc16.getCrc16(msg, msg.length, 0x0ffff);
			cmd[msg.length] = (byte) crc;
			cmd[msg.length + 1] = (byte) (crc >> 8);
		}
		else 				
			throw new ModbusProtocolException(ModbusProtocolErrorCode.METHOD_NOT_SUPPORTED,"Only RTU over TCP/IP supported");


		// Check connection status and connect
		connect();
		if (!connected)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
					"Cannot transact on closed socket");

		// Send the message
		try {
			// flush input
			while (inputStream.available() > 0)
				inputStream.read();
			// send all data
			outputStream.write(cmd, 0, cmd.length);
			outputStream.flush();
		} catch (IOException e) {
			// Assume this means the socket is closed...make sure it is
			s_logger.error("Socket disconnect in send: " + e);
			disconnect();
			throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,"Send failure: "
					+ e.getMessage());
		}

		// wait for and process response
		byte[] response = new byte[262]; // response buffer
		int respIndex = 0;
		int minimumLength = 5; // default minimum message length
		while (true) {
			while (respIndex < minimumLength) {
				try {
					socket.setSoTimeout(m_respTimeout);
					int resp = inputStream.read(response, respIndex,
							minimumLength - respIndex);
					if (resp > 0) {
						respIndex += resp;
					} else {
						s_logger.error("Socket disconnect in recv");
						disconnect();
						throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,"Recv failure");
					}
				} catch (SocketTimeoutException e) {
					String failMsg = "Recv timeout";
					s_logger.warn(failMsg);
					throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,failMsg);
				} catch (IOException e) {
					s_logger.error("Socket disconnect in recv: " + e);
					disconnect();
					throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
							"Recv failure");
				}
			}

			// Check first for an Exception response
			if ((response[1] & 0x80) == 0x80) {
				if (Crc16.getCrc16(response, 5, 0xffff) == 0)
					throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
							"Resp exception = "
									+ Byte.toString(response[2]));
			} else {
				// then check for a valid message
				switch (response[1]) {
				case ModbusFunctionCodes.FORCE_SINGLE_COIL:
				case ModbusFunctionCodes.PRESET_SINGLE_REG:
				case ModbusFunctionCodes.FORCE_MULTIPLE_COILS:
				case ModbusFunctionCodes.PRESET_MULTIPLE_REGS:
					if (respIndex < 8)
						// wait for more data
						minimumLength = 8;
					else if (Crc16.getCrc16(response, 8, 0xffff) == 0) {
						byte[] ret = new byte[8];
						for (int i = 0; i < 8; i++)
							ret[i] = response[i];
						return ret;
					}
					break;
				case ModbusFunctionCodes.READ_COIL_STATUS:
				case ModbusFunctionCodes.READ_INPUT_STATUS:
				case ModbusFunctionCodes.READ_INPUT_REGS:
				case ModbusFunctionCodes.READ_HOLDING_REGS:
					int byteCnt = (response[2] & 0xff) + 5;
					if (respIndex < byteCnt)
						// wait for more data
						minimumLength = byteCnt;
					else if (Crc16.getCrc16(response, byteCnt, 0xffff) == 0) {
						byte[] ret = new byte[byteCnt];
						for (int i = 0; i < byteCnt; i++)
							ret[i] = response[i];
						return ret;
					}
				}
			}

			/*
			 * if required length then must have failed, drop first byte and
			 * try again
			 */
			if (respIndex >= minimumLength)
				throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
						"Error in recv");
		}
	}


}
