package org.jflickrsync.main.flickr;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jflickrsync.main.Configuration;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.RequestContext;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotoList;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.Photosets;
import com.flickr4java.flickr.photosets.PhotosetsInterface;
import com.flickr4java.flickr.uploader.UploadMetaData;
import com.flickr4java.flickr.uploader.Uploader;

public class UploadPhoto
{
    private static final Logger logger = Logger.getLogger( UploadPhoto.class );

    private static final String[] photoSuffixes = { "jpg", "jpeg", "png", "gif", "bmp", "tif", "tiff" };

    private static final String[] videoSuffixes =
        { "3gp", "3gp", "avi", "mov", "mp4", "mpg", "mpeg", "wmv", "ogg", "ogv", "m2v" };

    private final HashMap<String, Photo> filePhotos = new HashMap<String, Photo>();

    private final PhotoList<Photo> photos = new PhotoList<Photo>();

    private HashMap<String, ArrayList<String>> setNameToId = new HashMap<String, ArrayList<String>>();

    private HashMap<String, Photoset> allSetsMap = new HashMap<String, Photoset>();

    private static final SimpleDateFormat smp = new SimpleDateFormat( "yyyy.MM.dd HH:mm:ss a" );

    private Authorization auth;

    private Flickr flickr;

    public UploadPhoto( Authorization auth, Flickr flickr )
    {
        this.auth = auth;
        this.flickr = flickr;
    }

    /**
     * Upload a file to flickr with a set name
     * 
     * @param filename {@link String} filename to upload
     * @param setName {@link String} name of the set where the photo will be uploaded
     * @throws Exception
     */
    public boolean upload( String filename, String setName )
        throws Exception
    {
        assert ( !filename.isEmpty() );

        String photoid;
        String basefilename;
        String setid = null;

        getPhotosetsInfo( Configuration.getNSid() );

        if ( setName != null && !setName.equals( "" ) )
        {
            setid = getSetPhotos( setName );
        }

        if ( filename.lastIndexOf( File.separatorChar ) > 0 )
            basefilename = filename.substring( filename.lastIndexOf( File.separatorChar ) + 1, filename.length() );
        else
            basefilename = filename;

        boolean fileUploaded = checkIfLoaded( basefilename, filename );

        if ( !fileUploaded )
        {
            if ( !isValidSuffix( basefilename ) )
            {
                logger.error( " File: " + basefilename + " is not a supported filetype for flickr (invalid suffix)" );
                return false;
            }

            File f = new File( filename );
            if ( !f.exists() || !f.canRead() )
            {
                logger.error( " File: " + filename + " cannot be processed, does not exist or is unreadable." );
                return false;
            }
            logger.debug( "Calling uploadfile for filename : " + filename );
            logger.info( "Upload of " + filename + " started at: " + smp.format( new Date() ) + "\n" );

            photoid = uploadfile( filename, null, basefilename );
            // Add to Set. Create set if it does not exist.
            if ( photoid != null )
            {
                addPhotoToSet( photoid, setName, setid, basefilename );
            }
            logger.info( "Upload of " + filename + " finished at: " + smp.format( new Date() ) + "\n" );

        }
        else
        {
            logger.info( " File: " + filename + " has already been loaded on "
                + getUploadedTime( filename, basefilename ) );
        }

        return true;
    }

    private String getSetPhotos( String setName )
        throws FlickrException
    {
        // Check if this is an existing set. If it is get all the photo list to avoid reloading already
        // loaded photos.
        ArrayList<String> setIdarr;
        setIdarr = setNameToId.get( setName );
        if ( setIdarr != null )
        {
            String setid = setIdarr.get( 0 );
            PhotosetsInterface pi = flickr.getPhotosetsInterface();

            Set<String> extras = new HashSet<String>();
            /**
             * A comma-delimited list of extra information to fetch for each returned record. Currently supported fields
             * are: license, date_upload, date_taken, owner_name, icon_server, original_format, last_update, geo, tags,
             * machine_tags, o_dims, views, media, path_alias, url_sq, url_t, url_s, url_m, url_o
             */

            extras.add( "date_upload" );
            extras.add( "original_format" );
            extras.add( "media" );
            // extras.add("url_o");
            extras.add( "tags" );

            int setPage = 1;
            while ( true )
            {
                PhotoList<Photo> tmpSet = pi.getPhotos( setid, extras, Flickr.PRIVACY_LEVEL_NO_FILTER, 500, setPage );

                int tmpSetSize = tmpSet.size();
                photos.addAll( tmpSet );
                if ( tmpSetSize < 500 )
                {
                    break;
                }
                setPage++;
            }
            for ( int i = 0; i < photos.size(); i++ )
            {
                filePhotos.put( photos.get( i ).getTitle(), photos.get( i ) );
            }

            logger.debug( "Set title: " + setName + "  id:  " + setid + " found" );
            logger.debug( "   Photos in Set already loaded: " + photos.size() );

            return setid;
        }
        return null;
    }

    private String getUploadedTime( String filename, String basefilename )
    {

        String title = "";
        if ( basefilename.lastIndexOf( '.' ) > 0 )
            title = basefilename.substring( 0, basefilename.lastIndexOf( '.' ) );

        if ( filePhotos.containsKey( title ) )
        {
            Photo p = filePhotos.get( title );
            if ( p.getDatePosted() != null )
            {
                return ( smp.format( p.getDatePosted() ) );
            }
        }

        return "";
    }

    private String uploadfile( String filename, String inpTitle, String basefilename )
        throws Exception
    {
        String photoId;

        RequestContext rc = RequestContext.getRequestContext();

        if ( this.auth.getAuthStore() != null )
        {
            Auth auth = this.auth.getAuthStore().retrieve( Configuration.getNSid() );
            if ( auth == null )
            {
                this.auth.authorize();
            }
            else
            {
                rc.setAuth( auth );
            }
        }

        // PhotosetsInterface pi = flickr.getPhotosetsInterface();
        // PhotosInterface photoInt = flickr.getPhotosInterface();
        // Map<String, Collection> allPhotos = new HashMap<String, Collection>();
        /**
         * 1 : Public 2 : Friends only 3 : Family only 4 : Friends and Family 5 : Private
         **/
        int privacy = Configuration.getPrivacy();// this.auth.getPrivacy();

        UploadMetaData metaData = new UploadMetaData();

        if ( privacy == 1 )
            metaData.setPublicFlag( true );
        if ( privacy == 2 || privacy == 4 )
            metaData.setFriendFlag( true );
        if ( privacy == 3 || privacy == 4 )
            metaData.setFamilyFlag( true );

        if ( basefilename == null || basefilename.equals( "" ) )
            basefilename = filename; // "image.jpg";

        String title = basefilename;
        boolean setMimeType = true; // change during testing. Doesn't seem to be supported at this time in flickr.
        if ( setMimeType )
        {
            if ( basefilename.lastIndexOf( '.' ) > 0 )
            {
                title = basefilename.substring( 0, basefilename.lastIndexOf( '.' ) );
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
        logger.debug( " File : " + filename );
        logger.debug( " basefilename : " + basefilename );

        if ( inpTitle != null && !inpTitle.equals( "" ) )
        {
            title = inpTitle;
            logger.debug( " title : " + inpTitle );
            metaData.setTitle( title );
        } // flickr defaults the title field from file name.

        // UploadMeta is using String not Tag class.

        // Tags are getting mangled by yahoo stripping off the = , '.' and many other punctuation characters
        // and converting to lower case: use the raw tag field to find the real value for checking and
        // for download.
        if ( Configuration.getNoTags() )
        {
            List<String> tags = new ArrayList<String>();
            String tmp = basefilename;
            basefilename = makeSafeFilename( basefilename );
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

            File f = new File( filename );
            photoId = uploader.upload( f, metaData );

            logger.debug( " File : " + filename + " uploaded: photoId = " + photoId );
        }
        finally
        {

        }

        return ( photoId );
    }

    public void getPhotosetsInfo( String nsid )
    {

        PhotosetsInterface pi = flickr.getPhotosetsInterface();
        try
        {
            int setsPage = 1;
            while ( true )
            {
                Photosets photosets = pi.getList( nsid, 500, setsPage, null );
                Collection<Photoset> setsColl = photosets.getPhotosets();
                Iterator<Photoset> setsIter = setsColl.iterator();
                while ( setsIter.hasNext() )
                {
                    Photoset set = setsIter.next();
                    allSetsMap.put( set.getId(), set );

                    // 2 or more sets can in theory have the same name. !!!
                    ArrayList<String> setIdarr = setNameToId.get( set.getTitle() );
                    if ( setIdarr == null )
                    {
                        setIdarr = new ArrayList<String>();
                        setIdarr.add( new String( set.getId() ) );
                        setNameToId.put( set.getTitle(), setIdarr );
                    }
                    else
                    {
                        setIdarr.add( new String( set.getId() ) );
                    }
                }

                if ( setsColl.size() < 500 )
                {
                    break;
                }
                setsPage++;
            }
            logger.debug( " Sets retrieved: " + allSetsMap.size() );
            // all_sets_retrieved = true;
            // Print dups if any.

            Set<String> keys = setNameToId.keySet();
            Iterator<String> iter = keys.iterator();
            while ( iter.hasNext() )
            {
                String name = iter.next();
                ArrayList<String> setIdarr = setNameToId.get( name );
                if ( setIdarr != null && setIdarr.size() > 1 )
                {
                    System.out.println( "There is more than 1 set with this name : " + setNameToId.get( name ) );
                    for ( int j = 0; j < setIdarr.size(); j++ )
                    {
                        System.out.println( "           id: " + setIdarr.get( j ) );
                    }
                }
            }

        }
        catch ( FlickrException e )
        {
            e.printStackTrace();
        }
    }

    /**
     * The assumption here is that for a given set only unique file-names will be loaded and the title field can be
     * used. Later change to use the tags field ( OrigFileName) and strip off the suffix.
     * 
     * @param filename
     * @return
     */
    private boolean checkIfLoaded( String basefilename, String filename )
    {
        String title;
        if ( basefilename.lastIndexOf( '.' ) > 0 )
            title = basefilename.substring( 0, basefilename.lastIndexOf( '.' ) );
        else
            return false;

        if ( filePhotos.containsKey( title ) )
            return true;

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

    private void addPhotoToSet( String photoid, String setName, String setid, String basefilename )
        throws Exception
    {

        ArrayList<String> setIdarr;

        // all_set_maps.

        PhotosetsInterface psetsInterface = flickr.getPhotosetsInterface();

        Photoset set = null;

        if ( setid == null )
        {
            // In case it is a new photo-set.
            setIdarr = setNameToId.get( setName );
            if ( setIdarr == null )
            {
                // setIdarr should be null since we checked it getSetPhotos.
                // Create the new set.
                // set the setid .

                String description = "";
                set = psetsInterface.create( setName, description, photoid );
                setid = set.getId();

                setIdarr = new ArrayList<String>();
                setIdarr.add( new String( setid ) );
                setNameToId.put( setName, setIdarr );

                allSetsMap.put( set.getId(), set );
            }
        }
        else
        {
            set = allSetsMap.get( setid );
            psetsInterface.addPhoto( setid, photoid );
        }
        // Add to photos .

        // Add Photo to existing set.
        PhotosInterface photoInt = flickr.getPhotosInterface();
        Photo p = photoInt.getPhoto( photoid );
        if ( p != null )
        {
            photos.add( p );
            String title;
            if ( basefilename.lastIndexOf( '.' ) > 0 )
                title = basefilename.substring( 0, basefilename.lastIndexOf( '.' ) );
            else
                title = p.getTitle();
            filePhotos.put( title, p );
        }
    }

    private String makeSafeFilename( String input )
    {
        byte[] fname = input.getBytes();
        byte[] bad = new byte[] { '\\', '/', '"', '*' };
        byte replace = '_';
        for ( int i = 0; i < fname.length; i++ )
        {
            for ( byte element : bad )
            {
                if ( fname[i] == element )
                {
                    fname[i] = replace;
                }
            }
            if ( Configuration.getReplaceSpaces() && fname[i] == ' ' )
                fname[i] = '_';
        }
        return new String( fname );
    }

}
