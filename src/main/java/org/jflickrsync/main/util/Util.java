package org.jflickrsync.main.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.jflickrsync.main.Configuration;

public class Util
{
    public static void printAbout()
    {
        System.out.println();
        System.out.println( "      ████╗                                           ███████╗                           " );
        System.out.println( "      ████║                                           ██╔════╝                           " );
        System.out.println( "      ████║███████╗██╗     ██╗ ██████╗██╗  ██╗██████╗ ██║     ██╗   ██╗███╗   ██╗ ██████╗" );
        System.out.println( "      ████║██╔════╝██║     ██║██╔════╝██║ ██╔╝██╔══██╗███████╗╚██╗ ██╔╝████╗  ██║██╔════╝" );
        System.out.println( " ████ ████║█████╗  ██║     ██║██║     █████╔╝ ██████╔╝╚════██║ ╚████╔╝ ██╔██╗ ██║██║     " );
        System.out.println( " █████████║██╔══╝  ██║     ██║██║     ██╔═██╗ ██╔══██╗     ██║  ╚██╔╝  ██║╚██╗██║██║     " );
        System.out.println( " ╚███████╔╝██║     ███████╗██║╚██████╗██║  ██╗██║  ██║███████║   ██║   ██║ ╚████║╚██████╗" );
        System.out.println( "    ╚════╝ ╚═╝     ╚══════╝╚═╝ ╚═════╝╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝   ╚═╝   ╚═╝  ╚═══╝ ╚═════╝" );
        System.out.println();
    }

    /**
     * return the relative path of a photo/album from the base path
     * 
     * @param absolutePath {@link String} absolute path to the resource
     * @return {@link String} relative path to the resource
     */
    public static String getRelativePath( String absolutePath )
    {
        String basePath = Configuration.getBasePath();
        String relativePath = absolutePath.substring( basePath.length() );
        if ( relativePath.charAt( 0 ) == File.separatorChar )
        {
            relativePath = relativePath.substring( 1 );
        }
        return relativePath;
    }

    /**
     * Remove a file or a folder (recursivelly) http://stackoverflow.com/questions/3775694/deleting-folder-from-java
     * 
     * @param path
     * @throws IOException
     */
    public static void deleteFileOrFolder( final Path path )
        throws IOException
    {
        Files.walkFileTree( path, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile( final Path file, final BasicFileAttributes attrs )
                throws IOException
            {
                Files.delete( file );
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed( final Path file, final IOException e )
            {
                return handleException( e );
            }

            private FileVisitResult handleException( final IOException e )
            {
                e.printStackTrace(); // replace with more robust error handling
                return FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult postVisitDirectory( final Path dir, final IOException e )
                throws IOException
            {
                if ( e != null )
                    return handleException( e );
                Files.delete( dir );
                return FileVisitResult.CONTINUE;
            }
        } );
    }
}
