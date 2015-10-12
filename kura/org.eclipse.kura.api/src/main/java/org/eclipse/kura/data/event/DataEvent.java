package org.eclipse.kura.data.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.osgi.service.event.Event;

public class DataEvent extends Event
{
    private String emitterId;
    private List<DataRecord> records = null;
    
    public DataEvent(String emitterId,
                     List<DataRecord> records)
    {
        super(DataEventTopic.build(emitterId), new HashMap<String,Object>());
    
        this.emitterId = emitterId; 
        this.records   = Collections.unmodifiableList(records);
    }
    
    public String getEmitterId() {
        return emitterId;
    }
    
    public List<DataRecord> getRecords() {
        return records;
    }
}