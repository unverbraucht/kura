package org.eclipse.kura.wire;

@SuppressWarnings("rawtypes")
public class WireField 
{
    private String    name;
    private WireValue value;
    
    public WireField(String name,
                     WireValue value) {
        this.name  = name;
        this.value = value;
        
    }

    public String getName() {
        return name;
    }

    public WireValue getValue() {
        return value;
    }
}
