package org.jflickrsync.main.index.photo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.jflickrsync.main.Configuration;
import org.jflickrsync.main.index.album.Album;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photos.Size;

import lombok.Getter;
import lombok.Setter;

public class PhotoFlickr
    extends Photo
{
    private static final Logger logger = Logger.getLogger( PhotoFlickr.class );

    private com.flickr4java.flickr.photos.Photo flickrPhoto;

    private Flickr flickr;

    private PhotosInterface photoInterface;

    /**
     * Constructor
     * 
     * @param flickr Flickr instance
     */
    public PhotoFlickr( Flickr flickr, com.flickr4java.flickr.photos.Photo flickrPhoto )
    {
        super();
        this.flickrPhoto = flickrPhoto;
        this.flickr = flickr;
        this.photoInterface = this.flickr.getPhotosInterface();
        this.setId( flickrPhoto.getId() );
        this.setTitle( flickrPhoto.getTitle() );
    }

    @Getter
    @Setter
    private String id;

    @Override
    public String getFilename()
    {
        String filename = getTitle();
        filename = filename.substring( filename.lastIndexOf( "/" ) + 1, filename.length() ) + "." + getExtension();
        return filename;
    }

    @Override
    public String getAbsolutePath()
    {
        String absPath = Configuration.getBasePath();
        Album album = getAlbum();
        if ( album != null )
        {
            absPath = absPath + File.separatorChar + album.getTitle() + File.separatorChar + getFilename();
        }
        else
        {
            absPath = absPath + File.separatorChar + getFilename();
        }
        return absPath;
    }

    /**
     * Download this photo to the folder specified
     * 
     * @param photoInt {@link PhotosInterface} interface to download the photo from flickr api
     * @param destinationFolder {@link String} destination folder where the photo will be downloaded
     * @return {@link File} the file created
     * @throws FlickrException
     * @throws IOException
     */
    public File downloadFromFlickR()
        throws FlickrException, IOException
    {
        if ( flickrPhoto != null )
        {
            PhotosInterface photoInt = this.flickr.getPhotosInterface();
            logger.info( "Downloading photo/video:" + getTitle()
                + ( this.getAlbum() != null ? " OF ALBUM '" + this.getAlbum().getTitle() + "'" : " WITHOUT ALBUM" ) );
            String filename = getFilename();
            BufferedInputStream inStream =
                new BufferedInputStream( photoInt.getImageAsStream( flickrPhoto, Size.LARGE ) );
            String destinationFolder = ( this.getAlbum() != null ? this.getAlbum().getLocalPathAlbum().getAbsolutePath()
                            : Configuration.getBasePath() );
            File newFile = new File( destinationFolder, filename );

            FileOutputStream fos = new FileOutputStream( newFile );

            int read;

            while ( ( read = inStream.read() ) != -1 )
            {
                fos.write( read );
            }
            fos.flush();
            fos.close();
            inStream.close();

            return newFile;
        }
        else
        {
            logger.warn( "Trying to download a photo without a flickr reference" );
        }
        return null;

    }

    public com.flickr4java.flickr.photos.Photo getFlickrPhoto()
    {
        return flickrPhoto;
    }

    public void setFlickrPhoto( com.flickr4java.flickr.photos.Photo flickrPhoto )
    {
        this.flickrPhoto = flickrPhoto;
    }

    public String getExtension()
    {
        String format = this.flickrPhoto.getOriginalFormat();
        if ( format != null && format.length() < 4 )
        {
            return format;
        }
        else
        {
            return "jpeg";
        }
    }

    @Override
    public boolean remove()
        throws FlickrException
    {
        try
        {
            com.flickr4java.flickr.photos.Photo fPhoto = this.photoInterface.getInfo( getId(), "" );
            if ( fPhoto != null )
            {
                this.photoInterface.delete( fPhoto.getId() );
                return true;
            }
            else
            {
                return false;
            }
        }
        catch ( FlickrException fe )
        {
            return false;
        }
    }
}
