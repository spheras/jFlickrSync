package org.jflickrsync.main.flickr;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.jflickrsync.main.Configuration;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.xml.sax.SAXException;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.RequestContext;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.auth.AuthInterface;
import com.flickr4java.flickr.auth.Permission;
import com.flickr4java.flickr.people.User;
import com.flickr4java.flickr.prefs.PrefsInterface;
import com.flickr4java.flickr.util.AuthStore;
import com.flickr4java.flickr.util.FileAuthStore;

import lombok.Getter;
import lombok.Setter;

public class Authorization
{
    private static final Logger logger = Logger.getLogger( Authorization.class );

    private static final String FLICKR_AUTH_FILE = ".flickrAuth";

    @Setter
    @Getter
    private Flickr flickr;

    @Getter
    @Setter
    private AuthStore authStore;

    /**
     * Constructor
     */
    public Authorization()
    {
        String sharedsecret = Configuration.getSharedSecret();
        String apikey = Configuration.getAPIKey();
        setFlickr( new Flickr( apikey, sharedsecret, new REST() ) );
        Flickr.debugRequest = false;
        Flickr.debugStream = false;
    }

    /**
     * Try to authorize the app to get user info
     * 
     * @return {@link Flickr} object created
     * @throws IOException
     * @throws FlickrException
     * @throws SAXException
     */
    public Flickr authorize()
        throws IOException, FlickrException, SAXException
    {
        String accessToken = Configuration.getAccessToken();
        String authsDirStr = Configuration.getConfigurationFolderAbsolutePath() + File.separatorChar + FLICKR_AUTH_FILE;

        setAuthStore( new FileAuthStore( new File( authsDirStr ) ) );

        if ( accessToken == null || accessToken.isEmpty() )
        {
            authorizeApp();
        }

        setAuth( accessToken, Configuration.getUsername(), Configuration.getTokenSecret(), Configuration.getNSid() );

        return getFlickr();
    }

    private void setAuth( String accessToken, String username, String tokenSecret, String nsid )
        throws IOException, SAXException, FlickrException
    {
        RequestContext rc = RequestContext.getRequestContext();
        Auth auth = null;

        if ( accessToken != null && !accessToken.equals( "" ) && tokenSecret != null && !tokenSecret.equals( "" ) )
        {
            auth = constructAuth( accessToken, tokenSecret, username, nsid );
            rc.setAuth( auth );
        }
        else
        {
            if ( getAuthStore() != null )
            {
                auth = getAuthStore().retrieve( nsid );
                if ( auth == null )
                {
                    this.authorizeApp();
                }
                else
                {
                    rc.setAuth( auth );
                }
            }
        }
    }

    /**
     * The app haven't been authorized yet
     * 
     * @throws FlickrException
     * @throws IOException
     */
    private void authorizeApp()
        throws FlickrException, IOException
    {
        // we need to allow this application for this user
        Flickr.debugStream = false;
        AuthInterface authInterface = getFlickr().getAuthInterface();
        Scanner scanner = new Scanner( System.in );
        Token token = authInterface.getRequestToken();

        logger.info( "token: " + token );

        String url = authInterface.getAuthorizationUrl( token, Permission.DELETE );
        logger.info( "Follow this URL to authorise yourself on Flickr" );
        logger.info( url );
        logger.info( "Paste in the token it gives you:" );
        logger.info( ">>" );

        String tokenKey = scanner.nextLine();
        scanner.close();

        Token requestToken = authInterface.getAccessToken( token, new Verifier( tokenKey ) );
        logger.info( "Authentication success" );

        Auth auth = authInterface.checkToken( requestToken );
        getAuthStore().store( auth );

        // we save the token secret for the next time
        Configuration.saveToken( requestToken.getToken(), requestToken.getSecret(), auth.getUser().getId(),
                                 auth.getUser().getUsername() );

        // This token can be used until the user revokes it.
        logger.info( "Token: " + requestToken.getToken() );
        logger.info( "Secret: " + requestToken.getSecret() );
        logger.info( "nsid: " + auth.getUser().getId() );
        logger.info( "Realname: " + auth.getUser().getRealName() );
        logger.info( "Username: " + auth.getUser().getUsername() );
        logger.info( "Permission: " + auth.getPermission().getType() );
    }

    /**
     * If the Authtoken was already created in a separate program but not saved to file.
     * 
     * @param accessToken
     * @param tokenSecret
     * @param username
     * @return
     * @throws IOException
     */
    private Auth constructAuth( String accessToken, String tokenSecret, String username, String nsid )
        throws IOException
    {

        Auth auth = new Auth();
        auth.setToken( accessToken );
        auth.setTokenSecret( tokenSecret );

        // Prompt to ask what permission is needed: read, update or delete.
        auth.setPermission( Permission.fromString( "delete" ) );

        User user = new User();
        // Later change the following 3. Either ask user to pass on command line or read
        // from saved file.
        user.setId( nsid );
        user.setUsername( ( username ) );
        user.setRealName( "" );
        auth.setUser( user );
        getAuthStore().store( auth );
        return auth;
    }

    public boolean canUpload()
    {
        RequestContext rc = RequestContext.getRequestContext();
        Auth auth = null;
        auth = rc.getAuth();
        if ( auth == null )
        {
            logger.error( " Cannot upload, there is no authorization information." );
            return false;
        }
        Permission perm = auth.getPermission();
        if ( ( perm.getType() == Permission.WRITE_TYPE ) || ( perm.getType() == Permission.DELETE_TYPE ) )
            return true;
        else
        {
            logger.error( " Cannot upload, You need write or delete permission, you have : " + perm.toString() );
            return false;
        }
    }

    /**
     * 1 : Public 2 : Friends only 3 : Family only 4 : Friends and Family 5 : Private
     **/
    public int getPrivacy()
        throws Exception
    {
        PrefsInterface prefi = getFlickr().getPrefsInterface();
        return prefi.getPrivacy();
    }
}
