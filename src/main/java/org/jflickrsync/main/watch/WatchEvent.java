package org.jflickrsync.main.watch;

import lombok.Data;

@Data
public class WatchEvent
{
    /**
     * Type of event
     */
    private WatchEventType type;

    /**
     * Processor of this kind of event
     */
    private EventProcessor processor;

    /**
     * Concrete event to be processed to the processor
     */
    private Object event;

    /**
     * well... this is a cheat to ensure that the flickr api is called in the same thread as the authentication. We
     * cannot call the api in other threads.... mmm sorry for this.
     */
    private boolean serverPoll;

}
