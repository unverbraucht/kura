package org.eclipse.kura.wire;

public class WireValueShort implements WireValue<Short>
{
    private short value;
    
    public WireValueShort(short value) {
        this.value = value;
    }
    
    public WireDataType getType() {
        return WireDataType.SHORT;
    }

    public Short getValue() {
        return value;
    }
}
