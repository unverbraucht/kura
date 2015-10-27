package org.eclipse.kura.wire;

import org.osgi.service.wireadmin.Producer;

public interface WireEmitter extends WireSupporter, Producer
{
    public String getEmitterPid();
}
