package org.eclipse.kura.data.event;

public interface DataValue<T>
{
    public DataType getType();

    public T getValue();
}
