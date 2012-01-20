package dk.statsbiblioteket.doms.ingesters.handleRegistrar;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.statsbiblioteket.doms.client.DomsWSClient;
import dk.statsbiblioteket.doms.client.DomsWSClientImpl;
import dk.statsbiblioteket.doms.webservices.authentication.Base64;

import java.util.ArrayList;
import java.util.List;

/** Use Fedora risearch for performing Queries, domsClient for adding identifier
 * and a HandleAdministrator instance for registering handles. */
public class BasicHandleRegistrar implements HandleRegistrar {
    private static final Client client = Client.create();
    private final RegistrarConfiguration config;
    private final Log log = LogFactory.getLog(getClass());
    private int success = 0;
    private int failure = 0;

    public BasicHandleRegistrar(RegistrarConfiguration config) {
        this.config = config;
    }

    public void addHandles(String query, String urlPattern) {
        List<String> pids = findObjectFromQuery(query);
        for (String pid : pids) {
            try {
                log.debug("Adding handle to '" + pid + "'");
                String handle = addHandleToObject(pid);
                log.debug("registering handle '" + handle + "'");
                registerHandle(pid, handle, urlPattern);
                log.info("");
                success++;
            } catch (Exception e) {
                failure++;
                log.error("Error handling pid'" + pid + "'", e);
            }
        }
    }

    private void registerHandle(String pid, String handle, String urlPattern) {
        String url = String.format(urlPattern, pid);
        //TODO: Lookup handle in handleserver
        //TODO: If there and same URL, return
        //TODO: If there and different URL, update URL
        //TODO: If not there Add handle and URL in handle server
    }

    private String addHandleToObject(String pid) {
        DomsWSClient domsClient = new DomsWSClientImpl();
        domsClient.setCredentials(config.getDomsWSAPIEndpoint(),
                                  config.getUsername(), config.getPassword());
        String handle = "";//TODO: Generate handle from UUID ("hdl:109.1/<uuid> I think, must check syntax)
        //TODO: Read DC datastream
        //TODO: Check handle is not already there
        //TODO: If not
        //TODO:   add it to DC XML
        //TODO:   Unpublish
        //TODO:   Update DC datastream
        //TODO:   Publish
        return handle;
    }

    public List<String> findObjectFromQuery(String query)
            throws BackendInvalidCredsException, BackendMethodFailedException {
        try {
            String objects = client.resource(config.getFedoraLocation())
                    .path("/risearch").queryParam("type", "tuples")
                    .queryParam("lang", "iTQL").queryParam("format", "CSV")
                    .queryParam("flush", "true").queryParam("stream", "on")
                    .queryParam("query", query)
                    .header("Authorization", getBase64Creds())
                    .post(String.class);
            String[] lines = objects.split("\n");
            List<String> foundobjects = new ArrayList<String>();
            for (String line : lines) {
                if (line.startsWith("\"")) {
                    continue;
                }
                if (line.startsWith("info:fedora/")) {
                    line = line.substring("info:fedora/".length());
                }
                foundobjects.add(line);
            }
            return foundobjects;
        } catch (UniformInterfaceException e) {
            if (e.getResponse().getStatus() == ClientResponse.Status
                    .UNAUTHORIZED.getStatusCode()) {
                throw new BackendInvalidCredsException(
                        "Invalid Credentials Supplied", e);
            } else {
                throw new BackendMethodFailedException(
                        "Server error: " + e.getMessage(), e);
            }
        }

    }

    private String getBase64Creds() {
        return "Basic " + Base64.encodeBytes(
                (config.getUsername() + ":" + config.getPassword()).getBytes());
    }


}
