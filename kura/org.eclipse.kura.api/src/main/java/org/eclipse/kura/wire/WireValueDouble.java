package org.eclipse.kura.wire;

public class WireValueDouble implements WireValue<Double>
{
    private double value;
    
    public WireValueDouble(double value) {
        this.value = value;
    }
    
    public WireDataType getType() {
        return WireDataType.DOUBLE;
    }

    public Double getValue() {
        return value;
    }
}
