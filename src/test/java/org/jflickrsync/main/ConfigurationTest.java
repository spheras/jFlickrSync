package org.jflickrsync.main;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ConfigurationTest
{
    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    private static File configFolder;

    @BeforeClass
    public static void setup()
        throws IOException
    {
        Configuration.resetConfiguration();
        configFolder = folder.newFolder( "configuration" );
        Configuration.getConfiguration( configFolder.getAbsolutePath() );
    }

    @Test
    public void testConfigurationCreation()
        throws IOException
    {
        Properties prop = Configuration.getConfiguration();

        Assert.assertNotNull( prop );
        Assert.assertTrue( prop.getProperty( Configuration.CONFIG_APIKEY ) != null );
        Assert.assertTrue( configFolder.list()[0].equals( Configuration.CONFIG_FILE_NAME ) );

        Assert.assertEquals( Configuration.getAccessToken(), prop.get( Configuration.CONFIG_ACCESSTOKEN ) );
        Assert.assertEquals( Configuration.getAPIKey(), prop.get( Configuration.CONFIG_APIKEY ) );
        Assert.assertEquals( Configuration.getNSid(), prop.get( Configuration.CONFIG_NSID ) );
        Assert.assertEquals( "" + Configuration.getServerPollMillisecons(),
                             prop.get( Configuration.CONFIG_SERVERPOLLMS ) );
        Assert.assertEquals( Configuration.getSharedSecret(), prop.get( Configuration.CONFIG_SHAREDSECRET ) );
        Assert.assertEquals( Configuration.getTokenSecret(), prop.get( Configuration.CONFIG_TOKENSECRET ) );
        Assert.assertEquals( Configuration.getUsername(), prop.get( Configuration.CONFIG_USERNAME ) );
        Assert.assertEquals( "" + Configuration.getNoTags(), prop.get( Configuration.CONFIG_NOTAGS ) );

        Assert.assertEquals( Configuration.getBasePath(),
                             System.getProperty( Configuration.USER_HOME ) + "/jflickrsync" );
    }

    @Test
    public void testSaveToken()
        throws IOException
    {
        Assert.assertTrue( Configuration.getNSid() == null );
        Assert.assertTrue( Configuration.getAccessToken() == null );
        Assert.assertTrue( Configuration.getTokenSecret() == null );
        Assert.assertTrue( Configuration.getUsername() == null );

        Configuration.saveToken( "abc", "def", "ghi", "jkl" );

        Assert.assertEquals( Configuration.getAccessToken(), "abc" );
        Assert.assertEquals( Configuration.getTokenSecret(), "def" );
        Assert.assertEquals( Configuration.getNSid(), "ghi" );
        Assert.assertEquals( Configuration.getUsername(), "jkl" );
    }
}
