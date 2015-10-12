package org.eclipse.kura.data.event;

public class DataValueString implements DataValue<String>
{
    private String value;
    
    public DataValueString(String value) {
        this.value = value;
    }
    
    public DataType getType() {
        return DataType.STRING;
    }

    public String getValue() {
        return value;
    }
}
