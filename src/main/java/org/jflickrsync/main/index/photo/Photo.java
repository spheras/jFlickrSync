package org.jflickrsync.main.index.photo;

import org.jflickrsync.main.index.album.Album;

import com.google.gson.annotations.Expose;

import lombok.Data;

@Data
public abstract class Photo
{
    /**
     * Title of the photo
     */
    @Expose
    private String title;

    /**
     * Album of this photo. Null if no album associated
     */
    private Album album;

    /**
     * indicates if the photo was synced
     */
    @Expose
    private boolean synced;

    @Override
    public boolean equals( Object obj )
    {
        if ( obj instanceof Photo )
        {
            Photo photo = (Photo) obj;
            if ( photo.title.equalsIgnoreCase( this.title ) )
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

    /**
     * Removes the photo
     * 
     * @return boolean -> true if the photo was removed, false if not
     * @throws Exception
     */
    public abstract boolean remove()
        throws Exception;

    /**
     * Return the filename of this photo (the real file name it has or the file name it should have)
     * 
     * @return {@link String} the filename
     */
    public abstract String getFilename();

    /**
     * Return the absolute path to the file of this photo (the real file path it has or the file path it should have)
     * 
     * @return {@link String} the absolute path
     */
    public abstract String getAbsolutePath();

}
