package org.jflickrsync.main.index.photo;

import org.junit.Test;

import junit.framework.Assert;

public class PhotoTest
{

    @Test
    public void testEquals()
    {
        Photo photo1 = new PhotoMock( "title1" );
        Photo photo2 = new PhotoMock( "title2" );
        Photo photo3 = new PhotoMock( "title3" );
        Photo photo4 = new PhotoMock( "title2" );
        Photo photo5 = new PhotoMock( "title1" );

        Assert.assertEquals( photo1, photo5 );
        Assert.assertEquals( photo2, photo4 );
        Assert.assertNotSame( photo1, photo5 );
        Assert.assertTrue( !photo1.equals( photo2 ) );
        Assert.assertTrue( !photo1.equals( photo3 ) );
        Assert.assertTrue( !photo1.equals( photo4 ) );
    }
}
