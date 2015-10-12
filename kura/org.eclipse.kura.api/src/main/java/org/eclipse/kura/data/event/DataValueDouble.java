package org.eclipse.kura.data.event;

public class DataValueDouble implements DataValue<Double>
{
    private double value;
    
    public DataValueDouble(double value) {
        this.value = value;
    }
    
    public DataType getType() {
        return DataType.DOUBLE;
    }

    public Double getValue() {
        return value;
    }
}
