package org.jflickrsync.main.watch;

public interface EventProcessor
{

    /**
     * process the watch event
     * 
     * @param event {@link WatchEvent} to process
     */
    void process( WatchEvent event )
        throws Exception;
}
