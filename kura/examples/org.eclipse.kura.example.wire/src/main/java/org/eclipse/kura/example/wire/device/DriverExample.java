package org.eclipse.kura.example.wire.device;

import java.util.HashMap;
import java.util.Random;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.wire.DeviceConnection;

public class DriverExample implements DeviceConnection {

	private boolean m_connected = false;
	
	private HashMap<String, Object> m_values = new HashMap<String, Object>();
	
	private Random m_random = new Random();
	
	public DriverExample(){
		//Random values
		m_values.put("1", m_random.nextInt());
		m_values.put("2", m_random.nextDouble());
		m_values.put("3", String.valueOf(m_random.nextGaussian()));
	}
	
	@Override
	public void connect() throws KuraException {
		m_connected = true;
	}

	@Override
	public void disconnect() throws KuraException {
		m_connected = false;
	}

	@Override
	public boolean isConnected() {
		return m_connected;
	}

	@Override
	public void write(String address, Object value) throws KuraException {
		m_values.put(address, value);
	}

	@Override
	public Object read(String address) throws KuraException {
		if(address.equals("1")){
			m_values.put("1", m_random.nextInt());
		}
		if(address.equals("2")){
			m_values.put("2", m_random.nextDouble());
		}
		if(address.equals("3")){
			m_values.put("3", String.valueOf(m_random.nextGaussian()));
		}		
		return m_values.get(address);
	}

	@Override
	public String getDriverDescription() {
		return "Example dummy Driver";
	}

	@Override
	public String getAddressSintax() {
		return "Available addresses are \"1\", \"2\" and \"3\". /n"
				+ "Address 1 is a random Int; /n"
				+ "Address 2 is a random Double; /n"
				+ "Address 3 is a random String";
	}

	@Override
	public String getHelp() {
		return "No help text available";
	}

}
