package org.eclipse.kura.core.wire;

import org.json.JSONException;
import org.json.JSONObject;

public class WireConfiguration 
{	
	private String m_producerPid;
	private String m_consumerPid;
	private String m_filter;
	
	WireConfiguration(String producerPid, 
					  String consumerPid,
					  String filter) {
		super();
		this.m_producerPid = producerPid;
		this.m_consumerPid = consumerPid;
		this.m_filter      = filter;
	}

	public static WireConfiguration newInstanceFromJson(JSONObject jsonWire) 
		throws JSONException 
	{
		String producer = jsonWire.getString("p");
		String consumer = jsonWire.getString("c");
		String filter   = jsonWire.optString("f");		
		return new WireConfiguration(producer, consumer, filter);
	}	

	public JSONObject toJson() 
		throws JSONException
	{
		JSONObject jsonWire = new JSONObject();
		jsonWire.put("p", m_producerPid);
		jsonWire.put("c", m_consumerPid);
		if (m_filter != null && !m_filter.isEmpty()) {
			jsonWire.putOpt("f", m_filter);
		}
		return jsonWire;
	}
	
	public String getProducerPid() {
		return m_producerPid;
	}

	public String getConsumerPid() {
		return m_consumerPid;
	}

	public String getFilter() {
		return m_filter;
	}
}
