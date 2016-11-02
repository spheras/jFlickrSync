package org.jflickrsync.main.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

import org.jflickrsync.main.Configuration;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import junit.framework.Assert;

public class UtilTest
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
    public void testGetRelativePath()
    {
        String relPath = Util.getRelativePath( Configuration.getBasePath() + File.separatorChar + "album/prueba.png" );
        Assert.assertEquals( relPath, "album/prueba.png" );
        relPath = Util.getRelativePath( Configuration.getBasePath() + File.separatorChar + "prueba.png" );
        Assert.assertEquals( relPath, "prueba.png" );
    }

    @Test
    public void testDeleteFileOrDirectory()
        throws IOException
    {
        File ffolder1 = folder.newFolder( "test" );
        File ffolder2 = new File( ffolder1.getAbsolutePath() + File.separatorChar + "other" );
        ffolder2.mkdirs();
        File ffile1 = new File( ffolder2.getAbsolutePath() + File.separatorChar + "myfile.png" );
        FileOutputStream fos = new FileOutputStream( ffile1 );
        fos.write( new byte[] { 1, 2, 3, 4, 5 } );
        fos.close();

        Assert.assertTrue( ffile1.exists() );

        Util.deleteFileOrFolder( Paths.get( ffolder1.getAbsolutePath() ) );

        Assert.assertTrue( !ffolder1.exists() );
    }

}
