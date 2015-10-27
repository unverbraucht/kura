package org.eclipse.kura.wire;

import java.util.List;

import org.osgi.service.wireadmin.BasicEnvelope;

public class WireEnvelope extends BasicEnvelope
{
	public WireEnvelope(String emitterPid, List<WireRecord> wireRecords)
	{
		super(wireRecords, emitterPid, null);
	}
	
	public String getEmitterPid() {
		return (String) getIdentification();
	}

	@SuppressWarnings("unchecked")
	public List<WireRecord> getRecords() {
		return (List<WireRecord>) getValue();
	}
}
