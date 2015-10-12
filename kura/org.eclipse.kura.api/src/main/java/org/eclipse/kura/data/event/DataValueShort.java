package org.eclipse.kura.data.event;

public class DataValueShort implements DataValue<Short>
{
    private short value;
    
    public DataValueShort(short value) {
        this.value = value;
    }
    
    public DataType getType() {
        return DataType.SHORT;
    }

    public Short getValue() {
        return value;
    }
}
