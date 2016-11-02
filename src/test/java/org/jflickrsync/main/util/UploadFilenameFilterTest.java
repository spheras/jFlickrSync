package org.jflickrsync.main.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jflickrsync.main.Configuration;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class UploadFilenameFilterTest
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
    public void testFilter()
        throws IOException
    {
        File ffolder = createFolderAlbum( "test" );
        createFilePhoto( "test", "photo1", "jpg" );
        createFilePhoto( "test", "photo2", "jpeg" );
        createFilePhoto( "test", "photo3", "123" );
        createFilePhoto( "test", "photo4", "kkk" );
        createFilePhoto( "test", "photo5", "mp4" );

        String[] list = ffolder.list( new UploadFilenameFilter() );

        Assert.assertTrue( list.length == 3 );
        for ( int i = 0; i < 2; i++ )
        {
            Assert.assertTrue( list[i].equals( "photo1.jpg" ) || list[i].equals( "photo2.jpeg" )
                || list[i].equals( "photo5.mp4" ) );
        }
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
    private File createFilePhoto( String albumTitle, String photoTitle, String extension )
        throws IOException
    {
        File fPhoto =
            new File( baseFolder.getAbsolutePath() + ( albumTitle != null ? File.separatorChar + albumTitle : "" )
                + File.separatorChar + photoTitle + "." + extension );

        FileOutputStream fos = new FileOutputStream( fPhoto );
        fos.write( new byte[] { 1, 2, 3, 4, 5 } );
        fos.close();
        return fPhoto;
    }
}
