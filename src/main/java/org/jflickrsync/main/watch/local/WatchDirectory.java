package org.jflickrsync.main.watch.local;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

public class WatchDirectory
{

    @Subscribe
    @AllowConcurrentEvents
    public void handleEventConcurrent( PathEvents events )
    {
        System.out.println( "event concurrent!" );
    }

    /*
     * @Subscribe public void handleEventNonConcurrent( PathEvents events ) { System.out.println(
     * "event non concurrent!" ); }
     */
}
