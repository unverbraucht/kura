package org.eclipse.kura.example.wire.modbus;

import java.util.Map;
import java.util.Properties;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.device.Device;
import org.eclipse.kura.device.DeviceConnection;
import org.eclipse.kura.protocol.modbus.ModbusProtocolDeviceService;
import org.eclipse.kura.wire.WireField;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusDevice implements Device, ConfigurableComponent {

	private static final Logger s_logger = LoggerFactory.getLogger(ModbusDevice.class);
	private static final String PID = "Modbus Device";
	
	private WireSupport m_WireSupport;
	
	private ModbusConnection m_ModbusConnection;
	
	private Map<String, Object> m_properties;
	private int m_polling_interval;
	
	private ModbusProtocolDeviceService m_ModbusProtocolDeviceService;
	
	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------
	
	public void setModbusProtocolDeviceService(ModbusProtocolDeviceService modbusProtocolDeviceService){
		m_ModbusProtocolDeviceService = modbusProtocolDeviceService;
	}
	
	public void unsetModbusProtocolDeviceService(ModbusProtocolDeviceService modbusProtocolDeviceService){
		m_ModbusProtocolDeviceService = null;
	}
	
	public ModbusDevice(){
		m_WireSupport = new WireSupport(this);
	}

	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------
	
	protected void activate(ComponentContext componentContext, Map<String,Object> properties) 
	{
		s_logger.info("Activating Modbus Device...");
		m_properties = properties;
		doUpdate();
	}
	
	
	protected void deactivate(ComponentContext componentContext) 
	{
		s_logger.debug("Deactivating Modbus Device...");
	}	
	
	
	public void updated(Map<String,Object> properties)
	{
		s_logger.info("Updating Modbus Device...");
		m_properties = properties;
		doUpdate();	}
	
	// ----------------------------------------------------------------
	//
	//   WireEmitter APIs
	//
	// ----------------------------------------------------------------
	
	@Override
	public String getEmitterPid() {
		return PID;
	}

	@Override
	public void consumersConnected(Wire[] wires) {
		m_WireSupport.consumersConnected(wires);
	}

	@Override
	public Object polled(Wire wire) {
		return m_WireSupport.polled(wire);
	}

	// ----------------------------------------------------------------
	//
	//   Device APIs
	//
	// ----------------------------------------------------------------	

	@Override
	public WireRecord readDataRecord() throws KuraException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WireField readDataField(String name) throws KuraException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void writeDataField(WireField dataField) throws KuraException {
		// TODO Auto-generated method stub

	}

	@Override
	public DeviceConnection getDeviceConnection() {
		return m_ModbusConnection;
	}

	@Override
	public int getPollingInterval() {
		return m_polling_interval;
	}

	@Override
	public int getFetchMode() {
		// TODO Auto-generated method stub
		return 0;
	}

	private void connect() throws KuraException{
		if(!getDeviceConnection().isConnected()){
			getDeviceConnection().connect();
		}
	}
	
	// ----------------------------------------------------------------
	//
	//   Private methods
	//
	// ----------------------------------------------------------------	

	private void doUpdate(){
		
		getModbusProperties();
		
	}
	
	private Properties getModbusProperties() {
		Properties prop = new Properties();

		if(m_properties!=null){
			String portName = null;
			String serialMode = null;
			String baudRate = null;
			String bitsPerWord = null;
			String stopBits = null;
			String parity = null;
			String ptopic = null;
			String ctopic = null;
			String ipAddress = null;
			String pollInt= null;
			String pubInt= null;
			String Slave= null;
			String Mode= null;
			String timeout= null;
			if(m_properties.get("slaveAddr") != null) Slave 		= (String) m_properties.get("slaveAddr");
			if(m_properties.get("transmissionMode") != null) Mode	= (String) m_properties.get("transmissionMode");
			if(m_properties.get("respTimeout") != null) timeout		= (String) m_properties.get("respTimeout");
			if(m_properties.get("port") != null) portName 			= (String) m_properties.get("port");
			if(m_properties.get("serialMode") != null) serialMode 	= (String) m_properties.get("serialMode");
			if(m_properties.get("baudRate") != null) baudRate 		= (String) m_properties.get("baudRate");
			if(m_properties.get("bitsPerWord") != null) bitsPerWord = (String) m_properties.get("bitsPerWord");
			if(m_properties.get("stopBits") != null) stopBits 		= (String) m_properties.get("stopBits");
			if(m_properties.get("parity") != null) parity 			= (String) m_properties.get("parity");
			if(m_properties.get("publishTopic") != null) ptopic		= (String) m_properties.get("publishTopic");
			if(m_properties.get("controlTopic") != null) ctopic		= (String) m_properties.get("controlTopic");
			if(m_properties.get("ipAddress") != null) ipAddress		= (String) m_properties.get("ipAddress");
			if(m_properties.get("pollInterval") != null) pollInt	= (String) m_properties.get("pollInterval");
			if(m_properties.get("publishInterval") != null) pubInt	= (String) m_properties.get("publishInterval");
			
			if(portName==null) //portName="/dev/ttyUSB0";
				return null;		
			if(baudRate==null) baudRate="9600";
			if(stopBits==null) stopBits="1";
			if(parity==null) parity="0";
			if(bitsPerWord==null) bitsPerWord="8";
			if(Slave==null) Slave="1";
			if(Mode==null) Mode="RTU";
			if(timeout==null) timeout="1000";
			if(ptopic==null) ptopic="eurotech/demo";
			if(ctopic==null) ctopic="eurotech/demo";
			if(pollInt==null) pollInt="500";
			if(pubInt==null) pubInt="180";
			
			if(serialMode!=null) {
				if(serialMode.equalsIgnoreCase("RS232") || serialMode.equalsIgnoreCase("RS485")) {
					prop.setProperty("connectionType", "SERIAL");
					prop.setProperty("serialMode", serialMode);
					prop.setProperty("port", portName);
					prop.setProperty("exclusive", "false");
					prop.setProperty("mode", "0");
					prop.setProperty("baudRate", baudRate);
					prop.setProperty("stopBits", stopBits);
					prop.setProperty("parity", parity);
					prop.setProperty("bitsPerWord", bitsPerWord);
				} else {
					prop.setProperty("connectionType", "ETHERTCP");
					prop.setProperty("ipAddress", ipAddress);
					prop.setProperty("port", portName);
				}
			}
			prop.setProperty("slaveAddr", Slave);
			prop.setProperty("transmissionMode", Mode);
			prop.setProperty("respTimeout", timeout);
			prop.setProperty("publishTopic", ptopic);
			prop.setProperty("controlTopic", ctopic);
			prop.setProperty("pollInterval", pollInt);
			prop.setProperty("publishInterval", pubInt);

			return prop;
		} else {
			return null;
		}
	}

}
