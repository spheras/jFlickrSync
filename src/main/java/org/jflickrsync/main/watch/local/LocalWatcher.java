package org.jflickrsync.main.watch.local;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent.Kind;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.jflickrsync.main.Configuration;
import org.jflickrsync.main.index.Index;
import org.jflickrsync.main.watch.WatchEvent;
import org.jflickrsync.main.watch.WatchEventType;
import org.jflickrsync.main.watch.WatchMonitor;
import org.jflickrsync.main.watch.Watcher;

import com.flickr4java.flickr.Flickr;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.Subscribe;

public class LocalWatcher
    implements Watcher
{
    private static final Logger logger = Logger.getLogger( LocalWatcher.class );

    /**
     * Monitor of these local events
     */
    private WatchMonitor monitor;

    /**
     * Processor of events
     */
    private LocalEventProcessor processor;

    /**
     * Directory watcher
     */
    private DirectoryEventWatcherImpl watcher;

    public LocalWatcher( WatchMonitor monitor, Index localCurrentIndex, Index serverCurrentIndex, Flickr flickr )
    {
        this.monitor = monitor;
        this.processor = new LocalEventProcessor( localCurrentIndex, serverCurrentIndex, flickr, this );

    }

    /**
     * Watch the base folder for changes in the content
     * 
     * @throws IOException
     */
    @Override
    public void start()
        throws IOException
    {
        AsyncEventBus eb = new AsyncEventBus( Executors.newCachedThreadPool() );
        eb.register( this );
        Path path = Paths.get( Configuration.getBasePath() );
        this.watcher = new DirectoryEventWatcherImpl( eb, path );
        this.watcher.start();
    }

    /**
     * List of {@link File} to bypass when receiving a delete event
     */
    private List<File> bypassDeleteFileEvents = new ArrayList<>();

    /**
     * Add a new bypass delete event. If a new incoming delete event is received with this absolute path file, the it
     * will be bypassed
     * 
     * @param f {@link File} file object with the absolute path to bypass
     */
    public void addByPassDeleteEvent( File f )
    {
        this.bypassDeleteFileEvents.add( f );
    }

    @Subscribe
    @AllowConcurrentEvents
    public synchronized void incomingEvent( PathEvents pathEvents )
    {
        List<WatchEvent> wevents = new ArrayList<>();

        List<PathEvent> events = pathEvents.getEvents();

        for ( int i = 0; i < events.size(); i++ )
        {
            PathEvent pevent = events.get( i );
            WatchEvent wevent = new WatchEvent();
            wevent.setEvent( pevent );
            wevent.setProcessor( this.processor );
            Kind<?> type = pevent.getType();
            if ( type == ENTRY_CREATE )
            {
                wevent.setType( WatchEventType.CREATE );
            }
            else if ( type == ENTRY_MODIFY )
            {
                wevent.setType( WatchEventType.MODIFY );
            }
            else if ( type == ENTRY_DELETE )
            {
                String absPath = "" + pevent.getDir() + File.separatorChar + pevent.getEventTarget();
                if ( bypassDeleteFileEvents.remove( new File( absPath ) ) )
                {
                    continue;
                }
                wevent.setType( WatchEventType.DELETE );
            }
            else if ( type == OVERFLOW )
            {
                wevent.setType( WatchEventType.OVERFLOW );
            }
            else
            {
                logger.error( "LOCAL TYPE UNKNONWN!" );
            }

            wevents.add( wevent );
        }

        this.monitor.addEvents( wevents );
    }

    /**
     * Watch a new subdirectory. Add the subdirectory to the list of directories watched
     * 
     * @param absolutePath {@link String} absolute path of the directory to be watched
     * @throws IOException
     */
    public void watchSubdirectory( String absolutePath )
        throws IOException
    {
        this.watcher.watchSubdirectory( absolutePath );
    }

}
