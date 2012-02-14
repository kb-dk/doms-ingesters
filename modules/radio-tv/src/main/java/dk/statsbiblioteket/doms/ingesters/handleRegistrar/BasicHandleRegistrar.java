package dk.statsbiblioteket.doms.ingesters.handleRegistrar;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import dk.statsbiblioteket.doms.client.exceptions.ServerOperationFailed;
import dk.statsbiblioteket.util.xml.DefaultNamespaceContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.statsbiblioteket.doms.client.DomsWSClient;
import dk.statsbiblioteket.doms.client.DomsWSClientImpl;
import dk.statsbiblioteket.doms.webservices.authentication.Base64;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import net.handle.hdllib.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/** Use Fedora risearch for performing Queries, domsClient for adding identifier
 * and a HandleAdministrator instance for registering handles. */
public class BasicHandleRegistrar implements HandleRegistrar {
    private static final Client client = Client.create();
    private static final String HANDLE_URI_NAMESPACE = "hdl:";
    private static final String HANDLE_PREFIX = "109.3.1/"; //TODO: Config
    private static final String DC_IDENTIFIER_ELEMENT = "identifier";
    private static final String DC_DATASTREAM_ID = "DC";

    /** The admin ID */
    private static final String ADMIN_ID = "";   // Should this be empty?!?

    /** Charset used by the Handle system. */
    private static final Charset DEFAULT_ENCODING = Charset.forName("UTF8");

    /** Admin index aka Handle index, default value 300 */
    private static final int ADMIN_INDEX = 300;

    /** Handle admin record index. */
    private static final int ADMIN_RECORD_INDEX = 200;

    /** Handle URL record index. */
    private static final int URL_RECORD_INDEX = 1;

    /** Handle value references. */
    private static final ValueReference[] REFERENCES = null;

    /** Handle admin read permission. */
    private static final Boolean ADMIN_READ = true;

    /** Handle admin write permission. */
    private static final Boolean ADMIN_WRITE = true;

    /** Handle public read permission. */
    private static final Boolean PUBLIC_READ = true;

    /** Handle public write permission. */
    private static final Boolean PUBLIC_WRITE = false;

    /** Init a public key authentication information object. */
    private static PublicKeyAuthenticationInfo PUB_KEY_AUTH_INFO = null;

    private final RegistrarConfiguration config;
    private final Log log = LogFactory.getLog(getClass());
    private int success = 0;
    private int failure = 0;
    private static NamespaceContext dsNamespaceContext = new DefaultNamespaceContext();

    private static final String DC_NAMESPACE_URI
            = "http://purl.org/dc/elements/1.1/";

    private static final String DC_PREFIX = "dc";

    static {
        ((DefaultNamespaceContext) dsNamespaceContext).setNameSpace(DC_PREFIX,
                                                                    DC_NAMESPACE_URI);
    }

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

    /**
     * Register handle in the Handle-server, so that it resolves to a URL
     * generated from the given urlPattern.
     *
     * @param pid The PID of the DOMS-object in question.
     * @param handle The handle to be registered.
     * @param urlPattern The URL-pattern that makes a PID into a URL.
     * @throws RegisteringHandleFailedException If resolving the handle failed
     * unexpectedly.
     */
    private void registerHandle(String pid, String handle, String urlPattern)
            throws RegisteringHandleFailedException {
        String urlToRegister = String.format(urlPattern, pid);
        HandleValue values[] = new HandleValue[]{};
        boolean handleExists;

        // Lookup handle in handleserver
        try {
            values = new HandleResolver().resolveHandle(handle, null, null);
            if (values != null) {
                handleExists = true;
            } else {
                handleExists = false;
            }
        } catch (HandleException e) {  // True exception-handling, lol :)
            int exceptionCode = e.getCode();
            if (exceptionCode == HandleException.HANDLE_DOES_NOT_EXIST) {
                handleExists = false;
            } else {
                throw new RegisteringHandleFailedException("Did not succeed in "
                        + "resolving handle, existing or not.", e);
            }
        }

        if (handleExists) {
            // Handle was there, now find its url
            for (int i = 0; i < values.length; i++) {
                String type = values[i].getTypeAsString();
                if (type.equalsIgnoreCase("URL")) {
                    String urlAtServer = values[i].getDataAsString();

                    if (urlAtServer.equalsIgnoreCase(urlToRegister)) {
                        // It was the same url, so just return
                        return;
                    } else {
                        int indexAtServer = values[i].getIndex();
                        // It was a different url, replace it
                        replaceUrlOfHandleAtServer(handle, indexAtServer,
                                urlToRegister);
                    }
                }
            }
            // There was no url, so add it to the existing handle
            addUrlToHandleAtServer(handle, urlToRegister);

        } else {
            // If not there, add handle and url in handle server
            addHandleToServer(handle, urlToRegister);
        }
    }

    /**
     * Add URL to given handle at the server.
     *
     * @param handle Handle which needs to have url added
     * @param url The url to be used in the handle
     * @throws RegisteringHandleFailedException In case we couldn't add URL
     * to handle at the server.
     */
    private void addUrlToHandleAtServer(String handle, String url)
            throws RegisteringHandleFailedException {

        // Create the new value to be registered at the server. This will
        // be added to the given handle
        int timestamp = (int) (System.currentTimeMillis() / 1000);
        HandleValue newValue = new HandleValue(
                URL_RECORD_INDEX,                   // unique index
                "URL".getBytes(DEFAULT_ENCODING),   // handle value type
                url.getBytes(DEFAULT_ENCODING),     // value data
                HandleValue.TTL_TYPE_RELATIVE,
                Common.DEFAULT_SESSION_TIMEOUT,
                timestamp, REFERENCES,
                ADMIN_READ, ADMIN_WRITE,
                PUBLIC_READ, PUBLIC_WRITE);

        // Create the request to send and the resolver to send it
        AddValueRequest request =
                new AddValueRequest(handle.getBytes(DEFAULT_ENCODING),
                        newValue, PUB_KEY_AUTH_INFO);
        HandleResolver resolver = new HandleResolver();
        AbstractResponse response;

        // Let the resolver process the request
        try {
            response = resolver.processRequest(request);
        } catch (HandleException e) {
            throw new RegisteringHandleFailedException("Could not process the "
                    + "request to add URL to handle at the server.", e);
        }

        // Check the response to see if operation was successful
        if (response.responseCode == AbstractMessage.RC_SUCCESS) {
            // Resolution successful, hooray
        } else {
            throw new RegisteringHandleFailedException("Failed trying to "
                    + "add URL to handle at the server.");
        }
    }

    /**
     * Replace the URL of given handle at the server.
     *
     * @param handle Handle, the url of which has to be replaced
     * @param indexOfHandleValue The index of the value containing the url, for
     * the given handle.
     * @param url The new url, to be used in the handle instead of the existing
     * @throws RegisteringHandleFailedException In case we couldn't replace URL
     * of handle at the server.
     */
    private void replaceUrlOfHandleAtServer(String handle,
                                            int indexOfHandleValue, String url)
            throws RegisteringHandleFailedException {

        // Create the new value to be registered at the server. This will
        // replace the value on the server that has the same index.
        int timestamp = (int) (System.currentTimeMillis() / 1000);
        HandleValue replacementValue = new HandleValue(
                indexOfHandleValue,                 // unique index
                "URL".getBytes(DEFAULT_ENCODING),   // handle value type
                url.getBytes(DEFAULT_ENCODING),     // value data
                HandleValue.TTL_TYPE_RELATIVE,
                Common.DEFAULT_SESSION_TIMEOUT,
                timestamp, REFERENCES,
                ADMIN_READ, ADMIN_WRITE,
                PUBLIC_READ, PUBLIC_WRITE);

        // Create the request to send and the resolver to send it
        ModifyValueRequest request =
                new ModifyValueRequest(handle.getBytes(DEFAULT_ENCODING),
                        replacementValue, PUB_KEY_AUTH_INFO);
        HandleResolver resolver = new HandleResolver();
        AbstractResponse response;

        // Let the resolver process the request
        try {
            response = resolver.processRequest(request);
        } catch (HandleException e) {
            throw new RegisteringHandleFailedException("Could not process the "
                    + "request to replace URL of handle at the server.", e);
        }

        // Check the response to see if operation was successful
        if (response.responseCode == AbstractMessage.RC_SUCCESS) {
            // Resolution successful, hooray
        } else {
            throw new RegisteringHandleFailedException("Failed trying to "
                    + "replace URL of handle at the server.");
        }
    }

    /**
     * Add given handle to the handle server, so that the handle resolves to the
     * given url.
     *
     * @param handle Handle to be added to handle server.
     * @param url Url that the handle should resolve to.
     * @throws RegisteringHandleFailedException In case we couldn't create a new
     * handle at the server.
     */
    private void addHandleToServer(String handle, String url)
            throws RegisteringHandleFailedException {

        // Define the admin record for the handle we want to create
        AdminRecord admin = new AdminRecord(
                ADMIN_ID.getBytes(DEFAULT_ENCODING),
                ADMIN_INDEX, AdminRecord.PRM_ADD_HANDLE,
                AdminRecord.PRM_DELETE_HANDLE, AdminRecord.PRM_ADD_NA,
                AdminRecord.PRM_DELETE_NA, AdminRecord.PRM_READ_VALUE,
                AdminRecord.PRM_MODIFY_VALUE, AdminRecord.PRM_REMOVE_VALUE,
                AdminRecord.PRM_ADD_VALUE, AdminRecord.PRM_MODIFY_ADMIN,
                AdminRecord.PRM_REMOVE_ADMIN, AdminRecord.PRM_ADD_ADMIN,
                AdminRecord.PRM_LIST_HANDLES);

        // Make a create-handle request.
        int timestamp = (int) (System.currentTimeMillis() / 1000);
        HandleValue values[] = {
                new HandleValue(ADMIN_RECORD_INDEX,       // unique index
                        "HS_ADMIN".getBytes(DEFAULT_ENCODING),
                        Encoder.encodeAdminRecord(admin),
                        HandleValue.TTL_TYPE_RELATIVE,
                        Common.DEFAULT_SESSION_TIMEOUT,
                        timestamp, REFERENCES,
                        ADMIN_READ, ADMIN_WRITE,
                        PUBLIC_READ, PUBLIC_WRITE),

                new HandleValue(URL_RECORD_INDEX,         // unique index
                        "URL".getBytes(DEFAULT_ENCODING), // handle value type
                        url.getBytes(DEFAULT_ENCODING),   // value data
                        HandleValue.TTL_TYPE_RELATIVE,
                        Common.DEFAULT_SESSION_TIMEOUT,
                        timestamp, REFERENCES,
                        ADMIN_READ, ADMIN_WRITE,
                        PUBLIC_READ, PUBLIC_WRITE)
        };

        // Create the request to send and the resolver to send it
        CreateHandleRequest request =
                new CreateHandleRequest(handle.getBytes(DEFAULT_ENCODING),
                        values, PUB_KEY_AUTH_INFO);
        HandleResolver resolver = new HandleResolver();
        AbstractResponse response;

        // Let the resolver process the request
        try {
            response = resolver.processRequest(request);
        } catch (HandleException e) {
            throw new RegisteringHandleFailedException("Could not process the "
                    + "request to create a new handle at the server.", e);
        }

        // Check the response to see if operation was successful
        if (response.responseCode == AbstractMessage.RC_SUCCESS) {
            // Resolution successful, hooray
        } else {
            throw new RegisteringHandleFailedException("Failed trying to "
                    + "create a new handle at the server.");
        }
    }

    private String addHandleToObject(String pid) {
        DomsWSClient domsClient = new DomsWSClientImpl();
        domsClient.setCredentials(config.getDomsWSAPIEndpoint(),
                                  config.getUsername(), config.getPassword());

        // Generate handle from UUID
        String handle = HANDLE_URI_NAMESPACE + HANDLE_PREFIX + dk.statsbiblioteket.doms.client.utils.Constants.ensurePID(pid);
        //Read DC datastream
        Document dataStream;
        try {
            dataStream = domsClient.getDataStream(pid, DC_DATASTREAM_ID);
        } catch (ServerOperationFailed serverOperationFailed) {
            throw new BackendMethodFailedException("Backendmethod failed while trying to to read DC from '" + pid + "'", serverOperationFailed);
        }
        //TODO: Consider JAXB over document manipulation
        //Check handle is not already there
        boolean found = false;
        XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(dsNamespaceContext);
        NodeList result;
        try {
            result = (NodeList) xPath.evaluate("//" + DC_PREFIX + ":" + DC_IDENTIFIER_ELEMENT, new DOMSource(dataStream), XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new InconsistentDataException("Unexpected trouble working with DC datastream from '" + pid + "'", e);
        }
        if (result.getLength() == 0) {
            throw new InconsistentDataException("Unexpected trouble working with DC datastream from '" + pid + "': No dc:identifier elements");
        }
        Node item;
        int i = 0;
        do {
            if ((item = result.item(i++)).getTextContent().trim().equals(handle)) {
                found = true;
            }
        } while (!found && i < result.getLength());
        //If not
        if (!found) {
            //Add it to DC XML
            Node newItem = dataStream.createElementNS(DC_NAMESPACE_URI,
                                                      DC_IDENTIFIER_ELEMENT);
            newItem.setTextContent(handle);
            item.getParentNode().insertBefore(newItem, item.getNextSibling());
            //Unpublish
            try {
                domsClient.unpublishObjects("Prepare to add handle PID", pid);
                //Update DC datastream
                domsClient.updateDataStream(pid, DC_DATASTREAM_ID, dataStream, "Added handle PID");
                //Publish
                domsClient.publishObjects("Prepare to add handle PID", pid);
            } catch (ServerOperationFailed serverOperationFailed) {
                throw new BackendMethodFailedException("Backendmethod failed while trying to add handle '" + handle + "' to '" + pid + "'", serverOperationFailed);
            }
        }
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
            List<String> foundObjects = new ArrayList<String>();
            for (String line : lines) {
                if (line.startsWith("\"")) {
                    continue;
                }
                if (line.startsWith("info:fedora/")) {
                    line = line.substring("info:fedora/".length());
                }
                foundObjects.add(line);
            }
            return foundObjects;
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
