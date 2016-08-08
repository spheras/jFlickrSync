package org.jflickrsync.main.index.photo;

import org.junit.Test;

import junit.framework.Assert;

public class PhotoMockTest
{

    @Test
    public void constructor()
    {
        PhotoMock mock = new PhotoMock( "title1" );
        Assert.assertEquals( mock.getTitle(), "title1" );
    }

}
