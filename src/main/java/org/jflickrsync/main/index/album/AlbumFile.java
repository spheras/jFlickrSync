package org.jflickrsync.main.index.album;

import java.io.File;

import org.apache.log4j.Logger;
import org.jflickrsync.main.Configuration;
import org.jflickrsync.main.index.photo.Photo;
import org.jflickrsync.main.index.photo.PhotoFile;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.PhotosetsInterface;

public class AlbumFile
    extends Album
{
    private static final Logger logger = Logger.getLogger( AlbumFile.class );

    private File fAlbum;

    public AlbumFile( File f )
    {
        super();
        this.fAlbum = f;
        String title = f.getAbsolutePath().substring( Configuration.getBasePath().length() + 1 );
        setTitle( title );
    }

    @Override
    public void addPhoto( Photo photo )
    {
        if ( photo instanceof PhotoFile )
        {
            photo.setAlbum( this );
            getPhotos().add( photo );
        }
        else
        {
            logger.error( "trying to add a non photofile to an albumfile" );
        }

    }

    /**
     * Upload the album as a {@link Photoset} to flickr. It only links this album with a primary Photo ID that you must
     * pass. It is impossible to create an album without a primary photo. So, you need to link an existing photo for
     * this uploaded album. </br>
     * NOTE: This method doesn't upload the contained photos of the album!
     * 
     * @param flickr {@link Flickr} object
     * @param primaryPhotoID {@link String} photoid of the first photo linked to this new album
     * @return {@link Photoset} the photoset created
     * @throws FlickrException
     */
    public Photoset uploadToFlickr( Flickr flickr, String primaryPhotoID )
        throws FlickrException
    {
        PhotosetsInterface psetsInterface = flickr.getPhotosetsInterface();

        Photoset set = null;

        // In case it is a new photo-set.
        String description = getTitle();
        set = psetsInterface.create( getTitle(), description, primaryPhotoID );
        set.setTitle( getTitle() );

        return set;
    }

    @Override
    public boolean remove()
        throws Exception
    {
        if ( this.fAlbum.exists() )
        {
            return this.fAlbum.delete();
        }
        else
        {
            return false;
        }
    }

}
