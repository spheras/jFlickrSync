package org.jflickrsync.main.watch;

public enum WatchEventType
{
    /** a new element has been created */
    CREATE,
    /** an existing element has been deleted */
    DELETE,

    /** and existing element has been modified */
    MODIFY,
    /** overflow of events, the system cannot process all the incoming events */
    OVERFLOW

}
