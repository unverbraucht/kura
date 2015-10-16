package org.eclipse.kura.data.event;

public class DataValueRaw implements DataValue<byte[]>
{
    private byte[] value;
    
    public DataValueRaw(byte[] value) {
        this.value = value;
    }
    
    public DataType getType() {
        return DataType.RAW;
    }

    public byte[] getValue() {
        return value;
    }
}
