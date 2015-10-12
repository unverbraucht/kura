package org.eclipse.kura.data.event;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.osgi.util.position.Position;

public class DataRecord 
{
    private Date            timestamp;
    private Position        position;
    private List<DataField> fields;
    
    public DataRecord(Date timestamp, 
                      List<DataField> fields) {
        this.timestamp = timestamp;
        this.position  = null;
        this.fields    = Collections.unmodifiableList(fields);
    }

    public DataRecord(Date timestamp, 
                      Position position, 
                      List<DataField> fields) {
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

    public List<DataField> getFields() {
        return fields;
    }
}
