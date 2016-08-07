package org.jflickrsync.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.flickr4java.flickr.util.IOUtilities;

public class Configuration
{
    private static final Logger logger = Logger.getLogger( Configuration.class );

    public static final String USER_HOME = "user.home";

    private static final String PROPERTIES_COMMENTS = "jfilesync configuration";

    private static final String CONFIG_FILE_NAME = "config.properties";

    private static final String CONFIG_FILE_CLASSPATH = "/org/flickrsync/main/config.properties";

    public static final String CONFIG_FOLDER_NAME = ".jflickrsync";

    private static Properties configuration;

    public static final String CONFIG_APIKEY = "apikey";

    public static final String CONFIG_SHAREDSECRET = "sharedsecret";

    public static final String CONFIG_ACCESSTOKEN = "accesstoken";

    public static final String CONFIG_TOKENSECRET = "tokensecret";

    public static final String CONFIG_NSID = "nsid";

    public static final String CONFIG_USERNAME = "username";

    public static final String CONFIG_SETTAG = "settag";

    public static final String CONFIG_REPLACESPACES = "replacespaces";

    public static final String CONFIG_NOTAGS = "notags";

    public static final String CONFIG_PRIVACY = "privacy";

    public static final String CONFIG_BASEPATH = "basepath";

    public static final String CONFIG_SERVERPOLLMS = "serverpollms";

    /**
     * #SYNCTYPE CAN BE: <br>
     * #TOTAL -> everything should be synced. Local and Server must be exactly equal. <br>
     * #LOCAL -> local must have all the server files, but it doesn't sync from local to server (if you add a file to
     * local it will not be uploaded to server) </br>
     * #SERVER -> Server must have all the local files, but it doesn't sync from server to local (if you add a file to
     * server it will not be downloaded to local)
     */
    public static final String CONFIG_SYNCTYPE = "synctype";

    public static final int SYNCTYPE_TOTAL = 1;

    public static final int SYNCTYPE_LOCAL = 2;

    public static final int SYNCTYPE_SERVER = 3;

    // Private constructor
    private Configuration()
    {

    }

    public static long getServerPollMillisecons()
    {
        try
        {
            String soption = (String) getConfiguration().get( CONFIG_SERVERPOLLMS );
            return Long.valueOf( soption );
        }
        catch ( Exception e )
        {
            logger.error( e );
            return 30000l;
        }

    }

    public static String getUserHome()
    {
        return System.getProperty( Configuration.USER_HOME );
    }

    public static String getUsername()
    {
        try
        {
            return (String) getConfiguration().get( CONFIG_USERNAME );
        }
        catch ( Exception e )
        {
            logger.error( e );
            return "";
        }
    }

    /** local photos path, after the calculation of variables */
    private static String sBasePath = null;

    public static String getBasePath()
    {
        try
        {
            if ( sBasePath == null )
            {
                // second let's get the base path
                sBasePath = ( (String) getConfiguration().get( CONFIG_BASEPATH ) ).replaceAll( "\\{"
                    + Configuration.USER_HOME + "}", Configuration.getUserHome() );

                File fBasePath = new File( Configuration.getBasePath() );
                if ( !fBasePath.exists() )
                {
                    fBasePath.mkdirs();
                }
            }

            return sBasePath;
        }
        catch ( Exception e )
        {
            logger.error( e );
            return "";
        }
    }

    public static int getPrivacy()
    {
        try
        {
            String soption = (String) getConfiguration().get( CONFIG_PRIVACY );
            return Integer.valueOf( soption );
        }
        catch ( Exception e )
        {
            logger.error( e );
            return 5;
        }
    }

    public static int getSyncType()
    {
        try
        {
            String soption = (String) getConfiguration().get( CONFIG_SYNCTYPE );
            if ( soption.equalsIgnoreCase( "total" ) )
            {
                return SYNCTYPE_TOTAL;
            }
            else if ( soption.equalsIgnoreCase( "server" ) )
            {
                return SYNCTYPE_SERVER;
            }
            else
            {
                return SYNCTYPE_LOCAL;
            }
        }
        catch ( Exception e )
        {
            logger.error( e );
            return SYNCTYPE_TOTAL;
        }
    }

    public static boolean getReplaceSpaces()
    {
        try
        {
            String soption = (String) getConfiguration().get( CONFIG_REPLACESPACES );
            return Boolean.valueOf( soption );
        }
        catch ( Exception e )
        {
            logger.error( e );
            return true;
        }
    }

    public static boolean getNoTags()
    {
        try
        {
            String soption = (String) getConfiguration().get( CONFIG_NOTAGS );
            return Boolean.valueOf( soption );
        }
        catch ( Exception e )
        {
            logger.error( e );
            return true;
        }
    }

    public static boolean getSetTag()
    {
        try
        {
            String ssettag = (String) getConfiguration().get( CONFIG_SETTAG );
            return Boolean.valueOf( ssettag );
        }
        catch ( Exception e )
        {
            logger.error( e );
            return true;
        }
    }

    public static String getNSid()
    {
        try
        {
            return (String) getConfiguration().get( CONFIG_NSID );
        }
        catch ( Exception e )
        {
            logger.error( e );
            return "";
        }
    }

    public static String getTokenSecret()
    {
        try
        {
            return (String) getConfiguration().get( CONFIG_TOKENSECRET );
        }
        catch ( Exception e )
        {
            logger.error( e );
            return "";
        }
    }

    public static String getSharedSecret()
    {
        try
        {
            return (String) getConfiguration().get( CONFIG_SHAREDSECRET );
        }
        catch ( Exception e )
        {
            logger.error( e );
            return "";
        }
    }

    public static String getAccessToken()
    {
        try
        {
            return (String) getConfiguration().get( CONFIG_ACCESSTOKEN );
        }
        catch ( Exception e )
        {
            logger.error( e );
            return "";
        }
    }

    public static String getAPIKey()
    {
        try
        {
            return (String) getConfiguration().get( CONFIG_APIKEY );
        }
        catch ( Exception e )
        {
            logger.error( e );
            return "";
        }
    }

    /**
     * Returns the configuration folder at the user home directory
     * 
     * @return
     */
    public static String getConfigurationUserHomeFolder()
    {
        String userHomeConfigFolder = System.getProperty( USER_HOME ) + File.separatorChar + CONFIG_FOLDER_NAME;
        return userHomeConfigFolder;
    }

    /**
     * returns the configuration file path
     * 
     * @return
     */
    public static String getConfigurationFilePath()
    {
        String userHomeConfigFolder = getConfigurationUserHomeFolder();
        String userHomeConfigFile = CONFIG_FILE_NAME;
        return userHomeConfigFolder + File.separatorChar + userHomeConfigFile;
    }

    /**
     * Obtain the configuration. This configuration is stored at the user home directory /.jflickrsync/config.properties
     * 
     * @return {@link Properties}
     * @throws IOException
     */
    public static Properties getConfiguration()
        throws IOException
    {
        if ( configuration == null )
        {
            String userHomeConfigFolder = getConfigurationUserHomeFolder();

            File fUserHomeConfigFile = new File( getConfigurationFilePath() );
            if ( !fUserHomeConfigFile.exists() || !fUserHomeConfigFile.isFile() )
            {
                // we need to load the default configuration
                InputStream in = null;
                try
                {
                    in = Configuration.class.getResourceAsStream( CONFIG_FILE_CLASSPATH );
                    configuration = new Properties();
                    configuration.load( in );
                }
                finally
                {
                    IOUtilities.close( in );
                }

                // we store the basic config into the user home directory
                File fUserHomeConfigFolder = new File( userHomeConfigFolder );
                fUserHomeConfigFolder.mkdirs();
                FileOutputStream fos = new FileOutputStream( fUserHomeConfigFile );
                try
                {
                    configuration.store( fos, PROPERTIES_COMMENTS );
                }
                finally
                {
                    IOUtilities.close( fos );
                }
            }
            else
            {
                // the configuration at user home directory exists, lets load that
                configuration = new Properties();
                FileInputStream fis = new FileInputStream( fUserHomeConfigFile );
                try
                {
                    configuration.load( fis );
                }
                finally
                {
                    IOUtilities.close( fis );
                }
            }

        }
        return configuration;
    }

    /**
     * Save the token obtained for the user with the api key and secret, allowing this application
     * 
     * @param accessToken {@link String} token obtained
     * @param tokenSecret {@link String} token secret obtained
     * @throws IOException
     */
    public static void saveToken( String accessToken, String tokenSecret, String nsid, String username )
        throws IOException
    {
        getConfiguration().put( CONFIG_ACCESSTOKEN, accessToken );
        getConfiguration().put( CONFIG_TOKENSECRET, tokenSecret );
        getConfiguration().put( CONFIG_NSID, nsid );
        getConfiguration().put( CONFIG_USERNAME, username );

        String userHomeConfigFolder = System.getProperty( USER_HOME ) + File.separatorChar + CONFIG_FOLDER_NAME;
        String userHomeConfigFile = CONFIG_FILE_NAME;
        File fUserHomeConfigFile = new File( userHomeConfigFolder + File.separatorChar + userHomeConfigFile );
        FileOutputStream fos = new FileOutputStream( fUserHomeConfigFile );
        try
        {
            configuration.store( fos, PROPERTIES_COMMENTS );
        }
        finally
        {
            IOUtilities.close( fos );
        }
    }
}
