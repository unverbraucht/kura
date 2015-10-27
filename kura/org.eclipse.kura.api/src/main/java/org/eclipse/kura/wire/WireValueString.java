package org.eclipse.kura.wire;

public class WireValueString implements WireValue<String>
{
    private String value;
    
    public WireValueString(String value) {
        this.value = value;
    }
    
    public WireDataType getType() {
        return WireDataType.STRING;
    }

    public String getValue() {
        return value;
    }
}
