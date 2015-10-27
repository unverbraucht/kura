package org.eclipse.kura.wire;

public class WireValueBoolean implements WireValue<Boolean>
{
    private boolean value;
    
    public WireValueBoolean(boolean value) {
        this.value = value;
    }
    
    public WireDataType getType() {
        return WireDataType.BOOLEAN;
    }

    public Boolean getValue() {
        return value;
    }
}
