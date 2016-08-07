package org.jflickrsync.main.watch.server;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jflickrsync.main.Configuration;
import org.jflickrsync.main.index.Index;
import org.jflickrsync.main.index.album.Album;
import org.jflickrsync.main.index.album.AlbumFlickr;
import org.jflickrsync.main.index.photo.Photo;
import org.jflickrsync.main.index.photo.PhotoFlickr;
import org.jflickrsync.main.watch.WatchEvent;
import org.jflickrsync.main.watch.WatchEventType;
import org.jflickrsync.main.watch.WatchMonitor;
import org.jflickrsync.main.watch.Watcher;
import org.jflickrsync.main.watch.local.LocalWatcher;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;

public class ServerWatcher
    implements Watcher
{
    private static final Logger logger = Logger.getLogger( ServerWatcher.class );

    /**
     * Monitor of the server events
     */
    private WatchMonitor monitor;

    /**
     * This is the current local index at memory
     */
    private Index localCurrentIndex;

    /**
     * This is the current server index at memory
     */
    private Index serverCurrentIndex;

    /**
     * Sleep time, milliseconds, to poll the server. This info is taken from configuration
     */
    private long SLEEP_TIME = 1000 * 30000;

    /**
     * Flickr object
     */
    private Flickr flickr;

    /**
     * Processor of events
     */
    private ServerEventProcessor processor;

    public ServerWatcher( WatchMonitor monitor, Index localCurrentIndex, Index serverCurrentIndex, Flickr flickr,
                          LocalWatcher local )
    {
        this.monitor = monitor;
        this.localCurrentIndex = localCurrentIndex;
        this.serverCurrentIndex = serverCurrentIndex;
        this.flickr = flickr;
        this.SLEEP_TIME = Configuration.getServerPollMillisecons();
        this.processor = new ServerEventProcessor( localCurrentIndex, serverCurrentIndex, flickr, local );
    }

    @Override
    public void start()
        throws Exception
    {

        // we start a new thread to watch server events
        Thread t = new Thread( "SERVER_WATCHER" )
        {
            public void run()
            {
                while ( true )
                {
                    try
                    {
                        Thread.sleep( SLEEP_TIME );

                        if ( processor.getPendingEvents() != null && processor.getPendingEvents().size() > 0 )
                        {
                            // continue sleeping
                        }
                        else
                        {
                            // sorry for this, but we need to use the flickr api in the same thread as authentication
                            // ????
                            WatchEvent we = new WatchEvent();
                            we.setServerPoll( true );
                            List<WatchEvent> events = new ArrayList<>();
                            events.add( we );
                            monitor.addEvents( events );
                        }
                    }
                    catch ( InterruptedException e )
                    {
                        e.printStackTrace();
                    }
                }
            }
        };
        t.start();
    }

    /**
     * This method poll the server to see diferences. Unfortunatelly the activity api is restricted to only once per
     * hour
     * 
     * @throws FlickrException
     */
    public synchronized void pollServer()
        throws FlickrException
    {
        synchronized ( this.monitor.getSemaphore() )
        {
            List<WatchEvent> events = new ArrayList<>();

            // we refresh de index
            Index serverJustNowIndex = new Index();
            serverJustNowIndex.indexFlickrServer( this.flickr );
            // Map<String, PhotoFlickr> addedPhotos = new HashMap<>();

            // 1st, photos -not in set- in server but not at local
            for ( int i = 0; i < serverJustNowIndex.photoNoInAlbums_size(); i++ )
            {
                PhotoFlickr serverPhoto = (PhotoFlickr) serverJustNowIndex.photoNoInAlbums_get( i );
                int index = this.serverCurrentIndex.photoNoInAlbums_indexOf( serverPhoto );
                if ( index < 0 )
                {
                    // photo not found!! we need to create a new event
                    WatchEvent wevent = new WatchEvent();
                    wevent.setType( WatchEventType.CREATE );
                    wevent.setProcessor( this.processor );
                    wevent.setEvent( new ServerEvent( WatchEventType.CREATE, serverPhoto.getFlickrPhoto(), null,
                                                      serverPhoto.getId() ) );
                    events.add( wevent );
                }
            }

            // 2nd, albums in server but not at local
            for ( int i = 0; i < serverJustNowIndex.albums_size(); i++ )
            {
                AlbumFlickr serverAlbum = (AlbumFlickr) serverJustNowIndex.albums_get( i );
                int index = this.serverCurrentIndex.albums_indexOf( serverAlbum );
                if ( index < 0 )
                {
                    // album not found!! we need to create a new event
                    WatchEvent wevent = new WatchEvent();
                    wevent.setType( WatchEventType.CREATE );
                    wevent.setProcessor( this.processor );
                    wevent.setEvent( new ServerEvent( WatchEventType.CREATE, null, serverAlbum.getFlickrSet(),
                                                      serverAlbum.getFlickrSet().getId() ) );
                    events.add( wevent );
                }
                else
                {
                    Album album = this.serverCurrentIndex.albums_get( index );
                    List<Photo> serverAlbumPhotos = serverAlbum.getPhotos();
                    // 3rd, photos in this album, but not at local
                    for ( int j = 0; j < serverAlbumPhotos.size(); j++ )
                    {
                        PhotoFlickr photo = (PhotoFlickr) serverAlbumPhotos.get( j );
                        index = album.getPhotos().indexOf( photo );
                        if ( index < 0 )
                        {
                            // photo not found!! we need to create a new event
                            WatchEvent wevent = new WatchEvent();
                            wevent.setType( WatchEventType.CREATE );
                            wevent.setProcessor( this.processor );
                            wevent.setEvent( new ServerEvent( WatchEventType.CREATE, photo.getFlickrPhoto(),
                                                              serverAlbum.getFlickrSet(), photo.getId() ) );
                            events.add( wevent );
                        }
                    }
                }
            }

            // 4rd, photos -not in set- in local but not in server
            for ( int i = 0; i < this.serverCurrentIndex.photoNoInAlbums_size(); i++ )
            {
                PhotoFlickr serverPhoto = (PhotoFlickr) this.serverCurrentIndex.photoNoInAlbums_get( i );
                int index = serverJustNowIndex.photoNoInAlbums_indexOf( serverPhoto );
                if ( index < 0 )
                {
                    // photo not found!! we need to create a DELETE event
                    WatchEvent wevent = new WatchEvent();
                    wevent.setType( WatchEventType.DELETE );
                    wevent.setProcessor( this.processor );
                    wevent.setEvent( new ServerEvent( WatchEventType.DELETE, serverPhoto.getFlickrPhoto(), null,
                                                      serverPhoto.getId() ) );
                    events.add( wevent );
                }
            }

            // 5th, albums in local but not in server
            for ( int i = 0; i < this.serverCurrentIndex.albums_size(); i++ )
            {
                AlbumFlickr serverAlbum = (AlbumFlickr) this.serverCurrentIndex.albums_get( i );
                int index = serverJustNowIndex.albums_indexOf( serverAlbum );
                if ( index < 0 )
                {
                    // photo not found!! we need to create a DELETE event
                    WatchEvent wevent = new WatchEvent();
                    wevent.setType( WatchEventType.DELETE );
                    wevent.setProcessor( this.processor );
                    wevent.setEvent( new ServerEvent( WatchEventType.DELETE, null, serverAlbum.getFlickrSet(),
                                                      serverAlbum.getFlickrSet().getId() ) );
                    events.add( wevent );
                }
                else
                {
                    // 6th, photos of this albums in local but not in server
                    List<Photo> serverAlbumPhotos = serverAlbum.getPhotos();
                    Album serverIndexAlbum = serverJustNowIndex.albums_get( index );
                    for ( int j = 0; j < serverAlbumPhotos.size(); j++ )
                    {
                        PhotoFlickr serverPhoto = (PhotoFlickr) serverAlbumPhotos.get( j );
                        index = serverIndexAlbum.getPhotos().indexOf( serverPhoto );
                        if ( index < 0 )
                        {
                            // photo not found!! we need to create a DELETE event
                            WatchEvent wevent = new WatchEvent();
                            wevent.setType( WatchEventType.DELETE );
                            wevent.setProcessor( this.processor );
                            wevent.setEvent( new ServerEvent( WatchEventType.DELETE, serverPhoto.getFlickrPhoto(),
                                                              serverAlbum.getFlickrSet(), serverPhoto.getId() ) );
                            events.add( wevent );

                        }
                    }
                }
            }

            if ( events.size() > 0 )
            {
                processor.getPendingEvents().addAll( events );
                this.monitor.addEvents( events );
            }
        }
    }
}
