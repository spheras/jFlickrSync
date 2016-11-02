package org.jflickrsync.main.sync;

import org.jflickrsync.main.Configuration;
import org.jflickrsync.main.index.Index;

import com.flickr4java.flickr.Flickr;

public class SyncFactory
{
    public static Sync getSync( int type, Flickr flickr, Index localCache, Index serverCache )
    {
        switch ( type )
        {
            case Configuration.SYNCTYPE_TOTAL:
                return new TotalSync( flickr, localCache, serverCache );
            default:
                return null;
        }
    }
}
