package org.jflickrsync.main.watch.local;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jflickrsync.main.Configuration;
import org.jflickrsync.main.index.Index;
import org.jflickrsync.main.watch.WatchEvent;
import org.jflickrsync.main.watch.WatchEventType;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.flickr4java.flickr.Flickr;

public class LocalEventProcessorTest
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
    public void testWaitToReviewFolders()
        throws InterruptedException
    {
        LocalWatcher lw = Mockito.mock( LocalWatcher.class );
        File falbum = new File( baseFolder.getAbsolutePath() + File.separatorChar + "album1" );
        falbum.mkdirs();

        LocalEventProcessor lep = new LocalEventProcessor( null, null, null, lw );
        lep.addPendingFolders( falbum );
        Thread t = lep.waitToReviewFolders();
        t.join();
    }

    @Test
    public void process_delete()
        throws Exception
    {
        // creating test files
        createFolderAlbum( "album1" );
        createFolderAlbum( "album2" );
        createFolderAlbum( "album3" );
        createFilePhoto( "album1", "photo1" );
        createFilePhoto( "album3", "photo2" );
        createFilePhoto( "album3", "photo3" );

        Index localIndex = new Index();
        localIndex.indexLocal( basePath() );

        Index serverIndex = new Index();
        serverIndex.indexLocal( basePath() );

        Flickr flickr = Mockito.mock( Flickr.class );
        LocalWatcher watcher = Mockito.mock( LocalWatcher.class );

        // delete photo1
        LocalEventProcessor lep = new LocalEventProcessor( localIndex, serverIndex, flickr, watcher );

        Path pdir = Paths.get( basePath() + sepchar() + "album1" );
        Path p = Paths.get( basePath() + sepchar() + "album1" + sepchar() + "photo1" );

        Assert.assertNotNull( localIndex.locatePhoto( p.toFile().getAbsolutePath() ) );
        Assert.assertNotNull( serverIndex.locatePhoto( p.toFile().getAbsolutePath() ) );

        PathEvent pe = new PathEvent( pdir, p, ENTRY_DELETE );
        WatchEvent we = new WatchEvent();
        we.setProcessor( lep );
        we.setServerPoll( false );
        we.setEvent( pe );
        we.setType( WatchEventType.DELETE );
        lep.process( we );

        Assert.assertNull( localIndex.locatePhoto( p.toFile().getAbsolutePath() ) );
        Assert.assertNull( serverIndex.locatePhoto( p.toFile().getAbsolutePath() ) );

        // delete album3
        pdir = Paths.get( basePath() );
        p = Paths.get( basePath() + sepchar() + "album3" );

        Assert.assertNotNull( localIndex.locateAlbum( "album3" ) );
        Assert.assertNotNull( serverIndex.locateAlbum( "album3" ) );

        pe = new PathEvent( pdir, p, ENTRY_DELETE );
        we = new WatchEvent();
        we.setProcessor( lep );
        we.setServerPoll( false );
        we.setEvent( pe );
        we.setType( WatchEventType.DELETE );
        lep.process( we );

        Assert.assertNull( localIndex.locateAlbum( "album3" ) );
        Assert.assertNull( serverIndex.locateAlbum( "album3" ) );
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
     * The separator char
     * 
     * @return
     */
    private char sepchar()
    {
        return File.separatorChar;
    }

    /**
     * The base path
     * 
     * @return
     */
    private String basePath()
    {
        return Configuration.getBasePath();
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
}
