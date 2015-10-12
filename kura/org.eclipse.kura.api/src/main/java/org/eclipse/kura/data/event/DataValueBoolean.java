package org.eclipse.kura.data.event;

public class DataValueBoolean implements DataValue<Boolean>
{
    private boolean value;
    
    public DataValueBoolean(boolean value) {
        this.value = value;
    }
    
    public DataType getType() {
        return DataType.BOOLEAN;
    }

    public Boolean getValue() {
        return value;
    }
}
