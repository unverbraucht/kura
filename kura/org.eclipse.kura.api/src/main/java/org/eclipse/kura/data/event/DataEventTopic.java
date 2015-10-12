package org.eclipse.kura.data.event;

import java.text.MessageFormat;

public class DataEventTopic {
    
    private static final String s_topicPattern = "org/eclipse/kura/data/event/emitter/{0}/UPDATE";

    public static String build(String emitterId) {
        return MessageFormat.format(s_topicPattern, emitterId);
    }

}
