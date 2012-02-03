package dk.statsbiblioteket.doms.ingesters.handleRegistrar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * Configuration read by property file.
 */
public class PropertyBasedRegistrarConfiguration
        implements RegistrarConfiguration {
    private final Properties properties;
    private final Log log = LogFactory.getLog(getClass());
    public static final String FEDORA_LOCATION_KEY = "dk.statsbiblioteket.doms.ingesters.handleRegistrar.fedoraLocation";
    public static final String USER_NAME_KEY = "dk.statsbiblioteket.doms.ingesters.handleRegistrar.userName";
    public static final String PASSWORD_KEY = "dk.statsbiblioteket.doms.ingesters.handleRegistrar.password";
    public static final String DOMS_WS_API_ENDPOINT_KEY = "dk.statsbiblioteket.doms.ingesters.handleRegistrar.domsWSAPIEndpoint";

    public PropertyBasedRegistrarConfiguration(File propertiesFile) {
        this.properties = new Properties();
        try {
            properties.load(new FileInputStream(propertiesFile));
        } catch (IOException e) {
            throw new InitializationFailedException("Unable to load properties from '" + propertiesFile.getAbsolutePath() + "'", e);
        }
        log.debug("Read properties for '" + propertiesFile.getAbsolutePath() + "'");
    }

    @Override
    public String getFedoraLocation() {
        return properties.getProperty(FEDORA_LOCATION_KEY);
    }

    @Override
    public String getUsername() {
        return properties.getProperty(USER_NAME_KEY);
    }

    @Override
    public String getPassword() {
        return properties.getProperty(PASSWORD_KEY);
    }

    @Override
    public URL getDomsWSAPIEndpoint() {
        try {
            return new URL(properties.getProperty(DOMS_WS_API_ENDPOINT_KEY));
        } catch (MalformedURLException e) {
            throw new InitializationFailedException("Invalid property for '" + DOMS_WS_API_ENDPOINT_KEY + "'", e);
        }
    }
}
