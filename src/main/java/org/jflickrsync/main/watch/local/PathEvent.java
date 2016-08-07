package org.jflickrsync.main.watch.local;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

public class PathEvent
{
    private final Path eventTarget;

    private final WatchEvent.Kind<?> type;

    private final Path dir;

    public PathEvent( Path dir, Path eventTarget, WatchEvent.Kind<?> type )
    {
        this.dir = dir;
        this.eventTarget = eventTarget;
        this.type = type;
    }

    public Path getEventTarget()
    {
        return eventTarget;
    }

    public WatchEvent.Kind<?> getType()
    {
        return type;
    }

    public Path getDir()
    {
        return dir;
    }
}