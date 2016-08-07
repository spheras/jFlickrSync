package org.jflickrsync.main.sync;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.jflickrsync.main.index.Index;
import org.jflickrsync.main.index.album.Album;
import org.jflickrsync.main.index.album.AlbumFile;
import org.jflickrsync.main.index.album.AlbumFlickr;
import org.jflickrsync.main.index.photo.Photo;
import org.jflickrsync.main.index.photo.PhotoFile;
import org.jflickrsync.main.index.photo.PhotoFlickr;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;

import lombok.Getter;
import lombok.Setter;

public class TotalSync
    implements Sync
{
    private static final Logger logger = Logger.getLogger( TotalSync.class );

    @Getter
    @Setter
    private Flickr flickr;

    @Getter
    @Setter
    private Index localCache;

    @Getter
    @Setter
    private Index serverCache;

    public TotalSync( Flickr flickr, Index localCache, Index serverCache )
    {
        this.flickr = flickr;
        this.localCache = localCache;
        this.serverCache = serverCache;
    }

    private void syncLocalToServer()
        throws FlickrException, IOException
    {
        // List<Album> localAlbums = localCache.getAlbums();
        // List<Photo> localNoAlbums = localCache.getPhotosNoInAlbums();

        for ( int j = 0; j < localCache.photoNoInAlbums_size(); j++ )
        {
            Photo photo = localCache.photoNoInAlbums_get( j );
            if ( !photo.isSynced() )
            {
                // this photo haven't been synced yet, so we need to upload
                // @TODO
                photo.setSynced( true );
            }
        }

        for ( int i = 0; i < localCache.albums_size(); i++ )
        {
            Album album = localCache.albums_get( i );
            if ( !album.isSynced() )
            {
                // this photo haven't been synced yet, so we need to upload all this album
                // @TODO
                album.setSynced( true );
            }
            else
            {
                List<? extends Photo> photos = album.getPhotos();
                for ( int j = 0; j < photos.size(); j++ )
                {
                    Photo photo = photos.get( j );
                    if ( !photo.isSynced() )
                    {
                        // this photo haven't been synced yet, so we need to upload
                        // @TODO
                        photo.setSynced( true );
                    }
                }
            }
        }

    }

    /**
     * Sync the local storage with the server info. Local will have at least all the server info.
     * 
     * @throws Exception
     */
    private void syncServerToLocal()
        throws Exception
    {
        // PhotosInterface photoint = flickr.getPhotosInterface();
        // List<Album> serverAlbums = serverCache.getAlbums();
        // List<Photo> serverNoAlbums = serverCache.getPhotosNoInAlbums();
        // List<Album> localAlbums = localCache.getAlbums();
        // List<Photo> localNoAlbums = localCache.getPhotosNoInAlbums();

        for ( int j = 0; j < serverCache.photoNoInAlbums_size(); j++ )
        {
            PhotoFlickr photo = (PhotoFlickr) serverCache.photoNoInAlbums_get( j );
            int indexLocalPhoto = localCache.photoNoInAlbums_indexOf( photo );
            if ( indexLocalPhoto < 0 )
            {
                // this photo/video doesn't exist at local, we need to download
                File newFile = photo.downloadFromFlickR();

                // we add this photo to local list
                PhotoFile newDownloadedPhoto = new PhotoFile( newFile );
                newDownloadedPhoto.setTitle( photo.getTitle() );
                localCache.photoNoInAlbums_add( newDownloadedPhoto );
                newDownloadedPhoto.setSynced( true );
            }

            photo.setSynced( true );
        }

        for ( int i = 0; i < serverCache.albums_size(); i++ )
        {
            AlbumFlickr serverAlbum = (AlbumFlickr) serverCache.albums_get( i );
            // is this album at local filesystem?
            int indexLocalAlbum = localCache.albums_indexOf( serverAlbum );
            if ( indexLocalAlbum < 0 )
            {
                // this album doesn't exist at local, we need to download the entire album?
                File newFileAlbum = serverAlbum.downloadFromFlickR();

                // we add this album and all the photos to local list
                AlbumFile newDownloadedAlbum = new AlbumFile( newFileAlbum );
                newDownloadedAlbum.setTitle( serverAlbum.getTitle() );
                localCache.albums_add( newDownloadedAlbum );
                for ( int j = 0; j < serverAlbum.getPhotos().size(); j++ )
                {
                    Photo serverPhoto = serverAlbum.getPhotos().get( j );
                    PhotoFile newDownloadedPhoto = new PhotoFile( new File( serverPhoto.getAbsolutePath() ) );
                    newDownloadedPhoto.setTitle( serverPhoto.getTitle() );
                    localCache.photoNoInAlbums_add( newDownloadedPhoto );
                    newDownloadedPhoto.setSynced( true );
                    newDownloadedAlbum.addPhoto( newDownloadedPhoto );
                }

                newDownloadedAlbum.setSynced( true );
            }
            else
            {
                // the album exists
                Album localAlbum = localCache.albums_get( indexLocalAlbum );
                localAlbum.setSynced( true );

                // let's see all the album's photos
                for ( int j = 0; j < serverAlbum.getPhotos().size(); j++ )
                {
                    PhotoFlickr photo = (PhotoFlickr) serverAlbum.getPhotos().get( j );
                    int indexLocalPhoto = localAlbum.getPhotos().indexOf( photo );
                    if ( indexLocalPhoto < 0 )
                    {
                        // this photo/video doesn't exist at local, we need to download
                        photo.downloadFromFlickR();
                    }

                    // we add this photo to local album
                    PhotoFile newDownloadedPhoto = new PhotoFile( new File( photo.getAbsolutePath() ) );
                    newDownloadedPhoto.setTitle( photo.getTitle() );
                    localAlbum.addPhoto( newDownloadedPhoto );
                    newDownloadedPhoto.setSynced( true );
                }
            }
            serverAlbum.setSynced( true );
        }
    }

    @Override
    public void sync()
        throws Exception
    {
        syncServerToLocal();
        syncLocalToServer();
    }

}
