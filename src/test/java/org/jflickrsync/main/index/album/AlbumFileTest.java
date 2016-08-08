package org.jflickrsync.main.index.album;

import java.io.File;
import java.io.IOException;

import org.jflickrsync.main.Configuration;
import org.jflickrsync.main.index.photo.PhotoFile;
import org.jflickrsync.main.index.photo.PhotoMock;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.PhotosetsInterface;

import junit.framework.Assert;

public class AlbumFileTest
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
    public void testAddPhoto()
        throws Exception
    {
        Album album = new AlbumFile( new File( "" ) );
        PhotoMock photo1 = new PhotoMock( "mock1" );
        PhotoMock photo2 = new PhotoMock( "mock2" );
        album.addPhoto( photo1 );
        album.addPhoto( photo2 );

        Assert.assertEquals( album.getPhotos().size(), 0 );

        PhotoFile photo3 = new PhotoFile( new File( "" ) );
        album.addPhoto( photo3 );

        Assert.assertEquals( album.getPhotos().size(), 1 );
    }

    @Test
    public void constructor()
        throws FlickrException, IOException
    {
        File ffolder = new File( baseFolder.getAbsolutePath() + File.separatorChar + "album1" );
        ffolder.mkdirs();

        AlbumFile album = new AlbumFile( ffolder );
        Assert.assertEquals( album.getTitle(), "album1" );
        Assert.assertEquals( album.getAbsolutePathFile(), ffolder );
    }

    @Test
    public void testUploadToFlickr()
        throws FlickrException, IOException
    {
        File ffolder = new File( baseFolder.getAbsolutePath() + File.separatorChar + "album1" );
        ffolder.mkdirs();

        Flickr flickr = Mockito.mock( Flickr.class );
        PhotosetsInterface photoint = Mockito.mock( PhotosetsInterface.class );
        Mockito.when( flickr.getPhotosetsInterface() ).thenReturn( photoint );
        Photoset set = new Photoset();
        set.setId( "id" );
        Mockito.when( photoint.create( Mockito.eq( "album1" ), Mockito.any( String.class ),
                                       Mockito.eq( "id" ) ) ).thenReturn( set );

        AlbumFile album = new AlbumFile( ffolder );
        Photoset setreturned = album.uploadToFlickr( flickr, "id" );

        Assert.assertTrue( setreturned.getTitle().equals( "album1" ) );
        Assert.assertTrue( setreturned.getId().equals( "id" ) );
    }

    @Test
    public void testRemove()
        throws Exception
    {
        File ffolder = new File( baseFolder.getAbsolutePath() + File.separatorChar + "album1" );
        ffolder.mkdirs();

        AlbumFile album = new AlbumFile( ffolder );

        Assert.assertTrue( ffolder.exists() );

        album.remove();

        Assert.assertTrue( !ffolder.exists() );
    }

}
