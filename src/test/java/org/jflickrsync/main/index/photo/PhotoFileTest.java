package org.jflickrsync.main.index.photo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jflickrsync.main.Configuration;
import org.jflickrsync.main.index.album.AlbumFile;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.uploader.UploadMetaData;
import com.flickr4java.flickr.uploader.Uploader;

import junit.framework.Assert;

public class PhotoFileTest
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
    }

    @Test
    public void constructor()
    {
        File ffolder = new File( baseFolder.getAbsolutePath() + File.separatorChar + "album1" );
        ffolder.mkdirs();

        File file1 = new File( ffolder.getAbsolutePath() + File.separatorChar + "file1.jpeg" );
        PhotoFile pf = new PhotoFile( file1 );

        Assert.assertEquals( pf.getTitle(), "file1" );
        Assert.assertEquals( file1.getAbsolutePath(), pf.getAbsolutePath() );
    }

    @Test
    public void testUploadToFlickr()
        throws FlickrException, IOException
    {
        File ffolder = new File( baseFolder.getAbsolutePath() + File.separatorChar + "album1" );
        ffolder.mkdirs();

        File file1 = new File( ffolder.getAbsolutePath() + File.separatorChar + "file1.jpeg" );
        PhotoFile pf = new PhotoFile( file1 );

        Flickr flickr = Mockito.mock( Flickr.class );
        Uploader uploader = Mockito.mock( Uploader.class );
        Mockito.when( flickr.getUploader() ).thenReturn( uploader );

        pf.uploadToFlickR( flickr );

        Mockito.verify( uploader, Mockito.atLeastOnce() ).upload( Mockito.eq( file1 ),
                                                                  Mockito.any( UploadMetaData.class ) );
    }

    @Test
    public void testRemove()
        throws Exception
    {
        File ffolder = new File( baseFolder.getAbsolutePath() + File.separatorChar + "album1" );
        ffolder.mkdirs();

        File file1 = new File( ffolder.getAbsolutePath() + File.separatorChar + "file1.jpeg" );
        FileOutputStream fos = new FileOutputStream( file1 );
        fos.write( new byte[] { 1, 2, 3, 4, 5 } );
        fos.close();
        PhotoFile pf = new PhotoFile( file1 );

        Assert.assertTrue( file1.exists() );

        pf.remove();

        Assert.assertTrue( !file1.exists() );
    }
}
