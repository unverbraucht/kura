package org.eclipse.kura.wire;

public class WireValueLong implements WireValue<Long>
{
    private long value;
    
    public WireValueLong(long value) {
        this.value = value;
    }
    
    public WireDataType getType() {
        return WireDataType.LONG;
    }

    public Long getValue() {
        return value;
    }
}
