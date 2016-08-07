package org.jflickrsync.main.watch.local;

import java.io.IOException;

public interface DirectoryEventWatcher
{
    void start()
        throws IOException;

    boolean isRunning();

    void stop();
}