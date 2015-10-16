package org.eclipse.kura.data.event;

import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

public class DataEventSupport implements EventHandler
{
    private DataEventSupporter m_eventSupporter;
    private BundleContext      m_bundleContext;
    private EventAdmin         m_eventAdmin;
    @SuppressWarnings("rawtypes")
    private Map<String,ServiceRegistration> m_subscriptions = null;
    
    @SuppressWarnings({ "rawtypes" })
    public DataEventSupport(DataEventSupporter eventSupporter)
    {
        m_subscriptions = new HashMap<String,ServiceRegistration>();
        
        // save the supporter
        this.m_eventSupporter = eventSupporter;
        this.m_bundleContext = FrameworkUtil.getBundle(eventSupporter.getClass()).getBundleContext();
    }
    
    public synchronized void emit(DataEvent dataEvent) 
    {
    	if (m_eventAdmin == null) {
            getEventAdmin();
    	}
    	m_eventAdmin.sendEvent(dataEvent);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public synchronized void subscribe(String emitterId) 
    {
        ServiceRegistration sr = m_subscriptions.get(emitterId); 
        if (sr == null) {
         
            // prepare the subscription
            String[] topics = new String[] { DataEventTopic.build(emitterId) };
            Dictionary props = new Hashtable();
            props.put(EventConstants.TIMESTAMP, new Date());
            props.put(EventConstants.EVENT_TOPIC, topics);
    
            // register the subscription and store it away
            sr = m_bundleContext.registerService(EventHandler.class.getName(), this, props);
            m_subscriptions.put(emitterId, sr);
        }
    }
    
    @SuppressWarnings({ "rawtypes" })
    public synchronized void unsubscribe(String emitterId) 
    {
        ServiceRegistration sr = m_subscriptions.get(emitterId); 
        if (sr != null) {
            sr.unregister();
            m_subscriptions.remove(emitterId);
        }
    }

    @SuppressWarnings({ "rawtypes" })
    public synchronized void unsubscribeAll() 
    {
        for (String emitterId : m_subscriptions.keySet()) {
            ServiceRegistration sr = m_subscriptions.get(emitterId); 
            if (sr != null) {
                sr.unregister();
            }            
        }
        m_subscriptions.clear();
    }
    
    public void handleEvent(Event event)
    {
        if (event instanceof DataEvent) {
            if (m_eventSupporter instanceof DataEventHandler) {
                ((DataEventHandler) m_eventSupporter).handleDataEvent((DataEvent) event);
            }
        }
    }
    
    
    // ----------------------------------------------------------------
    //
    //   Private methods
    //
    // ----------------------------------------------------------------

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void getEventAdmin() 
	{
		// get bundle instance via the OSGi Framework Util class
        ServiceReference ref = m_bundleContext.getServiceReference(EventAdmin.class);
        this.m_eventAdmin = (EventAdmin) m_bundleContext.getService(ref);
	}
}
