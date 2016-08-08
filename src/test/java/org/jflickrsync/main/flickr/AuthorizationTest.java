package org.jflickrsync.main.flickr;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.jflickrsync.main.Configuration;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.scribe.model.Token;
import org.xml.sax.SAXException;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.RequestContext;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.auth.AuthInterface;
import com.flickr4java.flickr.auth.Permission;
import com.flickr4java.flickr.people.User;

public class AuthorizationTest
{
    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    private static File configFolder;

    @BeforeClass
    public static void setup()
        throws IOException
    {
        configFolder = folder.newFolder( "configuration" );
        Configuration.getConfiguration( configFolder.getAbsolutePath() );
    }

    @Test
    public void testAuthorize()
        throws IOException, FlickrException, SAXException
    {
        AuthInterface authInt = Mockito.mock( AuthInterface.class );
        Token token = new Token( "mytoken", "mysecret" );
        Mockito.when( authInt.getRequestToken() ).thenReturn( token );
        Mockito.when( authInt.getAccessToken( Mockito.any( Token.class ),
                                              Mockito.any( org.scribe.model.Verifier.class ) ) ).thenReturn( token );
        Mockito.when( authInt.getAuthorizationUrl( Mockito.any( Token.class ),
                                                   Mockito.any( Permission.class ) ) ).thenReturn( "http://www.jflickrsync.com/tests" );

        User user = new User();
        user.setUsername( "username" );
        user.setDescription( "miuser" );
        user.setRealName( "user test realname" );
        user.setId( "idtest" );
        Auth mockAuth = new Auth( Permission.WRITE, user );
        mockAuth.setToken( "mocktoken" );
        mockAuth.setTokenSecret( "mockSecret" );
        Mockito.when( authInt.checkToken( Mockito.any( Token.class ) ) ).thenReturn( mockAuth );
        Flickr flickr = Mockito.mock( Flickr.class );
        Mockito.when( flickr.getAuthInterface() ).thenReturn( authInt );

        Authorization authorization = new Authorization();
        authorization.setFlickr( flickr );

        ByteArrayInputStream in = new ByteArrayInputStream( "12345\n".getBytes() );
        System.setIn( in );

        authorization.authorize();

        // optionally, reset System.in to its original
        System.setIn( System.in );

        Assert.assertEquals( Configuration.getAccessToken(), "mytoken" );
        Assert.assertEquals( Configuration.getTokenSecret(), "mysecret" );
        Assert.assertEquals( Configuration.getNSid(), "idtest" );
        Assert.assertEquals( Configuration.getUsername(), "username" );

        RequestContext rc = RequestContext.getRequestContext();
        Auth auth = rc.getAuth();
        Assert.assertEquals( auth.getPermission(), Permission.WRITE );
        Assert.assertEquals( auth.getTokenSecret(), "mockSecret" );
        Assert.assertEquals( auth.getUser(), user );
        Assert.assertEquals( auth.getToken(), "mocktoken" );
    }

}
