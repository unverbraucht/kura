package org.eclipse.kura.protocol.modbus2.connectors;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.kura.KuraConnectionStatus;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
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

public class SerialConnector extends ModbusConnector {
	
	private static final Logger s_logger = LoggerFactory.getLogger(SerialConnector.class);

	public static final String SERIAL_232 = "RS232";
	public static final String SERIAL_485 = "RS485";
	
	InputStream in;
	OutputStream out;
	CommConnection conn=null;

	FileWriter gpioModeSwitch;
	
	boolean m_serial485;
	int m_txMode;
	int m_respTimeout;
	

	public SerialConnector(ConnectionFactory connFactory, ModbusProtocolProperties props)
			throws ModbusProtocolException {
		s_logger.debug("Configure serial connection");
		
		
		String sPort;
		String gpioSwitchPin = null;
		String gpioRsModePin = null;

		sPort = props.getPort();
		
		int baud = Integer.parseInt(props.getBaudRate());
		int stop = Integer.parseInt(props.getStopBits());
		int parity = Integer.parseInt(props.getParity());
		int bits = Integer.parseInt(props.getBitsPerWord());
		m_txMode = 0;
		m_respTimeout = 5000;
		
			m_serial485=(props.getSerialMode()==ModbusProtocolProperties.MODE_RS485);
//			if(m_serial485){
//				if (((gpioSwitchPin = connectionConfig.getProperty("serialGPIOswitch"))==null)
//					|| ((gpioRsModePin = connectionConfig.getProperty("serialGPIOrsmode"))==null))
//				throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_CONFIGURATION);

		String uri = new CommURI.Builder(sPort)
								.withBaudRate(baud)
								.withDataBits(bits)
								.withStopBits(stop)
								.withParity(parity)
								.withTimeout(2000)
								.build().toString();

		try {
			conn = (CommConnection) connFactory.createConnection(uri, 1, false);
		} catch (IOException e1) {
			throw new ModbusProtocolException(ModbusProtocolErrorCode.CONNECTION_FAILURE,e1);
		}
		if(m_serial485){
			setupSerialGpio(gpioRsModePin, gpioSwitchPin);
		}

		// get the streams
		try {
			in = conn.openInputStream();
			out = conn.openOutputStream();
			if(m_serial485)
				gpioModeSwitch  = new FileWriter(new File("/sys/class/gpio/" + gpioSwitchPin + "/value"));
		} catch (Exception e) {
			throw new ModbusProtocolException(ModbusProtocolErrorCode.CONNECTION_FAILURE,e);
		}
	}

	/**
	 * Initializes the GPIO Serial Ports to RS485 mode with output direction.
	 * Communication can then be switched to RX or TX.
	 * @param gpioRsMode
	 * @param gpioSwitch
	 * @throws ModbusProtocolException 
	 */
	private void setupSerialGpio(String gpioRsMode, String gpioSwitch) throws ModbusProtocolException {
		final String[] requiredGpio = {
				gpioRsMode, /* port 3 RS mode */
				gpioSwitch /* port TX<->RX switch */
		};

		for (String gpio : requiredGpio) {
			File gpioFile = new File("/sys/class/gpio/" + gpio);
			if (!gpioFile.exists()) {
				// # Pin is not exported, so do it now
				FileWriter fwExport = null;
				try {
					fwExport = new FileWriter(new File("/sys/class/gpio/export"));
					// write only the PIN number
					fwExport.write(gpio.replace("gpio", ""));
					fwExport.flush();
					s_logger.debug("Exported GPIO {}", gpio);

				} catch (IOException e) {
					s_logger.error("Exporting Error", e);
				} finally {
					try {
						fwExport.close();
					} catch (IOException e) {
						s_logger.error("Error closing export file");
					}
				}
				// wait a little after exporting
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			};


			// After exporting the pin, set the direction to "out"
			FileWriter fwDirection = null;
			try {
				fwDirection = new FileWriter(new File("/sys/class/gpio/" + gpio + "/direction"));
				fwDirection.write("out");
				fwDirection.flush();
				s_logger.debug("Direction GPIO {}", gpio);

			} catch (IOException e) {
				s_logger.error("Direction Error", e);
			} finally {
				try {
					fwDirection.close();
				} catch (IOException e) {
					s_logger.error("Error closing export file");
				}
			}
			
			//Switch to RS485 mode
			FileWriter fwValue = null;
			try {
				fwValue = new FileWriter(new File("/sys/class/gpio/" + gpio + "/value"));
				fwValue.write("1");
				fwValue.flush();
				s_logger.debug("Value GPIO {}", gpio);

			} catch (IOException e) {
				s_logger.error("Value Error", e);
			} finally {
				try {
					fwValue.close();
				} catch (IOException e) {
					s_logger.error("Error closing value file");
				}
			}

		}

	}
	
	/**
	 * Sets the GPIO Serial port to Transmit mode. Data can be written to OutputStream.
	 * @throws InterruptedException 
	 */
	private void switchTX() throws ModbusProtocolException {
		try {
			gpioModeSwitch.write("1");
			gpioModeSwitch.flush();
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				throw new ModbusProtocolException(ModbusProtocolErrorCode.CONNECTION_FAILURE,"switchTX interrupted");
			}
		} catch (IOException e) {
			throw new ModbusProtocolException(ModbusProtocolErrorCode.CONNECTION_FAILURE,"switchTX IOException "+e.getMessage());
		}
	}

	/**
	 * Sets the GPIO Serial port to Receive mode. Data can be read from InputStream.
	 * @throws InterruptedException 
	 */
	private void switchRX() throws ModbusProtocolException {
		try {
			gpioModeSwitch.write("0");
			gpioModeSwitch.flush();
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				throw new ModbusProtocolException(ModbusProtocolErrorCode.CONNECTION_FAILURE,"switchRX interrupted");
			}
		} catch (IOException e) {
			throw new ModbusProtocolException(ModbusProtocolErrorCode.CONNECTION_FAILURE,"switchRX IOException "+e.getMessage());
		}
	}

	public void connect() {
		/*
		 * always connected
		 */
	}

	public void disconnect() throws ModbusProtocolException {
		if (conn!=null) {
			try {
				conn.close();
				s_logger.debug("Serial connection closed");
			} catch (IOException e) {
				throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,e.getMessage());
			}
			conn = null;
		}
		if(m_serial485){
			if (gpioModeSwitch != null) {
				try {
					gpioModeSwitch.close();
				} catch (IOException e) {
					throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,e.getMessage());
				}
			}
		}
	}

	public int getConnectStatus() {
		return KuraConnectionStatus.CONNECTED;
	}

	private byte asciiLrcCalc(byte[] msg, int len){
		char[] ac=new char[2];
		ac[0]=(char) msg[len-4]; ac[1]=(char) msg[len - 3];
		String s=new String(ac);
		byte lrc=(byte) Integer.parseInt(s,16);
		return lrc;
	}
	
	private int binLrcCalc(byte[] msg){
		int llrc=0;
		for(int i=0; i<msg.length; i++) 
			llrc+=(int)msg[i] & 0xff;
		llrc = (llrc ^ 0xff) + 1;
		byte lrc=(byte)(llrc & 0x0ff);
		return llrc;
	}
	
	/**
	 * convertCommandToAscii: convert a binary command into a standard Modbus
	 * ASCII frame 
	 */
	private byte[] convertCommandToAscii(byte[] msg){
		int lrc=binLrcCalc(msg);
		
		char[] hexArray = "0123456789ABCDEF".toCharArray();
		byte[] ab=new byte[msg.length*2 + 5];
		ab[0]=':';
		int v;
		for(int i=0; i<msg.length; i++){
			v=msg[i] & 0xff;
			ab[i*2 + 1] = (byte) hexArray[v >>> 4];
			ab[i*2 + 2] = (byte) hexArray[v & 0x0f];
		}
		v=lrc & 0x0ff;
		ab[ab.length-4]=(byte) hexArray[v >>> 4];
		ab[ab.length-3]=(byte) hexArray[v & 0x0f];
		ab[ab.length-2]=13;
		ab[ab.length-1]=10;
		return ab;
	}

	/**
	 * convertAsciiResponseToBin: convert a standard Modbus frame to
	 * byte array 
	 */
	private byte[] convertAsciiResponseToBin(byte[] msg, int len){
		int l = (len-5)/2;
		byte[] ab=new byte[l];
		char[] ac=new char[2];
		String s=new String(msg);
		for(int i=0; i<l; i++){
			ac[0]=(char) msg[i*2 + 1]; ac[1]=(char) msg[i*2 + 2];
			s=new String(ac);
			ab[i]=(byte) Integer.parseInt(s,16);
		}
		return ab;
	}
	
	/**
	 * msgTransaction must be called with a byte array having two extra
	 * bytes for the CRC. It will return a byte array of the response to the
	 * message. Validation will include checking the CRC and verifying the
	 * command matches.
	 */
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
		else if(m_txMode == ModbusTransmissionMode.ASCII_MODE){
			cmd=convertCommandToAscii(msg);
		}

		// Send the message
		try {
			synchronized (out) {
				synchronized (in) {
					// flush input
					if(m_serial485)
						switchRX();
					while (in.available() > 0)
						in.read();
					// send all data
					if(m_serial485)
						switchTX();
					out.write(cmd, 0, cmd.length);
					out.flush();
					//outputStream.waitAllSent(respTout);

					// wait for and process response
					if(m_serial485)
						switchRX();
					byte[] response = new byte[262]; // response buffer
					int respIndex = 0;
					int minimumLength = 5; // default minimum message length
					if(m_txMode == ModbusTransmissionMode.ASCII_MODE)
						minimumLength = 11;
					int timeOut = m_respTimeout;
					for (int maxLoop = 0; maxLoop < 1000; maxLoop++) {
						boolean endFrame=false;
						//while (respIndex < minimumLength) {
						while (!endFrame) {
							long start = System.currentTimeMillis();
							while(in.available()==0) {								
								try {
									Thread.sleep(5);	// avoid a high cpu load
								} catch (InterruptedException e) {
									throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE, "Thread interrupted");
								}

								long elapsed = System.currentTimeMillis()-start;
								//if( elapsed > timeOut) {
								if( elapsed > 3000) {
									String failMsg = "Recv timeout";
									s_logger.warn(failMsg+" : "+elapsed+" minimumLength="+minimumLength+" respIndex="+respIndex);
									throw new ModbusProtocolException(ModbusProtocolErrorCode.RESPONSE_TIMEOUT, failMsg);
								}
							}
							// address byte must match first
							if (respIndex == 0) {
								if(m_txMode == ModbusTransmissionMode.ASCII_MODE){
									if ((response[0] = (byte) in.read()) == ':')
										respIndex++;
								}
								else{
									if ((response[0] = (byte) in.read()) == msg[0])
										respIndex++;
								}
							} else
								response[respIndex++] = (byte) in.read();

							if(m_txMode == ModbusTransmissionMode.RTU_MODE){
								timeOut = 100; // move to character timeout
								if(respIndex >= minimumLength)
									endFrame=true;
							}
							else{
								if((response[respIndex-1]==10)&&(response[respIndex-2]==13))
									endFrame=true;
							}
						}
						// if ASCII mode convert response
						if(m_txMode == ModbusTransmissionMode.ASCII_MODE){
							byte lrcRec = asciiLrcCalc(response,respIndex);
							response=convertAsciiResponseToBin(response,respIndex);
							byte lrcCalc = (byte)binLrcCalc(response);
							if(lrcRec!=lrcCalc)
								throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,"Bad LRC");
						}
						
						// Check first for an Exception response
						if ((response[1] & 0x80) == 0x80) {
							if ((m_txMode == ModbusTransmissionMode.ASCII_MODE)||(Crc16.getCrc16(response, 5, 0xffff) == 0))
								throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
										"Exception response = "+ Byte.toString(response[2]));
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
								else if ((m_txMode == ModbusTransmissionMode.ASCII_MODE)||
										(Crc16.getCrc16(response, 8, 0xffff) == 0)) {
									byte[] ret = new byte[6];
									for (int i = 0; i < 6; i++)
										ret[i] = response[i];
									return ret;
								}
								break;
							case ModbusFunctionCodes.READ_COIL_STATUS:
							case ModbusFunctionCodes.READ_INPUT_STATUS:
							case ModbusFunctionCodes.READ_INPUT_REGS:
							case ModbusFunctionCodes.READ_HOLDING_REGS:									
								int byteCnt;
								if ((m_txMode == ModbusTransmissionMode.ASCII_MODE))
									byteCnt= (response[2] & 0xff) + 3;
								else
									byteCnt= (response[2] & 0xff) + 5;
								if (respIndex < byteCnt)
									// wait for more data
									minimumLength = byteCnt;
								else if ((m_txMode == ModbusTransmissionMode.ASCII_MODE)||
										(Crc16.getCrc16(response, byteCnt,0xffff) == 0)) {
									byte[] ret = new byte[byteCnt];
									for (int i = 0; i < byteCnt; i++)
										ret[i] = response[i];
									return ret;
								}
							}
						}

						/*
						 * if required length then must have failed, drop
						 * first byte and try again
						 */
						if (respIndex >= minimumLength) {
							respIndex--;
							for (int i = 0; i < respIndex; i++)
								response[i] = response[i + 1];
							minimumLength = 5; // reset minimum length
						}
					}
				}
			}
		} catch (IOException e) {
			//e.printStackTrace();
			throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,e.getMessage());
		}
		throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
				"Too much activity on recv line");
	}


}
