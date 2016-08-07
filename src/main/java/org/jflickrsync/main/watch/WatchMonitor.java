package org.jflickrsync.main.watch;

import java.util.ArrayList;
import java.util.List;

import org.jflickrsync.main.index.Index;
import org.jflickrsync.main.watch.local.LocalWatcher;
import org.jflickrsync.main.watch.server.ServerWatcher;

import com.flickr4java.flickr.Flickr;

public class WatchMonitor
{
    /**
     * Local watcher
     */
    private Watcher localWatcher;

    /**
     * Server watcher
     */
    private Watcher serverWatcher;

    /**
     * Pending events to be processed
     */
    private List<WatchEvent> pendingEvents = new ArrayList<WatchEvent>();

    /**
     * Start to watch events from local and server watchers
     * 
     * @throws Exception
     */
    public void start( Index localCurrentIndex, Index serverCurrentIndex, Flickr flickr )
        throws Exception
    {
        // starting local watcher
        this.localWatcher = new LocalWatcher( this, localCurrentIndex, serverCurrentIndex, flickr );
        this.localWatcher.start();

        // starting server watcher
        this.serverWatcher =
            new ServerWatcher( this, localCurrentIndex, serverCurrentIndex, flickr, (LocalWatcher) localWatcher );
        this.serverWatcher.start();

        // waiting events
        this.waitEvents();
    }

    /**
     * Get a semaphore for synchronized events
     * 
     * @return {@link Object} the semaphore
     */
    public synchronized Object getSemaphore()
    {
        return this;
    }

    /**
     * Add events to the monitor. This events will be processed later in a synced way.
     * 
     * @param events
     */
    public synchronized void addEvents( List<WatchEvent> events )
    {
        this.pendingEvents.addAll( events );
        this.notify();
    }

    /**
     * Unfortunatelly it seems we cannot use the Flickr4Java in separate threads (probably authentication)
     * 
     * @throws Exception
     */
    private synchronized void waitEvents()
        throws Exception
    {
        while ( true )
        {
            this.wait();
            for ( int i = 0; i < this.pendingEvents.size(); i++ )
            {
                WatchEvent event = this.pendingEvents.get( i );
                if ( event.isServerPoll() )
                {
                    // sorry for this
                    ( (ServerWatcher) this.serverWatcher ).pollServer();
                }
                else
                {
                    event.getProcessor().process( event );
                }
            }
            this.pendingEvents.clear();
        }
    }

}
