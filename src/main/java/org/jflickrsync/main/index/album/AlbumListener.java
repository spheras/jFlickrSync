package org.jflickrsync.main.index.album;

public interface AlbumListener
{
    /**
     * Notify a change at the album (new photo or removed photo)
     * 
     * @throws Exception
     */
    void albumChange()
        throws Exception;
}
