package org.jflickrsync.main.watch.local;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jflickrsync.main.Configuration;
import org.jflickrsync.main.index.Index;
import org.jflickrsync.main.index.album.Album;
import org.jflickrsync.main.index.album.AlbumFile;
import org.jflickrsync.main.index.album.AlbumFlickr;
import org.jflickrsync.main.index.photo.Photo;
import org.jflickrsync.main.index.photo.PhotoFile;
import org.jflickrsync.main.index.photo.PhotoFlickr;
import org.jflickrsync.main.util.Util;
import org.jflickrsync.main.watch.EventProcessor;
import org.jflickrsync.main.watch.WatchEvent;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.photosets.Photoset;

public class LocalEventProcessor
    implements EventProcessor
{
    private static final Logger logger = Logger.getLogger( LocalEventProcessor.class );

    /**
     * List of folders that need to be reviewed due they were created and we need to be sure that all the resources has
     * been synced
     */
    private List<File> pendingFolders = new ArrayList<>();

    /**
     * Last time we received an event
     */
    private long lastEventTimestamp = 0;

    /**
     * Time to wait until review folders
     */
    private static final long TIME_TO_WAIT = 1000 * 3;

    /**
     * This is the thread that is waiting to start reviewing pending folders
     */
    private Thread waitThread = null;

    /**
     * This is the current local index at memory
     */
    private Index localCurrentIndex;

    /**
     * This is the current server index at memory
     */
    private Index serverCurrentIndex;

    /**
     * Local parent watcher
     */
    private LocalWatcher watcher;

    /**
     * Flickr object
     */
    private Flickr flickr;

    public LocalEventProcessor( Index localCurrentIndex, Index serverCurrentIndex, Flickr flickr, LocalWatcher watcher )
    {
        this.localCurrentIndex = localCurrentIndex;
        this.serverCurrentIndex = serverCurrentIndex;
        this.flickr = flickr;
        this.watcher = watcher;
    }

    /**
     * Add a pending folder to the list
     * 
     * @param fFolder {@link File}
     */
    synchronized void addPendingFolders( File fFolder )
    {
        this.pendingFolders.add( fFolder );
    }

    /**
     * Create a thread and wait for a period (seconds wait after the last event). After that it starts reviewing created
     * folders.
     */
    synchronized Thread waitToReviewFolders()
    {
        if ( this.waitThread == null )
        {
            this.waitThread = new Thread( "WAIT_REVIEW_FOLDERS" )
            {
                @Override
                public void run()
                {
                    try
                    {
                        do
                        {
                            Thread.sleep( TIME_TO_WAIT );
                        }
                        while ( System.currentTimeMillis() - lastEventTimestamp < TIME_TO_WAIT );

                        PathEvents pathEvents = new PathEvents( true, Paths.get( Configuration.getBasePath() ) );
                        for ( int i = 0; i < pendingFolders.size(); i++ )
                        {
                            File fpending = pendingFolders.get( i );
                            if ( fpending.exists() )
                            {
                                watcher.watchSubdirectory( fpending.getAbsolutePath() );
                                String[] files = fpending.list();
                                for ( int j = 0; j < files.length; j++ )
                                {
                                    Path context = Paths.get( files[j] );
                                    Path dir = Paths.get( fpending.getAbsolutePath() );
                                    PathEvent pathevent = new PathEvent( dir, context, ENTRY_MODIFY );
                                    pathEvents.add( pathevent );
                                }
                            }
                        }

                        pendingFolders.clear();
                        watcher.incomingEvent( pathEvents );
                        waitThread = null;
                    }
                    catch ( InterruptedException | IOException e )
                    {
                        e.printStackTrace();
                    }
                }
            };
            this.waitThread.start();
        }
        return this.waitThread;
    }

    @Override
    public void process( WatchEvent wEvent )
        throws Exception
    {
        lastEventTimestamp = System.currentTimeMillis();

        PathEvent event = (PathEvent) wEvent.getEvent();
        Object type = event.getType();
        if ( type == ENTRY_CREATE )
        {
            // create, we need to wait the modify event to been completelly copied
            String filename = event.getEventTarget().toFile().getName();
            String absolutePath = event.getDir().toString() + File.separatorChar + filename;
            logger.info( "Create Event (real):" + Util.getRelativePath( absolutePath ) );

            File ffolder = new File( absolutePath );
            if ( ffolder.exists() && ffolder.isDirectory() )
            {
                addPendingFolders( ffolder );
                waitToReviewFolders();
            }

        }
        else if ( type == ENTRY_DELETE )
        {
            processDeleteEvent( event );
        }
        else if ( type == ENTRY_MODIFY )
        {
            String filename = event.getEventTarget().toFile().getName();
            String absolutePath = event.getDir().toString() + File.separatorChar + filename;
            boolean exist = this.localCurrentIndex.existPhotoOrAlbum( absolutePath );
            if ( !exist )
            {
                processCreateEvent( event );
            }
            else
            {
                // ??
                // String relativePath = Util.getRelativePath( absolutePath );
                // logger.debug( "Modify Event!" + relativePath );
            }
        }
        else if ( type == OVERFLOW )
        {
            logger.warn( "OVERFLOW Event!!!!!!!!!!" );
        }
    }

    /**
     * Process a new create event
     * 
     * @param event
     * @throws Exception
     */
    private void processCreateEvent( PathEvent event )
        throws Exception
    {
        String filename = event.getEventTarget().toFile().getName();
        String absolutePath = event.getDir().toString() + File.separatorChar + filename;
        File fabsolutePath = new File( absolutePath );
        String relativePath = Util.getRelativePath( absolutePath );
        logger.info( "Create Event: " + relativePath );
        int indexPathSeparator = relativePath.indexOf( File.separatorChar );

        if ( indexPathSeparator < 0 )
        {
            if ( fabsolutePath.isDirectory() || !fabsolutePath.exists() )
            {
                // new album, we need to wait until have at least one photo to create a set
            }
            else
            {
                // photo without album
                PhotoFile localPhoto = new PhotoFile( new File( absolutePath ) );
                localPhoto.setSynced( true );
                localCurrentIndex.photoNoInAlbums_add( localPhoto );

                // uploading the local photo
                String photoid = localPhoto.uploadToFlickR( flickr );
                com.flickr4java.flickr.photos.Photo flickrPhoto = flickr.getPhotosInterface().getPhoto( photoid );

                PhotoFlickr serverPhoto = new PhotoFlickr( flickr, flickrPhoto );
                serverPhoto.setSynced( true );
                serverCurrentIndex.photoNoInAlbums_add( serverPhoto );
            }
        }
        else
        {
            String folderName = relativePath.substring( 0, indexPathSeparator );
            Album localAlbum = localCurrentIndex.locateAlbum( folderName );
            boolean newAlbum = false;
            if ( localAlbum == null )
            {
                newAlbum = true;
                // the album doesn't exist, we need to create it before
                // new localalbum
                AlbumFile newLocalAlbum =
                    new AlbumFile( new File( Configuration.getBasePath() + File.separatorChar + folderName ) );
                newLocalAlbum.setSynced( true );
                localCurrentIndex.albums_add( newLocalAlbum );
                localAlbum = newLocalAlbum;

            }

            PhotoFile localPhoto = new PhotoFile( new File( absolutePath ) );
            localPhoto.setSynced( true );
            localAlbum.addPhoto( localPhoto );

            // uploading the local photo
            String photoid = localPhoto.uploadToFlickR( flickr );
            com.flickr4java.flickr.photos.Photo flickrPhoto = flickr.getPhotosInterface().getPhoto( photoid );

            PhotoFlickr serverPhoto = new PhotoFlickr( flickr, flickrPhoto );
            serverPhoto.setSynced( true );

            if ( newAlbum )
            {
                // new serveralbum
                Photoset set = ( (AlbumFile) localAlbum ).uploadToFlickr( this.flickr, flickrPhoto.getId() );
                AlbumFlickr newServerAlbum = new AlbumFlickr( this.flickr, set );
                newServerAlbum.setSynced( true );
                serverCurrentIndex.albums_add( newServerAlbum );
                newServerAlbum.addPhoto( serverPhoto );
            }
            else
            {
                // existing serveralbum
                AlbumFlickr serverAlbum = (AlbumFlickr) serverCurrentIndex.locateAlbum( localAlbum );
                serverAlbum.addPhoto( serverPhoto );
                serverAlbum.addPhotoToFlickrSet( serverPhoto );
            }
        }

    }

    /**
     * processing a delete event
     * 
     * @param event
     * @throws Exception
     */
    private void processDeleteEvent( PathEvent event )
        throws Exception
    {
        String filename = event.getEventTarget().toFile().getName();
        String absolutePath = event.getDir().toString() + File.separatorChar + filename;
        String relativePath = Util.getRelativePath( absolutePath );
        logger.info( "Delete Event! -> " + relativePath );
        int indexSeparator = relativePath.indexOf( File.separatorChar );
        int indexDot = relativePath.lastIndexOf( '.' );

        if ( ( indexSeparator < 0 && indexDot < 0 ) )
        {
            // an album?
            Album localAlbum = localCurrentIndex.locateAlbum( filename );
            if ( localAlbum != null )
            {
                localCurrentIndex.album_remove( localAlbum );
            }
            Album serverAlbum = serverCurrentIndex.locateAlbum( filename );
            if ( serverAlbum != null )
            {
                serverCurrentIndex.album_remove( serverAlbum );
            }

        }
        else
        {
            Photo localPhoto = localCurrentIndex.locatePhoto( absolutePath );
            if ( localPhoto == null )
            {
                logger.warn( "photo doesn't exist (and i was trying to remove it): " + relativePath );
                return;
            }
            if ( localPhoto.getAlbum() != null )
            {
                Album localAlbum = localPhoto.getAlbum();
                localAlbum.removePhoto( localPhoto );
                Photo serverPhoto = serverCurrentIndex.locatePhoto( absolutePath );
                serverPhoto.getAlbum().removePhoto( serverPhoto );

                if ( localAlbum.getPhotos().size() == 0 )
                {
                    // we will remove the local folder too, albums doesn't exist if there are any photo inside in flickr
                    serverCurrentIndex.album_remove( localAlbum );
                    localCurrentIndex.album_remove( localAlbum );
                }
            }
            else
            {
                localPhoto.remove();
                localCurrentIndex.photoNoInAlbums_remove( localPhoto );
                Photo serverPhoto = serverCurrentIndex.locatePhoto( localPhoto );
                serverPhoto.remove();
                serverCurrentIndex.photoNoInAlbums_remove( serverPhoto );
            }
        }
    }

}
