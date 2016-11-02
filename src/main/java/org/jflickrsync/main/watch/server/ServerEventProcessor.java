package org.jflickrsync.main.watch.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jflickrsync.main.Configuration;
import org.jflickrsync.main.index.Index;
import org.jflickrsync.main.index.album.Album;
import org.jflickrsync.main.index.album.AlbumFile;
import org.jflickrsync.main.index.album.AlbumFlickr;
import org.jflickrsync.main.index.album.AlbumMock;
import org.jflickrsync.main.index.photo.PhotoFile;
import org.jflickrsync.main.index.photo.PhotoFlickr;
import org.jflickrsync.main.index.photo.PhotoMock;
import org.jflickrsync.main.util.Util;
import org.jflickrsync.main.watch.EventProcessor;
import org.jflickrsync.main.watch.WatchEvent;
import org.jflickrsync.main.watch.WatchEventType;
import org.jflickrsync.main.watch.local.LocalWatcher;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotoList;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photosets.Photoset;

import lombok.Getter;

public class ServerEventProcessor
    implements EventProcessor
{
    private static final Logger logger = Logger.getLogger( ServerEventProcessor.class );

    /**
     * This is the current local index at memory
     */
    private Index localCurrentIndex;

    /**
     * This is the current server index at memory
     */
    private Index serverCurrentIndex;

    /**
     * Flickr object
     */
    private Flickr flickr;

    /**
     * Pending events to be processed
     */
    @Getter
    private List<WatchEvent> pendingEvents = new ArrayList<>();

    /**
     * Local watcher
     */
    private LocalWatcher localWatcher;

    public ServerEventProcessor( Index localCurrentIndex, Index serverCurrentIndex, Flickr flickr,
                                 LocalWatcher localWatcher )
    {
        this.localCurrentIndex = localCurrentIndex;
        this.serverCurrentIndex = serverCurrentIndex;
        this.flickr = flickr;
        this.localWatcher = localWatcher;
    }

    @Override
    public void process( WatchEvent event )
        throws Exception
    {
        WatchEventType type = event.getType();
        ServerEvent sevent = (ServerEvent) event.getEvent();
        if ( type == WatchEventType.CREATE )
        {
            processCreateEvent( sevent );
        }
        else if ( type == WatchEventType.DELETE )
        {
            processDeleteEvent( sevent );
        }
        else if ( type == WatchEventType.MODIFY )
        {
            processModifyEvent( sevent );
        }
        else if ( type == WatchEventType.OVERFLOW )
        {
            logger.warn( "OVERFLOW Event!!!!!!!!!!" );
        }

        this.pendingEvents.remove( event );
    }

    private void processModifyEvent( ServerEvent event )
    {
        // TODO Auto-generated method stub
        logger.debug( "TODO MODIFY SERVER EVENT PROCESS" );
    }

    private void processDeleteEvent( ServerEvent event )
        throws IOException
    {
        Photo flickrPhoto = event.getServerPhoto();
        Photoset serverPhotoset = event.getServerPhotoset();
        PhotosInterface photosInt = flickr.getPhotosInterface();

        if ( serverPhotoset == null )
        {
            // a photo deleted without set
            PhotoMock mockPhoto = new PhotoMock( flickrPhoto.getTitle() );
            int indexLocal = this.localCurrentIndex.photoNoInAlbums_indexOf( mockPhoto );
            int indexServer = this.serverCurrentIndex.photoNoInAlbums_indexOf( mockPhoto );
            if ( indexLocal >= 0 && indexServer >= 0 )
            {
                PhotoFile localPhoto = (PhotoFile) this.localCurrentIndex.photoNoInAlbums_get( indexLocal );
                PhotoFlickr serverPhoto = (PhotoFlickr) this.serverCurrentIndex.photoNoInAlbums_get( indexServer );
                Photo realFlickrPhoto = null;
                try
                {
                    realFlickrPhoto = photosInt.getPhoto( serverPhoto.getId() );
                }
                catch ( FlickrException e )
                {
                    // not found?
                }

                if ( realFlickrPhoto != null )
                {
                    /// oh oh.. it seems that the photo has been moved instead of deleted...
                    // we will delete it phisically, but we need to bypass the event
                    this.localWatcher.addByPassDeleteEvent( localPhoto.getFilePhoto() );
                    this.localCurrentIndex.photoNoInAlbums_remove( localPhoto );
                    this.serverCurrentIndex.photoNoInAlbums_remove( localPhoto );
                }

                // if really it doesn't exist (because the photo could be moved instead of only deleted) then it is
                // deleted
                File f = localPhoto.getFilePhoto();
                if ( f.exists() )
                {
                    f.delete();
                }
            }
        }
        else if ( flickrPhoto == null )
        {
            // a deleted set
            AlbumMock mock = new AlbumMock( serverPhotoset.getTitle() );
            int indexLocal = this.localCurrentIndex.albums_indexOf( mock );
            int indexServer = this.serverCurrentIndex.albums_indexOf( mock );
            if ( indexLocal >= 0 && indexServer >= 0 )
            {
                AlbumFile localAlbum = (AlbumFile) this.localCurrentIndex.albums_get( indexLocal );
                AlbumFlickr serverAlbum = (AlbumFlickr) this.serverCurrentIndex.albums_get( indexServer );

                for ( int i = 0; i < serverAlbum.getPhotos().size(); i++ )
                {
                    PhotoFlickr serverPhoto = (PhotoFlickr) serverAlbum.getPhotos().get( i );
                    int indexPhotoFile = localAlbum.getPhotos().indexOf( serverPhoto );
                    PhotoFile localPhoto = (PhotoFile) localAlbum.getPhotos().get( indexPhotoFile );

                    Photo realPhoto = null;
                    try
                    {
                        realPhoto = photosInt.getPhoto( serverPhoto.getId() );
                    }
                    catch ( FlickrException e )
                    {
                        // it doesn't exist
                    }

                    if ( realPhoto != null )
                    {
                        // the photo still exists.. then it has been moved!
                        this.localWatcher.addByPassDeleteEvent( localPhoto.getFilePhoto() );
                    }

                    if ( localPhoto.getFilePhoto().exists() )
                    {
                        localPhoto.getFilePhoto().delete();
                    }

                }

                File falbum = localAlbum.getAbsolutePathFile();
                this.localWatcher.addByPassDeleteEvent( falbum );

                this.localCurrentIndex.albums_remove( indexLocal );
                this.serverCurrentIndex.albums_remove( indexServer );

                if ( falbum.exists() )
                {
                    Util.deleteFileOrFolder( Paths.get( falbum.getAbsolutePath() ) );
                }
            }
        }
        else
        {
            // a deleted photo in a set
            AlbumMock mock = new AlbumMock( serverPhotoset.getTitle() );
            Album localAlbum = this.localCurrentIndex.locateAlbum( mock );
            Album serverAlbum = this.serverCurrentIndex.locateAlbum( mock );

            if ( localAlbum != null )
            {
                PhotoFile localPhoto = (PhotoFile) localAlbum.locatePhoto( flickrPhoto.getTitle() );
                PhotoFlickr serverPhoto = (PhotoFlickr) serverAlbum.locatePhoto( flickrPhoto.getTitle() );
                if ( localPhoto != null && serverPhoto != null )
                {
                    Photo realFlickrPhoto = null;
                    try
                    {
                        realFlickrPhoto = photosInt.getPhoto( serverPhoto.getId() );
                    }
                    catch ( FlickrException e )
                    {
                        // the photo still exists, the it has been moved
                    }

                    if ( realFlickrPhoto != null )
                    {
                        // it is moved, we will bypass the delete event
                        this.localWatcher.addByPassDeleteEvent( localPhoto.getFilePhoto() );
                        serverAlbum.getPhotos().remove( serverAlbum.getPhotos().indexOf( serverPhoto ) );
                        localAlbum.getPhotos().remove( localAlbum.getPhotos().indexOf( localPhoto ) );
                    }

                    File file = localPhoto.getFilePhoto();
                    if ( file.exists() )
                    {
                        file.delete();
                    }
                }
            }
        }
    }

    private void processCreateEvent( ServerEvent event )
        throws Exception
    {
        Photo flickrPhoto = event.getServerPhoto();
        Photoset serverPhotoset = event.getServerPhotoset();

        if ( serverPhotoset == null )
        {
            // a new photo without set
            int index = this.localCurrentIndex.photoNoInAlbums_indexOf( new PhotoMock( flickrPhoto.getTitle() ) );
            if ( index < 0 )
            {
                File file = new File( Configuration.getBasePath() + File.separatorChar + flickrPhoto.getTitle() + "."
                    + flickrPhoto.getOriginalFormat() );
                PhotoFile localPhoto = new PhotoFile( file );
                localPhoto.setSynced( true );
                this.localCurrentIndex.photoNoInAlbums_add( localPhoto );

                PhotoFlickr serverPhoto = new PhotoFlickr( this.flickr, flickrPhoto );
                serverPhoto.setSynced( true );
                this.serverCurrentIndex.photoNoInAlbums_add( serverPhoto );

                serverPhoto.downloadFromFlickR();
            }
        }
        else if ( flickrPhoto == null )
        {
            // a new set
            int index = this.localCurrentIndex.albums_indexOf( new AlbumMock( serverPhotoset.getTitle() ) );
            if ( index < 0 )
            {
                AlbumFlickr serverAlbum = new AlbumFlickr( this.flickr, serverPhotoset );
                serverAlbum.setSynced( true );

                int MAX_PAGES = 5000;
                int page = 1;
                int pages = 2;
                while ( pages > page )
                {
                    PhotoList<Photo> flickrPhotos =
                        this.flickr.getPhotosetsInterface().getPhotos( serverPhotoset.getId(), MAX_PAGES, page );
                    pages = flickrPhotos.getPages();
                    for ( int i = 0; i < flickrPhotos.size(); i++ )
                    {
                        Photo photoi = flickrPhotos.get( i );
                        PhotoFlickr photo = new PhotoFlickr( this.flickr, photoi );
                        photo.setSynced( true );
                        serverAlbum.addPhoto( photo );
                    }
                    page++;
                }

                this.serverCurrentIndex.albums_add( serverAlbum );
                File falbum = serverAlbum.downloadFromFlickR();

                AlbumFile localAlbum = new AlbumFile( falbum );
                for ( int i = 0; i < serverAlbum.getPhotos().size(); i++ )
                {
                    File fphoto = new File( Configuration.getBasePath() + File.separatorChar + serverAlbum.getTitle()
                        + File.separatorChar + serverAlbum.getPhotos().get( i ).getFilename() );
                    PhotoFile localPhoto = new PhotoFile( fphoto );
                    localPhoto.setSynced( true );
                    localAlbum.addPhoto( localPhoto );
                }
                this.localCurrentIndex.albums_add( localAlbum );
            }
        }
        else
        {
            // a new photo in a set
            AlbumMock mock = new AlbumMock( serverPhotoset.getTitle() );
            Album serverAlbum = this.serverCurrentIndex.locateAlbum( mock );
            Album localAlbum = this.localCurrentIndex.locateAlbum( mock );

            if ( serverAlbum != null )
            {
                if ( serverAlbum.locatePhoto( flickrPhoto.getTitle() ) == null )
                {
                    PhotoFlickr serverPhoto = new PhotoFlickr( this.flickr, flickrPhoto );
                    serverPhoto.setSynced( true );
                    File fphoto = serverPhoto.downloadFromFlickR();

                    serverAlbum.addPhoto( serverPhoto );

                    PhotoFile localPhoto = new PhotoFile( fphoto );
                    localPhoto.setSynced( true );
                    localAlbum.addPhoto( localPhoto );
                }
            }
        }

    }

}
