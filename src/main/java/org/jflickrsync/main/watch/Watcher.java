package org.jflickrsync.main.watch;

public interface Watcher
{

    /**
     * Start watching in a separate thread
     */
    void start()
        throws Exception;

}
