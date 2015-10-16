package org.eclipse.kura.data.event;

public class DataValueLong implements DataValue<Long>
{
    private long value;
    
    public DataValueLong(long value) {
        this.value = value;
    }
    
    public DataType getType() {
        return DataType.LONG;
    }

    public Long getValue() {
        return value;
    }
}
