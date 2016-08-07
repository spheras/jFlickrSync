package org.jflickrsync.main.index.album;

public class AlbumMock
    extends Album
{
    public AlbumMock(String title){
        super();
        setTitle( title );
    }

    @Override
    public boolean remove()
        throws Exception
    {
        return false;
    }

}
