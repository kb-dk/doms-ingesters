package dk.statsbiblioteket.doms.ingesters.handleRegistrar;

import junit.framework.TestCase;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Test properties load as expected.
 */
public class PropertyBasedRegistrarConfigurationTest extends TestCase {
    private RegistrarConfiguration config
            = new PropertyBasedRegistrarConfiguration(
            new File("src/test/resources/handleregistrar.properties"));

    public void testGetFedoraLocation() {
        assertEquals("http://alhena:7880/fedora", config.getFedoraLocation());
    }

    public void testGetUsername() {
        assertEquals("fedoraAdmin", config.getUsername());
    }

    public void testGetPassword() {
        assertEquals("fedoraAdminPass", config.getPassword());
    }

    public void testGetDomsWSAPIEndpoint() throws MalformedURLException {
        assertEquals(
                new URL("http://alhena:7880/centralWebservice-service/central/?wsdl"),
                config.getDomsWSAPIEndpoint());
    }

}
