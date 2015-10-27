package org.eclipse.kura.wire;

public class WireValueInteger implements WireValue<Integer>
{
    private int value;
    
    public WireValueInteger(int value) {
        this.value = value;
    }
    
    public WireDataType getType() {
        return WireDataType.INTEGER;
    }

    public Integer getValue() {
        return value;
    }
}
