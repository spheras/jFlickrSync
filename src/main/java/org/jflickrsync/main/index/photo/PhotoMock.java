package org.jflickrsync.main.index.photo;

public class PhotoMock
    extends Photo
{
    public PhotoMock( String title )
    {
        super();
        this.setTitle( title );
    }

    @Override
    public boolean remove()
        throws Exception
    {
        return false;
    }

    @Override
    public String getFilename()
    {
        return "";
    }

    @Override
    public String getAbsolutePath()
    {
        return "";
    }

}
