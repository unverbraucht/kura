package org.eclipse.kura.protocol.modbus2.api;

import java.util.Map;

public class ModbusProtocolProperties {
   
	private static final String PROP_INSTANCE_NAME="instanceName";
	private static final String PROP_SERIAL_MODE="serialMode";
	private static final String PROP_PORT="port";
	private static final String PROP_IP_ADDRESS="ipAddress";
	private static final String PROP_BAUD_RATE="baudRate";
	private static final String PROP_BITS_PER_WORD="bitsPerWord";
	private static final String PROP_STOP_BITS="stopBits";
	private static final String PROP_PARITY="parity";
	
	public static final String MODE_RS232 = "RS232";
	public static final String MODE_RS485 = "RS485";
	public static final String MODE_RCP = "ETHERTCP";
	
	public static final String BAUDRATE_110 = "110";
	public static final String BAUDRATE_300 = "300";
	public static final String BAUDRATE_600 = "600";
	public static final String BAUDRATE_1200 = "1200";
	public static final String BAUDRATE_2400 = "2400";
	public static final String BAUDRATE_4800 = "4800";
	public static final String BAUDRATE_9600 = "9600";
	public static final String BAUDRATE_14400 = "14400";
	public static final String BAUDRATE_19200 = "19200";
	public static final String BAUDRATE_28800 = "28800";
	public static final String BAUDRATE_38400 = "38400";
	public static final String BAUDRATE_56000 = "56000";
	public static final String BAUDRATE_57600 = "57600";
	public static final String BAUDRATE_115200 = "115200";
	public static final String BAUDRATE_128000 = "128000";
	public static final String BAUDRATE_153600 = "153600";
	public static final String BAUDRATE_230400 = "230400";
	public static final String BAUDRATE_256000 = "256000";
	public static final String BAUDRATE_460800 = "460800";
	public static final String BAUDRATE_921600 = "921600";

	public static final String BITS_PER_WORD_5 = "5";
	public static final String BITS_PER_WORD_6 = "6";
	public static final String BITS_PER_WORD_7 = "7";
	public static final String BITS_PER_WORD_8 = "8";

	public static final String STOP_BITS_1 = "1";
	public static final String STOP_BITS_2 = "2";
	public static final String STOP_BITS_1_5 = "3";

	public static final String PARITY_NONE = "0";
	public static final String PARITY_ODD = "1";
	public static final String PARITY_EVEN = "2";
	public static final String PARITY_MARK = "3";
	public static final String PARITY_SPACE = "4";

	private String instanceName = "instance_0";
	private String serialMode = "RS232";
	private String port = "ttyUSB0";
	private String ipAddress = "";            
	private String baudRate = "115200";
    private String bitsPerWord = "8";
    private String stopBits = "1";            
    private String parity = "0";
    
    public ModbusProtocolProperties(Map<String, Object> props){
    	try{
    		instanceName = props.get(PROP_INSTANCE_NAME).toString();
    		serialMode = props.get(PROP_SERIAL_MODE).toString();
    		port = props.get(PROP_PORT).toString();
    		ipAddress = props.get(PROP_IP_ADDRESS).toString();
    		baudRate = props.get(PROP_BAUD_RATE).toString();
    		bitsPerWord = props.get(PROP_BITS_PER_WORD).toString();
    		stopBits = props.get(PROP_STOP_BITS).toString();
    		parity = props.get(PROP_PARITY).toString();
    		
    	}catch(Exception ex){
    		
    	}
    }
    
	public void setPort(String port) {
		this.port = port;
	}

	public void setBaudRate(String baudRate) {
		this.baudRate = baudRate;
	}

	public String getInstanceName() {
		return instanceName;
	}
	public String getSerialMode() {
		return serialMode;
	}
	public String getPort() {
		return port;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public String getBaudRate() {
		return baudRate;
	}
	public String getBitsPerWord() {
		return bitsPerWord;
	}
	public String getStopBits() {
		return stopBits;
	}
	public String getParity() {
		return parity;
	}
    
    
}
