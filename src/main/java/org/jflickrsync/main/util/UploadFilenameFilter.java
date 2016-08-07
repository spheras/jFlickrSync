package org.jflickrsync.main.util;

import java.io.File;
import java.io.FilenameFilter;

import org.apache.log4j.Logger;

public class UploadFilenameFilter
    implements FilenameFilter
{
    private static final Logger logger = Logger.getLogger( UploadFilenameFilter.class );

    // Following suffixes from flickr upload page. An App should have this configurable,
    // for videos and photos separately.
    private static final String[] photoSuffixes = { "jpg", "jpeg", "png", "gif", "bmp", "tif", "tiff" };

    private static final String[] videoSuffixes =
        { "3gp", "3gp", "avi", "mov", "mp4", "mpg", "mpeg", "wmv", "ogg", "ogv", "m2v" };

    @Override
    public boolean accept( File dir, String name )
    {
        if ( isValidSuffix( name ) )
            return true;
        else
            return false;
    }

    private static boolean isValidSuffix( String basefilename )
    {
        if ( basefilename.lastIndexOf( '.' ) <= 0 )
        {
            return false;
        }
        String suffix = basefilename.substring( basefilename.lastIndexOf( '.' ) + 1 ).toLowerCase();
        for ( int i = 0; i < photoSuffixes.length; i++ )
        {
            if ( photoSuffixes[i].equals( suffix ) )
                return true;
        }
        for ( int i = 0; i < videoSuffixes.length; i++ )
        {
            if ( videoSuffixes[i].equals( suffix ) )
                return true;
        }
        logger.debug( basefilename + " does not have a valid suffix, skipped." );
        return false;
    }

}