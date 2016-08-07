package org.jflickrsync.main.watch.server;

import org.jflickrsync.main.watch.WatchEventType;

import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photosets.Photoset;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServerEvent
{
    private WatchEventType type;

    private Photo serverPhoto;

    private Photoset serverPhotoset;

    public String id;

}
