package dk.statsbiblioteket.doms.ingesters.handleRegistrar;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.statsbiblioteket.doms.client.DomsWSClient;
import dk.statsbiblioteket.doms.client.DomsWSClientImpl;
import dk.statsbiblioteket.doms.client.exceptions.ServerOperationFailed;
import dk.statsbiblioteket.doms.webservices.authentication.Base64;
import dk.statsbiblioteket.util.xml.DefaultNamespaceContext;

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
     * @throws HandleException If resolving the handle failed unexpectedly.
     */
    private void registerHandle(String pid, String handle, String urlPattern)
            throws HandleException {
        String url = String.format(urlPattern, pid);
        HandleValue values[];
        boolean handleExists = false;

        // Lookup handle in handleserver
        try {
            values = new HandleResolver().resolveHandle(handle, null, null);
            if (handleExistsAmongValues(values, handle)) {
                handleExists = true;
            }

        } catch (HandleException e) {  // True exception-handling, lol :)
            int exceptionCode = e.getCode();
            if (exceptionCode == HandleException.HANDLE_ALREADY_EXISTS) {
                handleExists = true;
            } else if (exceptionCode == HandleException.HANDLE_DOES_NOT_EXIST) {
                handleExists = false;
            } else {
                throw e;  // TODO is this the right way to do it?
            }
        }

        if (handleExists) {
            //TODO: If there and same URL, return

            //TODO: If there and different URL, update URL

        } else {
            //TODO: If not there Add handle and URL in handle server

        }
    }

    /**
     * TODO javadoc
     * @param values
     * @param handle
     * @return
     */
    private boolean handleExistsAmongValues(HandleValue values[],
                                            String handle) {
        //TODO implement
        return false; // TODO redefine
    }

    private String addHandleToObject(String pid) {
        DomsWSClient domsClient = new DomsWSClientImpl();
        domsClient.setCredentials(config.getDomsWSAPIEndpoint(),
                                  config.getUsername(), config.getPassword());

        // Generate handle from UUID ("hdl:109.1/<uuid> I think, must check syntax)
        String handle = HANDLE_URI_NAMESPACE + HANDLE_PREFIX + dk.statsbiblioteket.doms.client.utils.Constants.ensurePID(pid);//TODO: Generate handle from UUID ("hdl:109.3.1/<uuid>" I think, must check syntax)
        //Read DC datastream
        Document dataStream = null;
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
        NodeList result = null;
        try {
            result = (NodeList) xPath.evaluate("//" + DC_PREFIX + ":" + DC_IDENTIFIER_ELEMENT, new DOMSource(dataStream), XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new InconsistentDataException("Unexpected trouble working with DC datastream from '" + pid + "'", e);
        }
        if (result.getLength() == 0) {
            throw new InconsistentDataException("Unexpected trouble working with DC datastream from '" + pid + "': No dc.identifier elements");
        }
        Node item = null;
        for (int i = 0; i < result.getLength(); i++) {
            item = result.item(i);
            if (item.getTextContent().trim().equals(handle)) {
                found = true;
            }
        }
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
