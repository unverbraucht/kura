package org.eclipse.kura.core.wire;

import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.json.JSONException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.wireadmin.Wire;
import org.osgi.service.wireadmin.WireAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WireService implements ConfigurableComponent
{
    private static final Logger s_logger = LoggerFactory.getLogger(WireService.class);

    private ComponentContext      m_ctx;

    private WireAdmin             m_wireAdmin;
    private ConfigurationService  m_configService;
    private WireServiceOptions    m_options;
    
    private String m_cloudPubPid;
    private String m_devExamplePid;
    private String m_devExamplePid2;
    private String m_modbusExamplePid;
    
    // ----------------------------------------------------------------
    //
    //   Dependencies
    //
    // ----------------------------------------------------------------

    public void setWireAdmin(WireAdmin wireAdmin) {
        this.m_wireAdmin = wireAdmin;
    }

    public void unsetWireAdmin(WireAdmin wireAdmin) {
        this.m_wireAdmin = null;
    }

    public WireAdmin getWireAdmin() {
        return m_wireAdmin;
    }

    public void setConfigurationService(ConfigurationService configService) {
        this.m_configService = configService;
    }

    public void unsetConfigurationService(ConfigurationService configService) {
        this.m_configService = null;
    }

    public ConfigurationService getConfigurationService() {
        return m_configService;
    }
    
    // ----------------------------------------------------------------
    //
    //   Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String,Object> properties) 
    	throws ComponentException
    {
        s_logger.info("activate...");

        // save the bundle context and the properties
        m_ctx = componentContext;        
    	try {
			m_options = WireServiceOptions.newInstance(properties);
		} 
    	catch (JSONException jsone) {
    		throw new ComponentException(jsone);
		}
        
        try { 
        	
 			String cloudPubFactPid = "org.eclipse.kura.core.wire.cloud.publisher.CloudPublisher";
 			ComponentConfiguration compConfig;
 			compConfig = m_configService.getComponentDefaultConfiguration(cloudPubFactPid);
 			m_cloudPubPid = m_configService.createComponent(cloudPubFactPid, compConfig.getConfigurationProperties());	        
 			s_logger.info("Created CloudPublisher instance with pid {}", m_cloudPubPid);

 			String dbStoreFactPid = "org.eclipse.kura.core.wire.store.DbWireRecordStore";
 			compConfig = m_configService.getComponentDefaultConfiguration(dbStoreFactPid);
 			String dbStorePubPid = m_configService.createComponent(dbStoreFactPid, compConfig.getConfigurationProperties());	        
 			s_logger.info("Created CloudPublisher instance with pid {}", dbStorePubPid);
 			
	        String heaterPid = "org.eclipse.kura.demo.heater.Heater";
	        Wire wire = m_wireAdmin.createWire(heaterPid, m_cloudPubPid, null);		        
	        s_logger.info("Created Wire between {} and {}.", heaterPid, m_cloudPubPid);
	        s_logger.info("Wire connected status: {}", wire.isConnected());
	        
	        Wire wire1 = m_wireAdmin.createWire(heaterPid, dbStorePubPid, null);		        
	        s_logger.info("Created Wire between {} and {}.", heaterPid, dbStorePubPid);
	        s_logger.info("Wire connected status: {}", wire1.isConnected());
	        	        
        	Wire[] wires = m_wireAdmin.getWires(null);
        	System.err.println("wires: "+wires);
 		} 
 		catch (Exception e2) {
 			// TODO Auto-generated catch block
 			e2.printStackTrace();
 		}

    }    	
    	
/*    	
    	
    	// rebuild the graph
    	resetGraph();
    }
    
    private synchronized void resetGraph() 
    {
    	// delete graph
    	Wire[] wires = m_wireAdmin.getWires(null);
    	for (Wire wire : wires) {
    		m_wireAdmin.deleteWire(wire);
    	}
    	
    	// build graph
    	for (WireConfiguration wireConf : m_options.getWireConfigurations()) {    	
    		createWire(wireConf);
    	}
    	buildGraph();
    }

		/*
		try {
			String deviceExampleFactPid = "org.eclipse.kura.example.wire.device.DeviceExample";

			ComponentConfiguration compConfig;
			compConfig = m_configService.getComponentDefaultConfiguration(deviceExampleFactPid);
			m_devExamplePid = m_configService.createComponent(deviceExampleFactPid, compConfig.getConfigurationProperties());	        
			s_logger.info("Created DeviceExample instance with pid {}", m_devExamplePid);
		} 
		catch (KuraException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		//Try a second one
		try {
			String deviceExampleFactPid = "org.eclipse.kura.example.wire.device.DeviceExample";
			ComponentConfiguration compConfig;
			compConfig = m_configService.getComponentDefaultConfiguration(deviceExampleFactPid);
			compConfig.getConfigurationProperties().put("sample.rate", 10);
			m_devExamplePid2 = m_configService.createComponent(deviceExampleFactPid, compConfig.getConfigurationProperties());	        
			s_logger.info("Created DeviceExample instance with pid {}", m_devExamplePid2);
		} 
		catch (KuraException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		*/
		

		Thread t = new Thread( new Runnable() {
			@Override
			public void run() {
				
				try {
					Thread.currentThread().sleep(10000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					String cloudPubFactPid = "org.eclipse.kura.core.wire.cloud.publisher.CloudPublisher";
					ComponentConfiguration compConfig;
					compConfig = m_configService.getComponentDefaultConfiguration(cloudPubFactPid);
					m_cloudPubPid = m_configService.createComponent(cloudPubFactPid, compConfig.getConfigurationProperties());	        
					s_logger.info("Created CloudPublisher instance with pid {}", m_cloudPubPid);
				} 
				catch (KuraException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}


				//Try a modbus one
				try {
					String modbusExampleFactPid = "org.eclipse.kura.example.wire.modbus.ModbusDevice";
					ComponentConfiguration compConfig;
					compConfig = m_configService.getComponentDefaultConfiguration(modbusExampleFactPid);
					m_modbusExamplePid = m_configService.createComponent(modbusExampleFactPid, compConfig.getConfigurationProperties());	        
					s_logger.info("Created ModbusExample instance with pid {}", m_modbusExamplePid);
				} 
				catch (KuraException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}

//		        String heaterPid = "org.eclipse.kura.demo.heater.Heater";
//		        try {
//		        	
//		        	ServiceReference[] refs = m_ctx.getBundleContext().getServiceReferences("org.osgi.service.wireadmin.Producer", null);
//		        	System.err.println("refs: "+refs);
//
//		        	refs = m_ctx.getBundleContext().getServiceReferences("org.osgi.service.wireadmin.Producer", "(" + Constants.SERVICE_PID + "=" + heaterPid + ")");
//		        	System.err.println("refs: "+refs);
//					
//		        	refs = m_ctx.getBundleContext().getServiceReferences(heaterPid, "(" + Constants.SERVICE_PID + "=" + heaterPid + ")");
//		        	System.err.println("refs: "+refs);
//		        	
//				} catch (InvalidSyntaxException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
					
		        // build a CloudPublisher
				
				/*
		        Wire wire = m_wireAdmin.createWire(devicePid, m_cloudPubPid, null);		        
		        s_logger.info("Created Wire between {} and {}.", devicePid, m_cloudPubPid);
		        s_logger.info("Wire connected status: {}", wire.isConnected());
		        
		        Wire wire2 = m_wireAdmin.createWire(m_devExamplePid2, m_cloudPubPid, null);		        
		        s_logger.info("Created Wire between {} and {}.", m_devExamplePid2, m_cloudPubPid);
		        s_logger.info("Wire connected status: {}", wire2.isConnected());
				*/
				
				/*
		        Wire wire3 = m_wireAdmin.createWire(m_modbusExamplePid, m_cloudPubPid, null);		        
		        s_logger.info("Created Wire between {} and {}.", m_modbusExamplePid, m_cloudPubPid);
		        s_logger.info("Wire connected status: {}", wire3.isConnected());

		        try {
					
		        	Wire[] wires = m_wireAdmin.getWires(null);
		        	System.err.println("wires: "+wires);
					
				} catch (InvalidSyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				*/
			}
        });
        t.start();
    }
*/

    public void updated(Map<String,Object> properties)
    {
        s_logger.info("updated...: " + properties);
    }

    protected void deactivate(ComponentContext componentContext) 
    {
        s_logger.info("deactivate...");
    }

    
    // ----------------------------------------------------------------
    //
    //   Kura Wire APIs
    //
    // ----------------------------------------------------------------
    
}
