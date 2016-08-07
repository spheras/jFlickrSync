package org.jflickrsync.main.flickr;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
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

public class Authorization
{
    private static final Logger logger = Logger.getLogger( Authorization.class );

    private static final String FLICKR_AUTH_FILE = ".flickrAuth";

    private Flickr flickr;

    private AuthStore authStore;

    public Flickr authorize()
        throws IOException, FlickrException, SAXException
    {
        Properties conf = Configuration.getConfiguration();
        String sharedsecret = conf.getProperty( Configuration.CONFIG_SHAREDSECRET );
        String apikey = conf.getProperty( Configuration.CONFIG_APIKEY );
        String stoken = conf.getProperty( Configuration.CONFIG_ACCESSTOKEN, "" );
        String authsDirStr = System.getProperty( Configuration.USER_HOME ) + File.separatorChar
            + Configuration.CONFIG_FOLDER_NAME + File.separatorChar + FLICKR_AUTH_FILE;

        this.flickr = new Flickr( apikey, sharedsecret, new REST() );
        this.authStore = new FileAuthStore( new File( authsDirStr ) );

        if ( stoken == null || stoken.isEmpty() )
        {
            authorizeApp();
        }

        setAuth( Configuration.getAccessToken(), Configuration.getUsername(), Configuration.getTokenSecret(),
                 Configuration.getNSid() );

        Flickr.debugRequest = false;
        Flickr.debugStream = false;
        
        return flickr;
    }

    public void setAuth( String accessToken, String username, String tokenSecret, String nsid )
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
            if ( this.authStore != null )
            {
                auth = this.authStore.retrieve( nsid );
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
        AuthInterface authInterface = flickr.getAuthInterface();
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
        this.authStore.store( auth );

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
        this.authStore.store( auth );
        return auth;
    }

    public Flickr getFlickr()
    {
        return flickr;
    }

    public void setFlickr( Flickr flickr )
    {
        this.flickr = flickr;
    }

    public AuthStore getAuthStore()
    {
        return authStore;
    }

    public void setAuthStore( AuthStore authStore )
    {
        this.authStore = authStore;
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
        PrefsInterface prefi = flickr.getPrefsInterface();
        return prefi.getPrivacy();
    }
}
