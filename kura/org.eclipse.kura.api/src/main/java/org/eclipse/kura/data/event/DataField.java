package org.eclipse.kura.data.event;

@SuppressWarnings("rawtypes")
public class DataField 
{
    private String    name;
    private DataValue value;
    
    public DataField(String name,
                     DataValue value) {
        this.name  = name;
        this.value = value;
        
    }

    public String getName() {
        return name;
    }

    public DataValue getValue() {
        return value;
    }
}
