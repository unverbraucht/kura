package org.eclipse.kura.wire;

public interface WireValue<T>
{
    public WireDataType getType();

    public T getValue();
}
