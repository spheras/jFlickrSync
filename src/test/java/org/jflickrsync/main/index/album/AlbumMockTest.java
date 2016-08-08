package org.jflickrsync.main.index.album;

import org.junit.Test;

import junit.framework.Assert;

public class AlbumMockTest
{

    @Test
    public void constructor()
    {
        AlbumMock mock = new AlbumMock( "title1" );
        Assert.assertEquals( mock.getTitle(), "title1" );
    }

}
