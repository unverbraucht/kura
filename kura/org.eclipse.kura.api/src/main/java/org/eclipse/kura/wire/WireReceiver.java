package org.eclipse.kura.wire;

import org.osgi.service.wireadmin.Consumer;


public interface WireReceiver extends WireSupporter, Consumer
{
    public void onWireReceive(WireEnvelope wireEvelope);
}
