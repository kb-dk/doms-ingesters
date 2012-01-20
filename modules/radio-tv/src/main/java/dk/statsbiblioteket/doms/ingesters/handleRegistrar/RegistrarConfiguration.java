package dk.statsbiblioteket.doms.ingesters.handleRegistrar;

import java.net.URL;

/**
 * Provide configuration for the handle registrar.
 */
public interface RegistrarConfiguration {
    String getFedoraLocation();

    String getUsername();

    String getPassword();

    URL getDomsWSAPIEndpoint();
}
