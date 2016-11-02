package org.jflickrsync.main.index.album;

import java.io.File;

import org.jflickrsync.main.Configuration;
import org.jflickrsync.main.index.photo.Photo;
import org.jflickrsync.main.index.photo.PhotoMock;
import org.junit.Test;
import org.mockito.Mockito;

import junit.framework.Assert;

public class AlbumTest
{

    @Test
    public void testAddAndRemovePhoto()
        throws Exception
    {
        Album album = new AlbumMock( "test" );
        PhotoMock photo1 = new PhotoMock( "mock1" );
        PhotoMock photo2 = new PhotoMock( "mock2" );
        PhotoMock photo3 = new PhotoMock( "mock3" );
        PhotoMock photo4 = new PhotoMock( "mock4" );
        PhotoMock photo5 = new PhotoMock( "mock5" );
        album.addPhoto( photo1 );
        album.addPhoto( photo2 );
        album.addPhoto( photo3 );
        album.addPhoto( photo4 );
        album.addPhoto( photo5 );

        Assert.assertTrue( album.getPhotos().size() == 5 );
        Assert.assertEquals( album.getPhotos().get( 4 ), photo5 );

        album.removePhoto( photo3 );
        Assert.assertTrue( album.getPhotos().size() == 4 );
        Assert.assertEquals( album.getPhotos().get( 3 ), photo5 );
        Assert.assertEquals( album.getPhotos().get( 2 ), photo4 );
    }

    @Test
    public void testLocatePhoto()
        throws Exception
    {
        Album album = new AlbumMock( "test" );
        PhotoMock photo1 = new PhotoMock( "mock1" );
        PhotoMock photo2 = new PhotoMock( "mock2" );
        PhotoMock photo3 = new PhotoMock( "mock3" );
        PhotoMock photo4 = new PhotoMock( "mock4" );
        PhotoMock photo5 = new PhotoMock( "mock5" );
        album.addPhoto( photo1 );
        album.addPhoto( photo2 );
        album.addPhoto( photo3 );
        album.addPhoto( photo4 );
        album.addPhoto( photo5 );

        Photo locate = album.locatePhoto( "mock5" );
        Assert.assertEquals( locate, photo5 );

        locate = album.locatePhoto( "mock4" );
        Assert.assertEquals( locate, photo4 );

        locate = album.locatePhoto( "mock3" );
        Assert.assertEquals( locate, photo3 );

        locate = album.locatePhoto( "mock2" );
        Assert.assertEquals( locate, photo2 );

        locate = album.locatePhoto( "mock1" );
        Assert.assertEquals( locate, photo1 );

        locate = album.locatePhoto( "mock6" );
        Assert.assertNull( locate );
    }

    @Test
    public void testEquals()
    {
        Album album1 = new AlbumMock( "test1" );
        Album album2 = new AlbumMock( "test2" );
        Album album3 = new AlbumMock( "test3" );
        Album album4 = new AlbumMock( "test1" );
        Album album5 = new AlbumMock( "test2" );

        Assert.assertEquals( album1, album4 );
        Assert.assertEquals( album2, album5 );

        Assert.assertTrue( !album1.equals( album2 ) );
        Assert.assertTrue( !album2.equals( album3 ) );
    }

    @Test
    public void testGetAbsolutePathFile()
    {
        Album album1 = new AlbumMock( "test1" );

        File f = album1.getAbsolutePathFile();
        Assert.assertEquals( f.getAbsolutePath(),
                             Configuration.getBasePath() + File.separatorChar + album1.getTitle() );
    }

    @Test
    public void testChange()
        throws Exception
    {
        AlbumListener listener = Mockito.mock( AlbumListener.class );
        Album album = new AlbumMock( "test1" );
        album.setListener( listener );
        PhotoMock photo1 = new PhotoMock( "mock1" );
        album.addPhoto( photo1 );
        PhotoMock photo2 = new PhotoMock( "mock2" );
        album.addPhoto( photo2 );

        Mockito.verify( listener, Mockito.times( 2 ) ).albumChange();
    }
}
