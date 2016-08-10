package org.jflickrsync.main.index;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jflickrsync.main.Configuration;
import org.jflickrsync.main.index.album.Album;
import org.jflickrsync.main.index.album.AlbumMock;
import org.jflickrsync.main.index.photo.Photo;
import org.jflickrsync.main.index.photo.PhotoMock;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.photos.PhotoList;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.Photosets;
import com.flickr4java.flickr.photosets.PhotosetsInterface;

import junit.framework.Assert;

public class IndexTest
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
        Configuration.resetConfiguration();
        Configuration.getConfiguration( configFolder.getAbsolutePath() );
        Configuration.getConfiguration().put( Configuration.CONFIG_BASEPATH, baseFolder.getAbsolutePath() );
        Configuration.setBasePath( baseFolder.getAbsolutePath() );
    }

    @Test
    public void testAlbumRemove()
        throws Exception
    {
        Index index = new Index();

        Album album1 = new AlbumMock( "album1" );
        Album album2 = new AlbumMock( "album3" );
        Album album3 = new AlbumMock( "album2" );

        index.albums_add( album1 );
        index.albums_add( album2 );
        index.albums_add( album3 );

        Assert.assertEquals( index.albums_size(), 3 );

        index.album_remove( album2 );

        Assert.assertEquals( index.albums_size(), 2 );

        Assert.assertSame( index.albums_get( 0 ), album1 );
        Assert.assertSame( index.albums_get( 1 ), album3 );
    }

    @Test
    public void testExistPhotoOrAlbum()
        throws Exception
    {
        Index index = new Index();

        Album album1 = new AlbumMock( "album1" );
        Album album2 = new AlbumMock( "album2" );
        Album album3 = new AlbumMock( "album3" );

        index.albums_add( album1 );
        index.albums_add( album2 );
        index.albums_add( album3 );

        Photo photo1 = new PhotoMock( "photo1" );
        Photo photo2 = new PhotoMock( "photo2" );
        Photo photo3 = new PhotoMock( "photo3" );

        album1.addPhoto( photo1 );
        album3.addPhoto( photo2 );
        album3.addPhoto( photo3 );

        Photo photo4 = new PhotoMock( "photo4" );
        Photo photo5 = new PhotoMock( "photo5" );

        index.photoNoInAlbums_add( photo4 );
        index.photoNoInAlbums_add( photo5 );

        String pathAlbum1 = Configuration.getBasePath() + File.separatorChar + "album1";
        String pathAlbum2 = Configuration.getBasePath() + File.separatorChar + "album2";
        String pathAlbum3 = Configuration.getBasePath() + File.separatorChar + "album3";
        Assert.assertTrue( index.existPhotoOrAlbum( pathAlbum1 ) );
        Assert.assertTrue( index.existPhotoOrAlbum( pathAlbum2 ) );
        Assert.assertTrue( index.existPhotoOrAlbum( pathAlbum3 ) );

        String pathPhoto1 =
            Configuration.getBasePath() + File.separatorChar + "album1" + File.separatorChar + "photo1.jpeg";
        String pathPhoto2 =
            Configuration.getBasePath() + File.separatorChar + "album3" + File.separatorChar + "photo2.png";
        String pathPhoto3 =
            Configuration.getBasePath() + File.separatorChar + "album3" + File.separatorChar + "photo3.bmp";
        Assert.assertTrue( index.existPhotoOrAlbum( pathPhoto1 ) );
        Assert.assertTrue( index.existPhotoOrAlbum( pathPhoto2 ) );
        Assert.assertTrue( index.existPhotoOrAlbum( pathPhoto3 ) );

        String pathPhoto4 =
            Configuration.getBasePath() + File.separatorChar + "album4" + File.separatorChar + "photo1.bmp";
        Assert.assertTrue( !index.existPhotoOrAlbum( pathPhoto4 ) );
    }

    @Test
    public void testLocatePhoto()
        throws Exception
    {
        Index index = new Index();

        Album album1 = new AlbumMock( "album1" );
        Album album2 = new AlbumMock( "album2" );
        Album album3 = new AlbumMock( "album3" );

        index.albums_add( album1 );
        index.albums_add( album2 );
        index.albums_add( album3 );

        Photo photo1 = new PhotoMock( "photo1" );
        Photo photo2 = new PhotoMock( "photo2" );
        Photo photo3 = new PhotoMock( "photo3" );

        album1.addPhoto( photo1 );
        album3.addPhoto( photo2 );
        album3.addPhoto( photo3 );

        Photo photo4 = new PhotoMock( "photo4" );
        Photo photo5 = new PhotoMock( "photo5" );

        index.photoNoInAlbums_add( photo4 );
        index.photoNoInAlbums_add( photo5 );

        Photo photo6 = new PhotoMock( "photo6" );
        Photo photo7 = new PhotoMock( "photo7" );

        Assert.assertEquals( index.locatePhoto( photo1 ), photo1 );
        Assert.assertEquals( index.locatePhoto( photo2 ), photo2 );
        Assert.assertEquals( index.locatePhoto( photo3 ), photo3 );
        Assert.assertEquals( index.locatePhoto( photo4 ), photo4 );
        Assert.assertEquals( index.locatePhoto( photo5 ), photo5 );

        Assert.assertNull( index.locatePhoto( photo6 ) );
        Assert.assertNull( index.locatePhoto( photo7 ) );
    }

    @Test
    public void testlocatePhotoByAbsolutePath()
        throws Exception
    {
        Index index = new Index();

        Album album1 = new AlbumMock( "album1" );
        Album album2 = new AlbumMock( "album2" );
        Album album3 = new AlbumMock( "album3" );

        index.albums_add( album1 );
        index.albums_add( album2 );
        index.albums_add( album3 );

        Photo photo1 = new PhotoMock( "photo1" );
        Photo photo2 = new PhotoMock( "photo2" );
        Photo photo3 = new PhotoMock( "photo3" );

        album1.addPhoto( photo1 );
        album3.addPhoto( photo2 );
        album3.addPhoto( photo3 );

        Photo photo4 = new PhotoMock( "photo4" );
        Photo photo5 = new PhotoMock( "photo5" );

        index.photoNoInAlbums_add( photo4 );
        index.photoNoInAlbums_add( photo5 );

        String pathPhoto1 =
            Configuration.getBasePath() + File.separatorChar + "album1" + File.separatorChar + "photo1.jpeg";
        String pathPhoto2 =
            Configuration.getBasePath() + File.separatorChar + "album3" + File.separatorChar + "photo2.png";
        String pathPhoto3 =
            Configuration.getBasePath() + File.separatorChar + "album3" + File.separatorChar + "photo3.bmp";
        Assert.assertEquals( index.locatePhoto( pathPhoto1 ), photo1 );
        Assert.assertEquals( index.locatePhoto( pathPhoto2 ), photo2 );
        Assert.assertEquals( index.locatePhoto( pathPhoto3 ), photo3 );

        String pathPhoto4 =
            Configuration.getBasePath() + File.separatorChar + "album4" + File.separatorChar + "photo1.bmp";
        Assert.assertNull( index.locatePhoto( pathPhoto4 ) );
    }

    @Test
    public void testLocateAlbum()
        throws Exception
    {
        Index index = new Index();

        Album album1 = new AlbumMock( "album1" );
        Album album2 = new AlbumMock( "album2" );
        Album album3 = new AlbumMock( "album3" );
        Album album4 = new AlbumMock( "album4" );
        Album album5 = new AlbumMock( "album5" );

        index.albums_add( album1 );
        index.albums_add( album2 );
        index.albums_add( album3 );

        Assert.assertEquals( index.locateAlbum( "album1" ), album1 );
        Assert.assertEquals( index.locateAlbum( "album2" ), album2 );
        Assert.assertEquals( index.locateAlbum( "album3" ), album3 );
        Assert.assertNull( index.locateAlbum( "album4" ) );
        Assert.assertNull( index.locateAlbum( "album5" ) );

        Assert.assertEquals( index.locateAlbum( album1 ), album1 );
        Assert.assertEquals( index.locateAlbum( album2 ), album2 );
        Assert.assertEquals( index.locateAlbum( album3 ), album3 );
        Assert.assertNull( index.locateAlbum( album4 ) );
        Assert.assertNull( index.locateAlbum( album5 ) );
    }

    @Test
    public void testStoreLoadAndEquals()
        throws Exception
    {
        Index index = new Index();
        Album album1 = new AlbumMock( "album1" );
        Album album2 = new AlbumMock( "album2" );
        Album album3 = new AlbumMock( "album3" );
        index.albums_add( album1 );
        index.albums_add( album2 );
        index.albums_add( album3 );
        Photo photo1 = new PhotoMock( "photo1" );
        Photo photo2 = new PhotoMock( "photo2" );
        Photo photo3 = new PhotoMock( "photo3" );
        album1.addPhoto( photo1 );
        album3.addPhoto( photo2 );
        album3.addPhoto( photo3 );
        Photo photo4 = new PhotoMock( "photo4" );
        Photo photo5 = new PhotoMock( "photo5" );
        index.photoNoInAlbums_add( photo4 );
        index.photoNoInAlbums_add( photo5 );

        File last = new File( Configuration.getConfigurationFolderAbsolutePath() + File.separatorChar
            + "finalStoreFileName.index" );

        Assert.assertTrue( !last.exists() );
        index.setStoreFileNameIndex( "finalStoreFileName.index" );
        index.save();
        Assert.assertTrue( last.exists() );

        Index newIndex = Index.load( "finalStoreFileName.index" );
        Assert.assertEquals( index, newIndex );

        Photo photo6 = new PhotoMock( "photo6" );
        album2.addPhoto( photo6 );
        Assert.assertTrue( !index.equals( newIndex ) );
        album2.removePhoto( photo6 );
        Assert.assertTrue( index.equals( newIndex ) );
        index.photoNoInAlbums_add( photo6 );
        Assert.assertTrue( !index.equals( newIndex ) );
        index.photoNoInAlbums_remove( photo6 );
        Assert.assertTrue( index.equals( newIndex ) );
        Album album4 = new AlbumMock( "album4" );
        index.albums_add( album4 );
        Assert.assertTrue( !index.equals( newIndex ) );
    }

    @Test
    public void testIndexLocal()
        throws Exception
    {
        createFolderAlbum( "album1" );
        createFolderAlbum( "album2" );
        createFolderAlbum( "album3" );
        createFilePhoto( "album1", "photo1" );
        createFilePhoto( "album3", "photo2" );
        createFilePhoto( "album3", "photo3" );
        createFilePhoto( null, "photo4" );
        createFilePhoto( null, "photo5" );
        Index index = new Index();
        index.indexLocal( Configuration.getBasePath() );

        Index other = new Index();
        Album album1 = new AlbumMock( "album1" );
        Album album2 = new AlbumMock( "album2" );
        Album album3 = new AlbumMock( "album3" );
        other.albums_add( album1 );
        other.albums_add( album2 );
        other.albums_add( album3 );
        Photo photo1 = new PhotoMock( "photo1" );
        Photo photo2 = new PhotoMock( "photo2" );
        Photo photo3 = new PhotoMock( "photo3" );
        album1.addPhoto( photo1 );
        album3.addPhoto( photo2 );
        album3.addPhoto( photo3 );
        Photo photo4 = new PhotoMock( "photo4" );
        Photo photo5 = new PhotoMock( "photo5" );
        other.photoNoInAlbums_add( photo4 );
        other.photoNoInAlbums_add( photo5 );

        Assert.assertTrue( index.equals( other ) );

    }

    /**
     * Create a folder simulating an album with the title passed, in the temporary folder (Base folder)
     * 
     * @param title {@link String} title for the album (folder name)
     * @return {@link File} folder created
     */
    private File createFolderAlbum( String title )
    {
        File fAlbum = new File( baseFolder.getAbsolutePath() + File.separatorChar + title );
        fAlbum.mkdirs();
        return fAlbum;
    }

    /**
     * Create a file simulating an photo with the title passed, in the temporary folder (Base folder)
     * 
     * @param albumTitle {@link String} title for the album (folder name), null if we don't want a photo inside a album
     * @param photoTitle {@link String} title for the photo inside the album (folder name)
     * @return {@link File} folder created
     * @throws IOException
     */
    private File createFilePhoto( String albumTitle, String photoTitle )
        throws IOException
    {
        File fPhoto = new File( baseFolder.getAbsolutePath()
            + ( albumTitle != null ? File.separatorChar + albumTitle : "" ) + File.separatorChar + photoTitle );

        FileOutputStream fos = new FileOutputStream( fPhoto );
        fos.write( new byte[] { 1, 2, 3, 4, 5 } );
        fos.close();
        return fPhoto;
    }

    @Test
    public void testIndexFlickrServer()
        throws Exception
    {
        Flickr flickr = Mockito.mock( Flickr.class );
        PhotosetsInterface psint = Mockito.mock( PhotosetsInterface.class );
        PhotosInterface pint = Mockito.mock( PhotosInterface.class );
        Mockito.when( flickr.getPhotosetsInterface() ).thenReturn( psint );
        Mockito.when( flickr.getPhotosInterface() ).thenReturn( pint );

        PhotoList<com.flickr4java.flickr.photos.Photo> photosNotInset = new PhotoList<>();
        photosNotInset.setPages( 1 );
        photosNotInset.setPage( 1 );
        com.flickr4java.flickr.photos.Photo photo4 = new com.flickr4java.flickr.photos.Photo();
        photo4.setTitle( "photo4" );
        photo4.setId( "photo4-id" );
        photosNotInset.add( photo4 );
        com.flickr4java.flickr.photos.Photo photo5 = new com.flickr4java.flickr.photos.Photo();
        photo5.setTitle( "photo5" );
        photo5.setId( "photo5-id" );
        photosNotInset.add( photo5 );
        Mockito.when( pint.getNotInSet( Mockito.anyInt(), Mockito.eq( 1 ) ) ).thenReturn( photosNotInset );

        Photosets photosets = new Photosets();
        photosets.setTotal( 3 );
        photosets.setPage( 1 );
        photosets.setPages( 1 );
        List<Photoset> list = new ArrayList<>();
        Photoset album1 = new Photoset();
        album1.setTitle( "album1" );
        album1.setId( "album1-id" );
        Photoset album2 = new Photoset();
        album2.setTitle( "album2" );
        album2.setId( "album2-id" );
        Photoset album3 = new Photoset();
        album3.setTitle( "album3" );
        album3.setId( "album3-id" );
        list.add( album1 );
        list.add( album2 );
        list.add( album3 );
        photosets.setPhotosets( list );

        com.flickr4java.flickr.photos.Photo photo1 = new com.flickr4java.flickr.photos.Photo();
        photo1.setTitle( "photo1" );
        photo1.setId( "photo1-id" );
        com.flickr4java.flickr.photos.Photo photo2 = new com.flickr4java.flickr.photos.Photo();
        photo2.setTitle( "photo2" );
        photo2.setId( "photo2-id" );
        com.flickr4java.flickr.photos.Photo photo3 = new com.flickr4java.flickr.photos.Photo();
        photo3.setTitle( "photo3" );
        photo3.setId( "photo3-id" );

        PhotoList<com.flickr4java.flickr.photos.Photo> album1List = new PhotoList<>();
        album1List.add( photo1 );
        PhotoList<com.flickr4java.flickr.photos.Photo> album2List = new PhotoList<>();
        PhotoList<com.flickr4java.flickr.photos.Photo> album3List = new PhotoList<>();
        album3List.add( photo2 );
        album3List.add( photo3 );

        Mockito.when( psint.getList( Mockito.eq( Configuration.getNSid() ) ) ).thenReturn( photosets );
        Mockito.when( psint.getPhotos( Mockito.eq( "album1-id" ), Mockito.anyInt(),
                                       Mockito.eq( 1 ) ) ).thenReturn( album1List );
        Mockito.when( psint.getPhotos( Mockito.eq( "album2-id" ), Mockito.anyInt(),
                                       Mockito.eq( 1 ) ) ).thenReturn( album2List );
        Mockito.when( psint.getPhotos( Mockito.eq( "album3-id" ), Mockito.anyInt(),
                                       Mockito.eq( 1 ) ) ).thenReturn( album3List );

        Index index = new Index();
        index.indexFlickrServer( flickr );

        Index index2 = new Index();
        Album index2_album1 = new AlbumMock( "album1" );
        Album index2_album2 = new AlbumMock( "album2" );
        Album index2_album3 = new AlbumMock( "album3" );
        index2.albums_add( index2_album1 );
        index2.albums_add( index2_album2 );
        index2.albums_add( index2_album3 );
        Photo index2_photo1 = new PhotoMock( "photo1" );
        Photo index2_photo2 = new PhotoMock( "photo2" );
        Photo index2_photo3 = new PhotoMock( "photo3" );
        index2_album1.addPhoto( index2_photo1 );
        index2_album3.addPhoto( index2_photo2 );
        index2_album3.addPhoto( index2_photo3 );
        Photo index2_photo4 = new PhotoMock( "photo4" );
        Photo index2_photo5 = new PhotoMock( "photo5" );
        index2.photoNoInAlbums_add( index2_photo4 );
        index2.photoNoInAlbums_add( index2_photo5 );

        Assert.assertTrue( index.equals( index2 ) );

        index2.photoNoInAlbums_remove( index2_photo4 );
        Assert.assertTrue( !index.equals( index2 ) );
    }
}
