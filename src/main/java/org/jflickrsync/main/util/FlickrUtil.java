package org.jflickrsync.main.util;

import org.jflickrsync.main.Configuration;

public class FlickrUtil
{
    public static String makeSafeFilename( String input )
    {
        byte[] fname = input.getBytes();
        byte[] bad = new byte[] { '\\', '/', '"', '*' };
        byte replace = '_';
        for ( int i = 0; i < fname.length; i++ )
        {
            for ( byte element : bad )
            {
                if ( fname[i] == element )
                {
                    fname[i] = replace;
                }
            }
            if ( Configuration.getReplaceSpaces() && fname[i] == ' ' )
                fname[i] = '_';
        }
        return new String( fname );
    }

}
