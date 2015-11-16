package org.eclipse.kura.example.wire.device;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.device.Device;
import org.eclipse.kura.device.DeviceConnection;
import org.eclipse.kura.wire.WireField;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.eclipse.kura.wire.WireValueDouble;
import org.eclipse.kura.wire.WireValueInteger;
import org.eclipse.kura.wire.WireValueString;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceExample implements Device, ConfigurableComponent {

	private static final Logger s_logger = LoggerFactory
			.getLogger(DeviceExample.class);

	private DriverExample m_device_driver = new DriverExample();
	private static final String PID = "Dirver example";

	private HashMap<String, String> mapping = new HashMap<String, String>();

	private static final String FIELD_1_NAME = "Integer data";
	private static final String FIELD_2_NAME = "Double data";
	private static final String FIELD_3_NAME = "String data";

	private static final String PROP_SAMPLING_RATE = "sample.rate";

	private WireSupport m_wireSupport;

	private Future<?> m_pollingHandle;
	private ExecutorService m_pollingExecutor;

	private Map<String, Object> m_properties;

	private Runnable m_runnable = new Runnable() {
		@Override
		public void run() {
			try {
				while (true) {
					int pi = getPollingInterval();
					if (pi != -1) {
						Thread.sleep(pi);

						try {
							WireRecord wr = readDataRecord();
							s_logger.debug("Wire record for Example Device read.");
							m_wireSupport.emit(wr);
						} catch (Exception ex) {
							s_logger.error(
									"Error reading data record from device!",
									ex);
						}
					} else {
						Thread.sleep(1000);
					}
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Throwable t) {
				s_logger.error("SOMETHING WENT WRONG!", t);
			}
		}
	};

	public DeviceExample() {
		m_wireSupport = new WireSupport(this);
		mapping.put("1", FIELD_1_NAME);
		mapping.put("2", FIELD_2_NAME);
		mapping.put("3", FIELD_3_NAME);

		m_pollingExecutor = Executors.newSingleThreadExecutor();
	}

	protected void activate(ComponentContext ctx, Map<String, Object> properties) {
		s_logger.info("Activating example device...");
		m_properties = properties;

		m_pollingHandle = m_pollingExecutor.submit(m_runnable);

		doUpdate();
	}

	protected void deactivate(ComponentContext ctx) {
		s_logger.info("Deactivating example device...");
		if (m_pollingHandle != null) {
			m_pollingHandle.cancel(true);
		}

		m_pollingExecutor.shutdown();
	}

	protected void updated(Map<String, Object> properties) {
		s_logger.info("Updating example device...");
		m_properties = properties;

		doUpdate();
	}

	private void doUpdate() {

	}

	@Override
	public String getEmitterPid() {
		return PID;
	}

	@Override
	public void consumersConnected(Wire[] wires) {
		m_wireSupport.consumersConnected(wires);
	}

	@Override
	public Object polled(Wire wire) {
		return m_wireSupport.polled(wire);
	}

	@Override
	public WireRecord readDataRecord() throws KuraException {
		WireRecord result = new WireRecord(readDataField(FIELD_1_NAME),
				readDataField(FIELD_2_NAME), readDataField(FIELD_3_NAME));
		return result;
	}

	private String getAddressForName(String name) {
		for (String address : mapping.keySet()) {
			if (name.equals(mapping.get(address))) {
				return address;
			}
		}
		return null;
	}

	@Override
	public WireField readDataField(String name) throws KuraException {
		String address = getAddressForName(name);
		if (address != null) {
			Object data = m_device_driver.read(address);
			if (data != null) {
				if (name.equals(FIELD_1_NAME)) {
					return new WireField(name, new WireValueInteger(
							(Integer) data));
				}
				if (name.equals(FIELD_2_NAME)) {
					return new WireField(name, new WireValueDouble(
							(Double) data));
				}
				if (name.equals(FIELD_3_NAME)) {
					return new WireField(name, new WireValueString(
							(String) data));
				}
			}
		}

		throw new KuraException(KuraErrorCode.INTERNAL_ERROR);

	}

	@Override
	public void writeDataField(WireField dataField) throws KuraException {
		String address = getAddressForName(dataField.getName());
		if (address != null) {
			if (dataField.getName().equals(FIELD_1_NAME)
					|| dataField.getName().equals(FIELD_2_NAME)
					|| dataField.getName().equals(FIELD_3_NAME)) {
				m_device_driver.write(address, dataField.getValue().getValue());
			}
		}

		throw new KuraException(KuraErrorCode.INTERNAL_ERROR);

	}

	@Override
	public DeviceConnection getDeviceConnection() {
		return m_device_driver;
	}

	@Override
	public int getPollingInterval() {
		try {
			return (Integer) m_properties.get(PROP_SAMPLING_RATE) * 1000;
		} catch (NullPointerException ex) {
			return -1;
		}
	}

	@Override
	public int getFetchMode() {
		return 0;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if (m_pollingHandle != null) {
			m_pollingHandle.cancel(true);
		}
		m_pollingExecutor.shutdown();
	}

}
