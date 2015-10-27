package org.eclipse.kura.wire;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.osgi.util.position.Position;

public class WireRecord 
{
    private Date            timestamp;
    private Position        position;
    private List<WireField> fields;
    
    public WireRecord(WireField... dataFields) {
    	this.timestamp = new Date();
    	this.position  = null;
    	this.fields    = Collections.unmodifiableList(Arrays.asList(dataFields));
    }

    public WireRecord(Date timestamp, 
                      List<WireField> fields) {
        this.timestamp = timestamp;
        this.position  = null;
        this.fields    = Collections.unmodifiableList(fields);
    }

    public WireRecord(Date timestamp, 
                      Position position, 
                      List<WireField> fields) {
        this.timestamp = timestamp;
        this.position  = position;
        this.fields    = Collections.unmodifiableList(fields);
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public Position getPosition() {
        return position;
    }

    public List<WireField> getFields() {
        return fields;
    }
}
