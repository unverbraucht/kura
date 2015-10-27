package org.eclipse.kura.wire;

public class WireValueRaw implements WireValue<byte[]>
{
    private byte[] value;
    
    public WireValueRaw(byte[] value) {
        this.value = value;
    }
    
    public WireDataType getType() {
        return WireDataType.RAW;
    }

    public byte[] getValue() {
        return value;
    }
}
