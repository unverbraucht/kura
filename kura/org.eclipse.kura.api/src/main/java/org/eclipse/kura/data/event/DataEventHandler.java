package org.eclipse.kura.data.event;

public interface DataEventHandler extends DataEventSupporter
{
    public void handleDataEvent(DataEvent dataEvent);
}
