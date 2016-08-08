package org.jflickrsync.main.index.photo;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.jflickrsync.main.Configuration;
import org.jflickrsync.main.index.album.AlbumFlickr;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photosets.Photoset;

import junit.framework.Assert;

public class PhotoFlickrTest
{
    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    /**
     * Folder where config is saved
     */
    private static File configFolder;

    /**
     * Folder where all photos are saved
     */
    private static File baseFolder;

    @BeforeClass
    public static void setup()
        throws IOException
    {
        configFolder = folder.newFolder( "configuration" );
        baseFolder = folder.newFolder( "base" );
        Configuration.getConfiguration( configFolder.getAbsolutePath() );
        Configuration.getConfiguration().put( Configuration.CONFIG_BASEPATH, baseFolder.getAbsolutePath() );
        Configuration.setBasePath( baseFolder.getAbsolutePath() );
    }

    @Test
    public void constructor()
        throws FlickrException, IOException
    {
        Flickr flickr = Mockito.mock( Flickr.class );
        Photo photoflickr = new Photo();
        photoflickr.setTitle( "photo1" );
        photoflickr.setOriginalFormat( "png" );
        Photoset photoset = new Photoset();
        photoset.setTitle( "album1" );

        PhotoFlickr photo = new PhotoFlickr( flickr, photoflickr );
        Assert.assertEquals( photo.getTitle(), "photo1" );
        Assert.assertEquals( photo.getAbsolutePath(),
                             baseFolder.getAbsolutePath() + File.separatorChar + "photo1.png" );

    }

    @Test
    public void testGetAbsolutePath()
    {
        Flickr flickr = Mockito.mock( Flickr.class );
        Photo photoflickr = new Photo();
        photoflickr.setTitle( "photo1" );
        photoflickr.setOriginalFormat( "png" );
        Photoset photoset = new Photoset();
        photoset.setTitle( "album1" );

        PhotoFlickr photo = new PhotoFlickr( flickr, photoflickr );
        Assert.assertEquals( photo.getTitle(), "photo1" );
        Assert.assertEquals( photo.getAbsolutePath(),
                             baseFolder.getAbsolutePath() + File.separatorChar + "photo1.png" );

        AlbumFlickr album = new AlbumFlickr( flickr, photoset );
        album.addPhoto( photo );

        Assert.assertEquals( photo.getAbsolutePath(), baseFolder.getAbsolutePath() + File.separatorChar + "album1"
            + File.separatorChar + "photo1.png" );
    }

    @Test
    public void testGetFilename()
    {
        Flickr flickr = Mockito.mock( Flickr.class );
        Photo photoflickr = new Photo();
        photoflickr.setTitle( "photo1" );
        photoflickr.setOriginalFormat( "png" );

        PhotoFlickr photo = new PhotoFlickr( flickr, photoflickr );

        Assert.assertEquals( photo.getFilename(), "photo1.png" );
    }

    @Test
    public void testDownloadFromFlickR()
        throws FlickrException, IOException
    {
        Flickr flickr = Mockito.mock( Flickr.class );
        PhotosInterface photoInt = Mockito.mock( PhotosInterface.class );
        Mockito.when( flickr.getPhotosInterface() ).thenReturn( photoInt );

        BufferedInputStream bis1 = new BufferedInputStream( new ByteArrayInputStream( new byte[] { 1, 2, 4, 5 } ) );
        BufferedInputStream bis2 = new BufferedInputStream( new ByteArrayInputStream( new byte[] { 1, 2 } ) );

        Photo flickrPhoto1 = new Photo();
        flickrPhoto1.setId( "id1" );
        flickrPhoto1.setSecret( "secret1" );
        flickrPhoto1.setTitle( "photo1" );
        flickrPhoto1.setOriginalFormat( "jpeg" );
        PhotoFlickr photo1 = new PhotoFlickr( flickr, flickrPhoto1 );

        Photo flickrPhoto2 = new Photo();
        flickrPhoto2.setId( "id2" );
        flickrPhoto2.setSecret( "secret2" );
        flickrPhoto2.setTitle( "photo2" );
        flickrPhoto2.setOriginalFormat( "png" );
        PhotoFlickr photo2 = new PhotoFlickr( flickr, flickrPhoto2 );

        Mockito.when( photoInt.getImageAsStream( Mockito.eq( flickrPhoto2 ), Mockito.anyInt() ) ).thenReturn( bis2 );
        Mockito.when( photoInt.getImageAsStream( Mockito.eq( flickrPhoto1 ), Mockito.anyInt() ) ).thenReturn( bis1 );

        File f1 = photo1.downloadFromFlickR();
        File f2 = photo2.downloadFromFlickR();

        Assert.assertTrue( f1.exists() );
        Assert.assertTrue( f2.exists() );
    }

    @Test
    public void testRemove()
        throws Exception
    {
        Flickr flickr = Mockito.mock( Flickr.class );
        PhotosInterface photoInt = Mockito.mock( PhotosInterface.class );
        Mockito.when( flickr.getPhotosInterface() ).thenReturn( photoInt );

        Photo photoflickr = new Photo();
        photoflickr.setTitle( "photo1" );
        photoflickr.setId( "id1" );
        photoflickr.setOriginalFormat( "png" );

        Mockito.when( photoInt.getInfo( Mockito.eq( "id1" ), Mockito.anyString() ) ).thenReturn( photoflickr );

        PhotoFlickr photo = new PhotoFlickr( flickr, photoflickr );

        boolean result = photo.remove();

        Assert.assertTrue( result );

        Mockito.verify( photoInt, Mockito.atLeastOnce() ).delete( Mockito.eq( "id1" ) );
    }

}
