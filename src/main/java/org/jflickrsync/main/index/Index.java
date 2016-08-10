package org.jflickrsync.main.index;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jflickrsync.main.Configuration;
import org.jflickrsync.main.index.album.Album;
import org.jflickrsync.main.index.album.AlbumFile;
import org.jflickrsync.main.index.album.AlbumFlickr;
import org.jflickrsync.main.index.album.AlbumListener;
import org.jflickrsync.main.index.album.AlbumMock;
import org.jflickrsync.main.index.album.AlbumProvider;
import org.jflickrsync.main.index.photo.Photo;
import org.jflickrsync.main.index.photo.PhotoFile;
import org.jflickrsync.main.index.photo.PhotoFlickr;
import org.jflickrsync.main.index.photo.PhotoMock;
import org.jflickrsync.main.index.photo.PhotoProvider;
import org.jflickrsync.main.util.Util;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photos.PhotoList;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.PhotosetsInterface;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import lombok.Getter;
import lombok.Setter;

public class Index
    implements AlbumListener
{

    private static final Logger logger = Logger.getLogger( Index.class );

    /**
     * File name of the index to be store (can be null)
     */
    @Getter
    @Setter
    @Expose
    private String storeFileNameIndex;

    /**
     * Optional path of the index info.
     */
    @Expose
    @Getter
    @Setter
    private String path = "";

    /**
     * list of photos without set associated
     */
    @Expose
    private List<Photo> photosNoInAlbums = new ArrayList<>();

    /**
     * List of albums
     */
    @Expose
    private List<Album> albums = new ArrayList<>();

    public int photoNoInAlbums_size()
    {
        return this.photosNoInAlbums.size();
    }

    public Photo photoNoInAlbums_get( int index )
    {
        return this.photosNoInAlbums.get( index );
    }

    public int photoNoInAlbums_indexOf( Photo photo )
    {
        return this.photosNoInAlbums.indexOf( photo );
    }

    public int albums_size()
    {
        return this.albums.size();
    }

    public Album albums_get( int index )
    {
        return this.albums.get( index );
    }

    public int albums_indexOf( Album album )
    {
        return this.albums.indexOf( album );
    }

    /**
     * Removes an existing album from the list and phisically
     * 
     * @param album
     * @throws Exception
     */
    public synchronized void album_remove( Album album )
        throws Exception
    {
        int index = albums.indexOf( album );
        if ( index >= 0 )
        {
            Album ialbum = albums.get( index );
            ialbum.remove();
            ialbum.setListener( null );
            albums.remove( index );
            save();
        }
    }

    /**
     * Check if a certain photo or album exists based on the absolute path of the resource. This method try to check if
     * it is an album or a photo and return if it exists in this index.
     * 
     * @param absPath {@link String} absolute path to check
     * @return boolean true->it exists
     */
    public boolean existPhotoOrAlbum( String absPath )
    {
        File fabspath = new File( absPath );
        String relPath = Util.getRelativePath( absPath );
        if ( fabspath.exists() )
        {
            if ( fabspath.isDirectory() )
            {
                Album album = locateAlbum( relPath );
                return album != null;
            }
            else
            {
                Photo photo = locatePhoto( absPath );
                return photo != null;
            }
        }
        else
        {
            // The resource doesn't exist fisically.. lets search
            int indexSep = relPath.indexOf( File.separatorChar );
            if ( indexSep >= 0 )
            {
                // a photo inside album
                Photo photo = locatePhoto( absPath );
                return photo != null;
            }
            else
            {
                // an album or a photo
                Photo photo = locatePhoto( absPath );
                if ( photo == null )
                {
                    Album album = locateAlbum( relPath );
                    return album != null;
                }
                else
                {
                    return true;
                }
            }
        }
    }

    /**
     * Search a photo inside this index by the title of the photo
     * 
     * @param photo {@link Photo} with a correct title
     * @return {@link Photo} photo found or null
     */
    public Photo locatePhoto( Photo photo )
    {
        Album palbum = photo.getAlbum();
        if ( palbum != null )
        {
            int indexa = albums.indexOf( palbum );
            if ( indexa >= 0 )
            {
                Album album = albums.get( indexa );
                int indexp = album.getPhotos().indexOf( photo );
                if ( indexp >= 0 )
                {
                    return album.getPhotos().get( indexp );
                }
            }
        }
        else
        {
            int index = photosNoInAlbums.indexOf( photo );
            if ( index >= 0 )
            {
                return photosNoInAlbums.get( index );
            }
        }

        return null;
    }

    /**
     * Locate a photo by the absolute path of the file representing it
     * 
     * @param absolutePath {@link String} absolute path of the photo
     * @return {@link Photo} photo located or null
     */
    public Photo locatePhoto( String absolutePath )
    {
        String relativePath = Util.getRelativePath( absolutePath );
        int index = relativePath.indexOf( File.separatorChar );
        if ( index > 0 )
        {
            // it is contained inside a folder
            String folder = relativePath.substring( 0, index );
            Album album = locateAlbum( folder );
            if ( album != null )
            {
                String file = relativePath.substring( index + 1 );

                int indexdot = file.lastIndexOf( '.' );
                if ( indexdot >= 0 )
                {
                    file = file.substring( 0, indexdot );
                }

                Photo photo = album.locatePhoto( file );
                return photo;
            }
        }
        else
        {
            int indexDot = relativePath.lastIndexOf( '.' );
            String title = relativePath;
            if ( indexDot >= 0 )
            {
                title = title.substring( 0, indexDot );
            }
            int indexphoto = this.photosNoInAlbums.indexOf( new PhotoMock( title ) );
            if ( indexphoto >= 0 )
            {
                return this.photosNoInAlbums.get( indexphoto );
            }
        }

        return null;
    }

    /**
     * search an album by title
     * 
     * @param title {@link String} title of the album
     * @return {@link Album} album found
     */
    public Album locateAlbum( String title )
    {
        Album album = new AlbumMock( title );
        return locateAlbum( album );
    }

    /**
     * search an album by title
     * 
     * @param title {@link String} title of the album
     * @return {@link Album} album found
     */
    public Album locateAlbum( Album album )
    {
        int index = albums.indexOf( album );
        if ( index >= 0 )
        {
            return albums.get( index );
        }

        return null;
    }

    /**
     * Save the index to the index.json file at the configuration folder
     * 
     * @throws IOException
     */
    private void storeIndex( String indexFileName )
        throws IOException
    {
        // Gson gson = new Gson();
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.excludeFieldsWithoutExposeAnnotation().create();

        String json = gson.toJson( this );
        File file = new File( Configuration.getConfigurationFolderAbsolutePath() + File.separatorChar + indexFileName );
        if ( file.exists() )
        {
            file.delete();
        }
        FileOutputStream fos = new FileOutputStream( file );
        fos.write( json.getBytes() );
        fos.close();
    }

    /**
     * Load an index file
     * 
     * @return {@link Index} index loaded
     * @throws IOException
     */
    public static Index load( String filenameIndex )
        throws IOException
    {
        File f = new File( Configuration.getConfigurationFolderAbsolutePath() + File.separatorChar + filenameIndex );
        if ( f.exists() )
        {
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter( Album.class, new AlbumProvider() );
            builder.registerTypeAdapter( Photo.class, new PhotoProvider() );
            Gson gson = builder.excludeFieldsWithoutExposeAnnotation().create();

            List<String> lines = Files.readAllLines( Paths.get( f.getAbsolutePath() ), StandardCharsets.UTF_8 );
            String content = Joiner.on( "\n" ).join( lines );
            Index index = gson.fromJson( content, Index.class );
            return index;
        }
        else
        {
            return null;
        }
    }

    /**
     * Add a new photo to the list of photos not inside albums
     * 
     * @param photo {@link Photo}
     * @throws IOException
     */
    public synchronized void photoNoInAlbums_add( Photo photo )
        throws IOException
    {
        this.photosNoInAlbums.add( photo );
        save();
    }

    /**
     * Remove an existing photo to the list of photos not inside albums
     * 
     * @param photo {@link Photo}
     * @throws IOException
     */
    public synchronized void photoNoInAlbums_remove( Photo photo )
        throws IOException
    {
        this.photosNoInAlbums.remove( photo );
        save();
    }

    /**
     * Add a new album to the list of albums
     * 
     * @param album {@link Album} new album to add
     * @throws IOException
     */
    public synchronized void albums_add( Album album )
        throws IOException
    {
        this.albums.add( album );
        album.setListener( this );
        save();
    }

    /**
     * Remove an album from the list
     * 
     * @param index
     * @throws IOException
     */
    public synchronized void albums_remove( int index )
        throws IOException
    {
        this.albums.remove( index );
        save();
    }

    public synchronized void save()
        throws IOException
    {
        if ( this.storeFileNameIndex != null )
        {
            this.storeIndex( this.storeFileNameIndex );
        }
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( obj instanceof Index )
        {
            Index index = (Index) obj;
            if ( index.albums_size() == albums_size() )
            {
                for ( int i = 0; i < index.albums_size(); i++ )
                {
                    Album album1 = index.albums_get( i );
                    int indexAlbum2 = albums_indexOf( album1 );
                    if ( indexAlbum2 < 0 )
                    {
                        return false;
                    }
                    Album album2 = albums_get( indexAlbum2 );
                    if ( !album1.equals( album2 ) )
                    {
                        return false;
                    }
                    if ( album1.getPhotos().size() != album2.getPhotos().size() )
                    {
                        return false;
                    }
                    for ( int j = 0; j < album1.getPhotos().size(); j++ )
                    {
                        Photo photo1 = album1.getPhotos().get( j );
                        int indexPhoto2 = album2.getPhotos().indexOf( photo1 );
                        if ( indexPhoto2 < 0 )
                        {
                            return false;
                        }
                        Photo photo2 = album2.getPhotos().get( indexPhoto2 );
                        if ( !photo1.equals( photo2 ) )
                        {
                            return false;
                        }
                    }
                }
            }
            else
            {
                return false;
            }

            if ( index.photoNoInAlbums_size() == photoNoInAlbums_size() )
            {
                for ( int i = 0; i < index.photoNoInAlbums_size(); i++ )
                {
                    Photo photo1 = index.photoNoInAlbums_get( i );
                    int indexPhoto2 = photoNoInAlbums_indexOf( photo1 );
                    if ( indexPhoto2 < 0 )
                    {
                        return false;
                    }
                    Photo photo2 = photoNoInAlbums_get( indexPhoto2 );
                    if ( !photo1.equals( photo2 ) )
                    {
                        return false;
                    }
                }
            }
            else
            {
                return false;
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Get all the files at local folder (as albums and photos)
     * 
     * @param spath {@link String} base path to search
     * @return list of albums found
     * @throws Exception
     */
    public void indexLocal( String spath )
        throws Exception
    {
        logger.info( "Indexing Local info from filesystem" );
        this.path = spath;
        List<Album> albums = new ArrayList<>();

        File fpath = new File( spath );
        if ( !fpath.exists() )
        {
            fpath.mkdirs();
        }

        String[] albumdirs = fpath.list();
        for ( int i = 0; i < albumdirs.length; i++ )
        {
            File f = new File( spath + File.separatorChar + albumdirs[i] );
            if ( f.isDirectory() )
            {
                Album album = new AlbumFile( f );
                album.setTitle( f.getName() );
                String[] photofiles = f.list();
                for ( int j = 0; j < photofiles.length; j++ )
                {
                    File fphoto =
                        new File( spath + File.separatorChar + f.getName() + File.separatorChar + photofiles[j] );
                    Photo photo = new PhotoFile( fphoto );
                    int index = fphoto.getName().lastIndexOf( '.' );
                    if ( index >= 0 )
                    {
                        photo.setTitle( fphoto.getName().substring( 0, index ) );
                    }
                    else
                    {
                        photo.setTitle( fphoto.getName() );
                    }
                    album.addPhoto( photo );
                }
                albums.add( album );
            }
            else
            {
                File fphoto = new File( spath + File.separatorChar + albumdirs[i] );
                Photo photo = new PhotoFile( fphoto );
                int index = fphoto.getName().lastIndexOf( '.' );
                if ( index >= 0 )
                {
                    photo.setTitle( fphoto.getName().substring( 0, index ) );
                }
                else
                {
                    photo.setTitle( fphoto.getName() );
                }
                photosNoInAlbums.add( photo );
            }
        }

        this.albums = albums;
    }

    /**
     * Get all the albums and photos found in the flickr server
     * 
     * @param flickr {@link Flickr}
     * @return list of albums found
     * @throws FlickrException
     */
    public void indexFlickrServer( Flickr flickr )
        throws FlickrException
    {
        logger.info( "Indexing server info from flickr" );

        // we need to get all the info from server. We need to know what is there
        String nsid = Configuration.getNSid();
        PhotosetsInterface pi = flickr.getPhotosetsInterface();
        PhotosInterface photoInt = flickr.getPhotosInterface();
        // Map<String, PhotoList<com.flickr4java.flickr.photos.Photo>> allSetPhotos =
        // new HashMap<String, PhotoList<com.flickr4java.flickr.photos.Photo>>();

        // first we get the fotos which are not in sets
        int page = 1;
        int pages = 1;
        int maxPhotos = 500;
        while ( pages >= page )
        {
            PhotoList<com.flickr4java.flickr.photos.Photo> photos = photoInt.getNotInSet( maxPhotos, page );
            pages = photos.getPages();
            // allSetPhotos.put( "", photos );
            for ( int i = 0; i < photos.size(); i++ )
            {
                com.flickr4java.flickr.photos.Photo photoi = (com.flickr4java.flickr.photos.Photo) photos.get( i );
                PhotoFlickr photo = new PhotoFlickr( flickr, photoi );
                photo.setTitle( photoi.getTitle() );
                photo.setId( photoi.getId() );
                photosNoInAlbums.add( photo );
            }
            page++;
        }

        // after we get all the sets and photos in those sets
        Iterator<Photoset> sets = pi.getList( nsid ).getPhotosets().iterator();

        List<Album> albums = new ArrayList<>();
        while ( sets.hasNext() )
        {
            Photoset set = (Photoset) sets.next();
            AlbumFlickr album = new AlbumFlickr( flickr, set );
            album.setTitle( set.getTitle() );

            page = 1;
            pages = 1;
            maxPhotos = 500;
            while ( pages >= page )
            {
                try
                {
                    PhotoList<com.flickr4java.flickr.photos.Photo> photos =
                        pi.getPhotos( set.getId(), maxPhotos, page );
                    pages = photos.getPages();
                    // allSetPhotos.put( set.getTitle(), photos );
                    for ( int i = 0; i < photos.size(); i++ )
                    {
                        com.flickr4java.flickr.photos.Photo photoi =
                            (com.flickr4java.flickr.photos.Photo) photos.get( i );
                        PhotoFlickr photo = new PhotoFlickr( flickr, photoi );
                        photo.setTitle( photoi.getTitle() );
                        photo.setId( photoi.getId() );
                        album.addPhoto( photo );
                    }
                    page++;
                }
                catch ( Exception e )
                {
                    // photo set not found!!
                    break;
                }
            }
            albums.add( album );
        }

        this.albums = albums;
    }

    @Override
    public synchronized void albumChange()
        throws IOException
    {
        save();
    }
}
