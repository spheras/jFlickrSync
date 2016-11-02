package org.jflickrsync.main.index.photo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jflickrsync.main.Configuration;
import org.jflickrsync.main.util.FlickrUtil;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.uploader.UploadMetaData;
import com.flickr4java.flickr.uploader.Uploader;

public class PhotoFile
    extends Photo
{
    private static final Logger logger = Logger.getLogger( PhotoFile.class );

    private File filePhoto;

    public PhotoFile( File f )
    {
        super();
        this.filePhoto = f;
        String abspath = this.filePhoto.getAbsolutePath();
        int index = abspath.lastIndexOf( File.separatorChar );
        String title = abspath.substring( index + 1 );
        int dotindex = title.lastIndexOf( '.' );
        if ( dotindex >= 0 )
        {
            title = title.substring( 0, dotindex );
        }

        setTitle( title );
    }

    public File getFilePhoto()
    {
        return filePhoto;
    }

    public void setFilePhoto( File filePhoto )
    {
        this.filePhoto = filePhoto;
    }

    /**
     * Upload the photo to flickr and return the photoid obtained
     * 
     * @param flickr {@link Flickr}
     * @return the photoid obtained at flickr
     * @throws FlickrException
     */
    public String uploadToFlickR( Flickr flickr )
        throws FlickrException
    {
        int privacy = Configuration.getPrivacy();

        UploadMetaData metaData = new UploadMetaData();

        if ( privacy == 1 )
            metaData.setPublicFlag( true );
        if ( privacy == 2 || privacy == 4 )
            metaData.setFriendFlag( true );
        if ( privacy == 3 || privacy == 4 )
            metaData.setFamilyFlag( true );

        String basefilename = this.getFilePhoto().getName();

        boolean setMimeType = true; // change during testing. Doesn't seem to be supported at this time in flickr.
        if ( setMimeType )
        {
            if ( basefilename.lastIndexOf( '.' ) > 0 )
            {
                String suffix = basefilename.substring( basefilename.lastIndexOf( '.' ) + 1 );
                // Set Mime Type if known.

                // Later use a mime-type properties file or a hash table of all known photo and video types
                // allowed by flickr.
                if ( suffix.equalsIgnoreCase( "png" ) )
                {
                    metaData.setFilemimetype( "image/png" );
                }
                else if ( suffix.equalsIgnoreCase( "mpg" ) || suffix.equalsIgnoreCase( "mpeg" ) )
                {
                    metaData.setFilemimetype( "video/mpeg" );
                }
                else if ( suffix.equalsIgnoreCase( "mov" ) )
                {
                    metaData.setFilemimetype( "video/quicktime" );
                }
            }
        }

        // UploadMeta is using String not Tag class.

        // Tags are getting mangled by yahoo stripping off the = , '.' and many other punctuation characters
        // and converting to lower case: use the raw tag field to find the real value for checking and
        // for download.
        if ( Configuration.getNoTags() )
        {
            List<String> tags = new ArrayList<String>();
            String tmp = basefilename;
            basefilename = FlickrUtil.makeSafeFilename( basefilename );
            tags.add( "OrigFileName='" + basefilename + "'" );
            metaData.setTags( tags );

            if ( !tmp.equals( basefilename ) )
            {
                logger.warn( " File : " + basefilename + " contains special characters.  stored as " + basefilename
                    + " in tag field" );
            }
        }

        // File imageFile = new File(filename);
        // InputStream in = null;
        Uploader uploader = flickr.getUploader();

        // ByteArrayOutputStream out = null;
        try
        {
            // in = new FileInputStream(imageFile);
            // out = new ByteArrayOutputStream();

            // int b = -1;
            /**
             * while ((b = in.read()) != -1) { out.write((byte) b); }
             **/

            /**
             * byte[] buf = new byte[1024]; while ((b = in.read(buf)) != -1) { // fos.write(read); out.write(buf, 0, b);
             * }
             **/

            metaData.setFilename( basefilename );
            // check correct handling of escaped value

            String photoId = uploader.upload( getFilePhoto(), metaData );

            // logger.debug( " File : " + getFilePhoto().getName() + " uploaded: photoId = " + photoId );
            return photoId;
        }
        finally
        {

        }
    }

    @Override
    public boolean remove()
        throws Exception
    {

        if ( this.filePhoto.exists() )
        {
            return this.filePhoto.delete();
        }
        else
        {
            return false;
        }
    }

    @Override
    public String getFilename()
    {
        return this.filePhoto.getName();
    }

    @Override
    public String getAbsolutePath()
    {
        return this.filePhoto.getAbsolutePath();
    }

}
