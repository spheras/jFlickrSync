package org.jflickrsync.main.index.album;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.jflickrsync.main.index.photo.Photo;
import org.jflickrsync.main.index.photo.PhotoFlickr;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photosets.Photoset;

public class AlbumFlickr
    extends Album
{
    private static final Logger logger = Logger.getLogger( AlbumFlickr.class );

    private Photoset flickrSet;

    private Flickr flickr;

    public AlbumFlickr( Flickr flickr, Photoset set )
    {
        super();

        this.flickr = flickr;
        this.flickrSet = set;
        this.setTitle( set.getTitle() );
    }

    /**
     * Download the entire album (with photos as well)
     * 
     * @return {@link File} the File obtained (directory)
     * @throws FlickrException
     * @throws IOException
     */
    public File downloadFromFlickR()
        throws FlickrException, IOException
    {
        logger.info( "Downloading set:" + getTitle() );
        if ( flickrSet != null )
        {
            File falbum = getAbsolutePathFile();
            falbum.mkdirs();
            for ( int i = 0; i < getPhotos().size(); i++ )
            {
                File fphoto = ( (PhotoFlickr) getPhotos().get( i ) ).downloadFromFlickR();
            }
            return falbum;
        }
        else
        {
            logger.warn( "Trying to download an album without a flickr reference" );
        }
        return null;
    }

    /**
     * Add the photo {@link PhotoFlickr} to the set in flickr
     * 
     * @param photo {@link PhotoFlickr} the photo to add
     * @return if it was sucessfully
     */
    public boolean addPhotoToFlickrSet( PhotoFlickr photo )
    {
        try
        {
            this.flickr.getPhotosetsInterface().addPhoto( this.flickrSet.getId(), photo.getId() );
            return true;
        }
        catch ( FlickrException e )
        {
            logger.error( "Error adding the photo to the album!" );
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void addPhoto( Photo photo )
    {
        if ( photo instanceof PhotoFlickr )
        {
            photo.setAlbum( this );
            getPhotos().add( (PhotoFlickr) photo );
        }
        else
        {
            logger.error( "trying to add a no photoflickr to an albumflickr" );
        }
    }

    public Photoset getFlickrSet()
    {
        return flickrSet;
    }

    public void setFlickrSet( Photoset flickrSet )
    {
        this.flickrSet = flickrSet;
    }

    @Override
    public boolean remove()
        throws Exception
    {
        try
        {
            for ( int i = 0; i < getPhotos().size(); i++ )
            {
                PhotoFlickr photo = (PhotoFlickr) getPhotos().get( i );
                photo.remove();
            }

            return true;

            // Photoset fset = this.photosetsInterface.getInfo( flickrSet.getId() );
            // if ( fset != null )
            // {
            // this.photosetsInterface.delete( fset.getId() );
            // return true;
            // }
            // else
            // {
            // return false;
            // }
        }
        catch ( FlickrException fe )
        {
            return false;
        }
    }

}
