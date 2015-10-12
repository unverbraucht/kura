package org.eclipse.kura.data.event;

public class DataValueInteger implements DataValue<Integer>
{
    private int value;
    
    public DataValueInteger(int value) {
        this.value = value;
    }
    
    public DataType getType() {
        return DataType.INTEGER;
    }

    public Integer getValue() {
        return value;
    }
}
