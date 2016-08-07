package org.jflickrsync.main.index.album;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jflickrsync.main.Configuration;
import org.jflickrsync.main.index.photo.Photo;
import org.jflickrsync.main.index.photo.PhotoMock;

import com.google.gson.annotations.Expose;

import lombok.Data;
import lombok.Setter;

@Data
public abstract class Album
{

    @Expose
    private String title;

    // indicates if the album was synced (not the photos of this album, only the album itself)
    private boolean synced;

    @Expose
    private List<Photo> photos = new ArrayList<Photo>();

    @Setter
    private AlbumListener listener;

    /**
     * Search a photo inside this album by title
     * 
     * @param title
     * @return
     */
    public Photo locatePhoto( String title )
    {
        Photo photo = new PhotoMock( title );
        int index = photos.indexOf( photo );
        if ( index >= 0 )
        {
            return photos.get( index );
        }
        return null;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( obj instanceof Album )
        {
            Album album = (Album) obj;
            if ( album.title.equalsIgnoreCase( this.title ) )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return title.hashCode();
    }

    public File getLocalPathAlbum()
    {
        File falbum = new File( Configuration.getBasePath() + File.separatorChar + getTitle() );
        return falbum;
    }

    public synchronized void addPhoto( Photo photo )
        throws Exception
    {
        photo.setAlbum( this );
        this.photos.add( photo );
        change();
    }

    /**
     * To notify changes
     * 
     * @throws Exception
     */
    private void change()
        throws Exception
    {
        if ( this.listener != null )
        {
            this.listener.albumChange();
        }
    }

    /**
     * Removes an existing photo from the list and phisically
     * 
     * @param photo {@link Photo} photo to remove (based on title to search)
     * @throws Exception
     */
    public synchronized void removePhoto( Photo photo )
        throws Exception
    {
        int index = getPhotos().indexOf( photo );
        if ( index >= 0 )
        {
            Photo photoi = getPhotos().get( index );
            getPhotos().remove( index );
            photoi.remove();
            change();
        }
    }

    /**
     * Removes the album and its photos
     * 
     * @return boolean -> true if the album was removed, false if not
     * @throws Exception
     */
    public abstract boolean remove()
        throws Exception;
}
