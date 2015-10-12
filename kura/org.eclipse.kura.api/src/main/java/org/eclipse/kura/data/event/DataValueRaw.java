package org.eclipse.kura.data.event;

public class DataValueRaw implements DataValue<Byte>
{
    private byte value;
    
    public DataValueRaw(byte value) {
        this.value = value;
    }
    
    public DataType getType() {
        return DataType.BYTE;
    }

    public Byte getValue() {
        return value;
    }
}
