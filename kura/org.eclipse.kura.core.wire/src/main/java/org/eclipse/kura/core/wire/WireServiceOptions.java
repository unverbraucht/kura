package org.eclipse.kura.core.wire;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Immutable object to capture the configuration of the Wire graph.
 */
public class WireServiceOptions 
{
	public static final String CONF_WIRES = "wires";
	
	private List<WireConfiguration> m_wireConfigurations;
	
	private WireServiceOptions(List<WireConfiguration> confs) {	
		m_wireConfigurations = confs;
	}	
	
	
	public List<WireConfiguration> getWireConfigurations() {
		return m_wireConfigurations;
	}
	
	
	public static WireServiceOptions newInstance(Map<String,Object> properties) 
		throws JSONException 
	{
		List<WireConfiguration> wireConfs = new ArrayList<WireConfiguration>();
		Object objWires = properties.get(CONF_WIRES);
		if (objWires instanceof String) {

			String strWires = (String) objWires; 
			JSONArray jsonWires = new JSONArray(strWires);
			for (int i=0; i<jsonWires.length(); i++) {
				JSONObject jsonWire = jsonWires.getJSONObject(i); 				
				wireConfs.add(WireConfiguration.newInstanceFromJson(jsonWire));
			}
		}
		return new WireServiceOptions(wireConfs);
	}

	public List<WireConfiguration> getWires(){
		return m_wireConfigurations;
	}

	public String toJsonString() 
		throws JSONException 
	{
		JSONArray jsonWires = new JSONArray();
		for (WireConfiguration wireConfig : m_wireConfigurations) {
			jsonWires.put(wireConfig.toJson());
		}
		return jsonWires.toString();
	}
	
	
	public static void main(String[] argv) throws JSONException 
	{
		WireConfiguration wireConf = null;
		wireConf = new WireConfiguration("org.eclipse.kura.demo.heater.Heater",
										 "org.eclipse.kura.core.wire.cloud.publisher.CloudPublisher",
										 null);
		
		WireConfiguration wireConf1 = null;
		wireConf1 = new WireConfiguration("org.eclipse.kura.demo.heater.Heater",
				 						  "org.eclipse.kura.core.wire.cloud.publisher.CloudPublisher",
				 						  "(wirevalue.elapsed>=1500)");
		
		List<WireConfiguration> wireConfs = new ArrayList<WireConfiguration>();
		wireConfs.add(wireConf);
		wireConfs.add(wireConf1);
		
		WireServiceOptions wireOpts = new WireServiceOptions(wireConfs);
		String json = wireOpts.toJsonString();
		System.err.println(json);
		
		Map<String,Object> opts = new HashMap<String,Object>();
		opts.put("wires", json);
		WireServiceOptions wireOpts1 = WireServiceOptions.newInstance(opts);
		System.err.println(wireOpts1.toJsonString());		
	}
}
