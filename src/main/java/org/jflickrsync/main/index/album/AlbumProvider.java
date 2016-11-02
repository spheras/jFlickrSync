package org.jflickrsync.main.index.album;

import java.lang.reflect.Type;

import com.google.gson.InstanceCreator;

/**
 * GSON provider to create Albums
 */
public class AlbumProvider
    implements InstanceCreator<Album>
{
    @Override
    public Album createInstance( Type type )
    {
        return new AlbumMock( "" );
    }

}