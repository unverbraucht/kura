package org.eclipse.kura.wire;

public class WireValueByte implements WireValue<Byte>
{
    private byte value;
    
    public WireValueByte(byte value) {
        this.value = value;
    }
    
    public WireDataType getType() {
        return WireDataType.BYTE;
    }

    public Byte getValue() {
        return value;
    }
}
