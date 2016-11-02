package org.jflickrsync.main.index.album;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.jflickrsync.main.Configuration;
import org.jflickrsync.main.index.photo.PhotoFlickr;
import org.jflickrsync.main.index.photo.PhotoMock;
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
import com.flickr4java.flickr.photosets.PhotosetsInterface;

import junit.framework.Assert;

public class AlbumFlickrTest
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
        Configuration.resetConfiguration();
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
        File ffolder = new File( baseFolder.getAbsolutePath() + File.separatorChar + "album1" );

        Flickr flickr = Mockito.mock( Flickr.class );
        Photoset photoset = new Photoset();
        photoset.setTitle( "album1" );

        AlbumFlickr album = new AlbumFlickr( flickr, photoset );
        Assert.assertEquals( album.getTitle(), "album1" );
        Assert.assertEquals( album.getAbsolutePathFile(), ffolder );
    }

    @Test
    public void testAddPhoto()
        throws Exception
    {
        Flickr flickr = Mockito.mock( Flickr.class );
        Photoset photoset = Mockito.mock( Photoset.class );
        photoset.setTitle( "album1" );
        Album album = new AlbumFlickr( flickr, photoset );
        PhotoMock photo1 = new PhotoMock( "mock1" );
        PhotoMock photo2 = new PhotoMock( "mock2" );
        album.addPhoto( photo1 );
        album.addPhoto( photo2 );

        Assert.assertEquals( album.getPhotos().size(), 0 );

        Photo photoflickr = Mockito.mock( Photo.class );
        PhotoFlickr photo3 = new PhotoFlickr( flickr, photoflickr );
        album.addPhoto( photo3 );

        Assert.assertEquals( album.getPhotos().size(), 1 );
    }

    @Test
    public void testDownloadFromFlickR()
        throws FlickrException, IOException
    {
        Flickr flickr = Mockito.mock( Flickr.class );
        PhotosInterface photoInt = Mockito.mock( PhotosInterface.class );
        Mockito.when( flickr.getPhotosInterface() ).thenReturn( photoInt );
        Photoset photoset = new Photoset();
        photoset.setTitle( "album1" );
        AlbumFlickr album = new AlbumFlickr( flickr, photoset );

        File fdownloaded = album.downloadFromFlickR();

        Assert.assertTrue( fdownloaded.getAbsolutePath().equals( Configuration.getBasePath() + File.separatorChar
            + "album1" ) );

        BufferedInputStream bis1 = new BufferedInputStream( new ByteArrayInputStream( new byte[] { 1, 2, 4, 5 } ) );
        BufferedInputStream bis2 = new BufferedInputStream( new ByteArrayInputStream( new byte[] { 1, 2 } ) );

        Photo flickrPhoto1 = new Photo();
        flickrPhoto1.setId( "id1" );
        flickrPhoto1.setSecret( "secret1" );
        flickrPhoto1.setTitle( "photo1" );
        flickrPhoto1.setOriginalFormat( "jpeg" );
        PhotoFlickr photo1 = new PhotoFlickr( flickr, flickrPhoto1 );
        album.addPhoto( photo1 );

        Photo flickrPhoto2 = new Photo();
        flickrPhoto2.setId( "id2" );
        flickrPhoto2.setSecret( "secret2" );
        flickrPhoto2.setTitle( "photo2" );
        flickrPhoto2.setOriginalFormat( "png" );
        PhotoFlickr photo2 = new PhotoFlickr( flickr, flickrPhoto2 );
        album.addPhoto( photo2 );

        Mockito.when( photoInt.getImageAsStream( Mockito.eq( flickrPhoto2 ), Mockito.anyInt() ) ).thenReturn( bis2 );
        Mockito.when( photoInt.getImageAsStream( Mockito.eq( flickrPhoto1 ), Mockito.anyInt() ) ).thenReturn( bis1 );

        fdownloaded = album.downloadFromFlickR();

        String[] list = fdownloaded.list();
        Assert.assertEquals( list[0], "photo1.jpeg" );
        Assert.assertEquals( list[1], "photo2.png" );
    }

    @Test
    public void testRemove()
        throws Exception
    {
        Flickr flickr = Mockito.mock( Flickr.class );
        Photoset photoset = Mockito.mock( Photoset.class );
        photoset.setTitle( "album1" );
        AlbumFlickr album = new AlbumFlickr( flickr, photoset );

        boolean result = album.remove();

        Assert.assertTrue( result );
    }

    @Test
    public void testAddPhotoToFlickrSet()
        throws FlickrException
    {
        Flickr flickr = Mockito.mock( Flickr.class );
        PhotosetsInterface photoInt = Mockito.mock( PhotosetsInterface.class );
        Mockito.when( flickr.getPhotosetsInterface() ).thenReturn( photoInt );
        Photoset photoset = new Photoset();
        photoset.setId( "setid1" );
        photoset.setTitle( "album1" );
        AlbumFlickr album = new AlbumFlickr( flickr, photoset );

        Photo flickrPhoto1 = new Photo();
        flickrPhoto1.setId( "photoid1" );
        flickrPhoto1.setSecret( "secret1" );
        flickrPhoto1.setTitle( "photo1" );
        flickrPhoto1.setOriginalFormat( "jpeg" );
        PhotoFlickr photo1 = new PhotoFlickr( flickr, flickrPhoto1 );

        boolean result = album.addPhotoToFlickrSet( photo1 );

        Assert.assertTrue( result );

        Mockito.verify( photoInt, Mockito.atLeast( 1 ) ).addPhoto( "setid1", "photoid1" );
    }

}
