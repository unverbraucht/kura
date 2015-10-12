package org.eclipse.kura.data.event;

public class DataValueFloat implements DataValue<Float>
{
    private float value;
    
    public DataValueFloat(float value) {
        this.value = value;
    }
    
    public DataType getType() {
        return DataType.FLOAT;
    }

    public Float getValue() {
        return value;
    }
}
