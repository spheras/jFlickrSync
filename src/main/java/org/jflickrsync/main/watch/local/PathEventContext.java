package org.jflickrsync.main.watch.local;

import java.nio.file.Path;
import java.util.List;

public interface PathEventContext
{

    boolean isValid();

    Path getWatchedDirectory();

    List<PathEvent> getEvents();

}