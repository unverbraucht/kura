package org.eclipse.kura.data.event;

public class DataValueByte implements DataValue<byte[]>
{
    private byte[] value;
    
    public DataValueByte(byte[] value) {
        this.value = value;
    }
    
    public DataType getType() {
        return DataType.RAW;
    }

    public byte[] getValue() {
        return value;
    }
}
