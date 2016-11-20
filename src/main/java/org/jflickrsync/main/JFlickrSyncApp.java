package org.jflickrsync.main;

import java.io.IOException;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.jflickrsync.main.flickr.Authorization;
import org.jflickrsync.main.index.Index;
import org.jflickrsync.main.sync.SyncFactory;
import org.jflickrsync.main.util.Util;
import org.jflickrsync.main.watch.WatchMonitor;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;

public class JFlickrSyncApp
{
    private static final String SERVER_INDEX_FILE = "server.index";

    private static final String LOCAL_INDEX_FILE = "local.index";

    private static final Logger logger = Logger.getLogger( JFlickrSyncApp.class );

    /**
     * This is the current local index at memory
     */
    private Index localCurrentIndex;

    /**
     * This is the current server index at memory
     */
    private Index serverCurrentIndex;

    /**
     * This is the local index database (historical)
     */
    private Index localDBIndex;

    /**
     * This is the server index database (historical)
     */
    private Index serverDBIndex;

    /**
     * flickr instance
     */
    private Flickr flickr;

    public void start()
        throws Exception
    {
        Util.printAbout();
        logger.info( "Starting jFlickR!" );

        // First, we authorize the user
        logger.info( "Authenticating against Flickr service..." );
        this.flickr = new Authorization().authorize();

        // second, getting indexes
        logger.info( "Getting indexes..." );
        getIndexes( this.flickr );

        // third, listening to file system events
        logger.info( "Watching Processors..." );
        new WatchMonitor().start( this.localCurrentIndex, this.serverCurrentIndex, this.flickr );

        // UploadPhoto up = new UploadPhoto( auth, flickr );
        // String folder = "/home/spheras/jasdata/projects/jflickrsync/src/jflickrsync/etc/images/";
        // String simage1 = folder + "TotallyFreeImages.com-64688-Standard-preview.jpg";
        // up.upload( simage1, "prueba" );
    }

    public void stop()
    {
        logger.info( "Stopping jFlickR!" );
        System.exit( 0 );
    }

    /**
     * Get the indexes from files or generate ones
     * 
     * @param flickr {@link Flickr} flickr object
     * @throws IOException
     * @throws FlickrException
     * @throws Exception
     */
    private void getIndexes( Flickr flickr )
        throws IOException, FlickrException, Exception
    {
        this.localDBIndex = Index.load( LOCAL_INDEX_FILE );
        this.serverDBIndex = Index.load( SERVER_INDEX_FILE );
        if ( localDBIndex == null || serverDBIndex == null
            || !localDBIndex.getPath().equals( Configuration.getBasePath() ) )
        {
            System.out.println();
            System.out.println( "It seems that this is the first time you execute jFlickrSync. All the photos at flickR will be downloaded to the following path:" );
            System.out.println( Configuration.getBasePath() );
            System.out.println( "And, if this local base path contains photos that doesn't exist at flickr server, them will be uploaded!" );
            System.out.println( "Remember: The configuration base path to download can be changed at "
                + Configuration.getConfigurationFileAbsolutePath() );
            Scanner scanner = new Scanner( System.in );
            String response = "";
            do
            {
                System.out.print( "Do you agree to start downloading/uploading images (Y/N)? " );
                try{
                    response = scanner.nextLine();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            while ( !response.equalsIgnoreCase( "Y" ) && !response.equalsIgnoreCase( "N" ) );
            scanner.close();
            if ( response.equalsIgnoreCase( "N" ) )
            {
                logger.info( "Exiting jFlickRSync" );
                System.exit( 0 );
            }

            // we first need to create indexes. Is the first time we launch jflickrsync? we need to download everything
            // from sever.
            localCurrentIndex = new Index();
            localCurrentIndex.indexLocal( Configuration.getBasePath() );
            serverCurrentIndex = new Index();
            serverCurrentIndex.indexFlickrServer( flickr );
            localCurrentIndex.setStoreFileNameIndex( LOCAL_INDEX_FILE );
            serverCurrentIndex.setStoreFileNameIndex( SERVER_INDEX_FILE );

            // we sync to have an initial state
            SyncFactory.getSync( Configuration.getSyncType(), flickr, localCurrentIndex, serverCurrentIndex ).sync();

            // and save the indexes
            localCurrentIndex.save();
            serverCurrentIndex.save();

            localDBIndex = Index.load( LOCAL_INDEX_FILE );
            serverDBIndex = Index.load( SERVER_INDEX_FILE );

            // so, now, current and DB shoud be equals, this is the initial state
        }
        else
        {
            // loading current index
            localCurrentIndex = new Index();
            serverCurrentIndex = new Index();

            localCurrentIndex.indexLocal( Configuration.getBasePath() );
            serverCurrentIndex.indexFlickrServer( flickr );

            localCurrentIndex.setStoreFileNameIndex( LOCAL_INDEX_FILE );
            serverCurrentIndex.setStoreFileNameIndex( SERVER_INDEX_FILE );
        }
    }

}
