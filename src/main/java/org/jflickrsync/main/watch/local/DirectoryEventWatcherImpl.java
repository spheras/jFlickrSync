package org.jflickrsync.main.watch.local;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import com.google.common.eventbus.EventBus;

public class DirectoryEventWatcherImpl
    implements DirectoryEventWatcher
{

    private FutureTask<Integer> watchTask;

    private EventBus eventBus;

    private WatchService watchService;

    private volatile boolean keepWatching = true;

    private final Map<WatchKey, Path> keys;

    private Path startPath;

    public DirectoryEventWatcherImpl( EventBus eventBus, Path startPath )
    {
        this.eventBus = Objects.requireNonNull( eventBus );
        this.startPath = Objects.requireNonNull( startPath );
        this.keys = new HashMap<WatchKey, Path>();
    }

    @Override
    public void start()
        throws IOException
    {
        initWatchService();
        registerDirectories();
        createWatchTask();
        startWatching();
    }

    @Override
    public boolean isRunning()
    {
        return watchTask != null && !watchTask.isDone();
    }

    @Override
    public void stop()
    {
        keepWatching = false;
    }

    // Used for testing purposes
    Integer getEventCount()
    {
        try
        {
            return watchTask.get();
        }
        catch ( InterruptedException | ExecutionException e )
        {
            throw new RuntimeException( e );
        }
    }

    private void createWatchTask()
    {
        watchTask = new FutureTask<>( new Callable<Integer>()
        {
            private int totalEventCount;

            @Override
            public Integer call()
                throws Exception
            {
                while ( keepWatching )
                {
                    WatchKey watchKey = watchService.poll( 10, TimeUnit.SECONDS );
                    if ( watchKey != null )
                    {
                        Path dir = keys.get( watchKey );

                        List<WatchEvent<?>> events = watchKey.pollEvents();
                        Path watched = (Path) watchKey.watchable();
                        PathEvents pathEvents = new PathEvents( watchKey.isValid(), watched );
                        for ( WatchEvent<?> event : events )
                        {
                            Path context = (Path) event.context();
                            PathEvent pathevent = new PathEvent( dir, context, event.kind() );
                            pathEvents.add( pathevent );
                            totalEventCount++;

                            // if ( pathevent.getType() == ENTRY_CREATE )
                            // {
                            // // if a new directory is created, we need to watch it too
                            // String foldername = pathevent.getEventTarget().toFile().getName();
                            // String absolutePath = dir.toString() + File.separatorChar + foldername;
                            // File fabsolutePath = new File( absolutePath );
                            // if ( fabsolutePath.isDirectory() )
                            // {
                            // watchSubdirectory( absolutePath );
                            // }
                            // }

                        }
                        watchKey.reset();
                        eventBus.post( pathEvents );
                    }
                }
                return totalEventCount;
            }

        } );
    }

    /**
     * Start watching a new subdirectory created
     * 
     * @param absolutePath
     * @throws IOException
     */
    public void watchSubdirectory( String absolutePath )
        throws IOException
    {
        Path newdir = Paths.get( absolutePath );
        WatchKey key = newdir.register( watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW );
        keys.put( key, newdir );
    }

    private void startWatching()
    {
        new Thread( watchTask ).start();
    }

    private void registerDirectories()
        throws IOException
    {
        Files.walkFileTree( startPath, new WatchServiceRegisteringVisitor() );
    }

    private WatchService initWatchService()
        throws IOException
    {
        if ( watchService == null )
        {
            watchService = FileSystems.getDefault().newWatchService();
        }
        return watchService;
    }

    private class WatchServiceRegisteringVisitor
        extends SimpleFileVisitor<Path>
    {
        @Override
        public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs )
            throws IOException
        {
            WatchKey key = dir.register( watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW );
            keys.put( key, dir );
            return FileVisitResult.CONTINUE;
        }
    }
}