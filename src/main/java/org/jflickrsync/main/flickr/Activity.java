package org.jflickrsync.main.flickr;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.activity.ActivityInterface;
import com.flickr4java.flickr.activity.Event;
import com.flickr4java.flickr.activity.Item;
import com.flickr4java.flickr.activity.ItemList;

public class Activity
{
    private static final Logger logger = Logger.getLogger( Activity.class );

    public void showActivity( Flickr f )
        throws FlickrException, IOException, SAXException
    {
        ActivityInterface iface = f.getActivityInterface();
        ItemList<Item> list = iface.userComments( 10, 0 );
        for ( int j = 0; j < list.size(); j++ )
        {
            Item item = (Item) list.get( j );
            logger.info( "Item " + ( j + 1 ) + "/" + list.size() + " type: " + item.getType() );
            logger.info( "Item-id:       " + item.getId() + "\n" );
            Collection<Event> events = item.getEvents();
            Iterator<Event> iterator = events.iterator();
            int i = 0;
            int size = events.size();
            while ( iterator.hasNext() )
            {
                Event event = iterator.next();
                logger.info( "Event " + ( i + 1 ) + "/" + size + " of Item " + ( j + 1 ) );
                logger.info( "Event-type: " + event.getType() );
                logger.info( "User:       " + event.getUser() );
                logger.info( "Username:   " + event.getUsername() );
                logger.info( "Value:      " + event.getValue() + "\n" );
                i++;
            }
        }

        ActivityInterface iface2 = f.getActivityInterface();
        list = iface2.userPhotos( 50, 0, "300d" );
        for ( int j = 0; j < list.size(); j++ )
        {
            Item item = (Item) list.get( j );
            logger.info( "Item " + ( j + 1 ) + "/" + list.size() + " type: " + item.getType() );
            logger.info( "Item-id:       " + item.getId() + "\n" );

            Collection<Event> events = item.getEvents();
            Iterator<Event> iterator = events.iterator();
            int i = 0;
            int size = events.size();
            while ( iterator.hasNext() )
            {
                Event event = iterator.next();
                logger.info( "Event " + ( i + 1 ) + "/" + size + " of Item " + ( j + 1 ) );
                logger.info( "Event-type: " + event.getType() );
                if ( event.getType().equals( "note" ) )
                {
                    logger.info( "Note-id:    " + event.getId() );
                }
                else if ( event.getType().equals( "comment" ) )
                {
                    logger.info( "Comment-id: " + event.getId() );
                }
                logger.info( "User:       " + event.getUser() );
                logger.info( "Username:   " + event.getUsername() );
                logger.info( "Value:      " + event.getValue() );
                logger.info( "Dateadded:  " + event.getDateadded() + "\n" );
                i++;
            }
        }
    }
}
