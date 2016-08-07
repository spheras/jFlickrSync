package org.jflickrsync.main.index.photo;

import java.lang.reflect.Type;

import com.google.gson.InstanceCreator;

/**
 * GSON provider to create Photos
 */
public class PhotoProvider
    implements InstanceCreator<Photo>
{
    @Override
    public Photo createInstance( Type type )
    {
        return new PhotoMock( "" );
    }

}