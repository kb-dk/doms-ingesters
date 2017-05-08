package dk.statsbiblioteket.doms.ingesters.radiotv;

import com.sun.jersey.api.client.WebResource;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.EnhancedFedora;
import dk.statsbiblioteket.doms.central.connectors.fedora.pidGenerator.PIDGeneratorException;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.FedoraRelation;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.SearchResult;
import dk.statsbiblioteket.doms.central.connectors.fedora.templates.ObjectIsWrongTypeException;
import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XPathSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.ComparisonType;
import org.xmlunit.diff.Diff;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Code to create programs.
 */
public class RecordCreator {

    public static final String DC_NAMESPACE = "http://purl.org/dc/elements/1.1/";
    public static final String PBCORE_DESCRIPTION_ELEMENT = "//program/pbcore/pbc:PBCoreDescriptionDocument";
    public static final String PROGRAM_TEMPLATE_PID = "doms:Template_Program";
    public static final String PROGRAM_PBCORE_DS_ID = "PBCORE";
    public static final String RITZAU_ORIGINAL_DS_ID = "RITZAU_ORIGINAL";
    public static final String GALLUP_ORIGINAL_DS_ID = "GALLUP_ORIGINAL";
    public static final String PROGRAM_BROADCAST_DS_ID = "PROGRAM_BROADCAST";
    public static final String DC_DS_ID = "DC";
    public static final String HAS_FILE_RELATION = "http://doms.statsbiblioteket.dk/relations/default/0/1/#hasFile";

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final EnhancedFedora domsClient;
    private final boolean overwrite;
    private final boolean check;
    private final DocumentBuilder documentBuilder;
    private final XPathSelector xPathSelector;


    public RecordCreator(EnhancedFedora domsClient, boolean overwrite, boolean check) {
        this.domsClient = domsClient;
        this.overwrite = overwrite;
        this.check = check;
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Failed to understand xml...", e);
        }
        //This is not threadsafe so it must be created here, as we make a new RecordCreater for each file to ingest
        xPathSelector = DOM.createXPathSelector(
                "dc", DC_NAMESPACE,
                "pbc", "http://www.pbcore.org/PBCore/PBCoreNamespace.html",
                "ritzau", "http://doms.statsbiblioteket.dk/types/ritzau_original/0/1/#",
                "gallup", "http://doms.statsbiblioteket.dk/types/gallup_original/0/1/#",
                "pb", "http://doms.statsbiblioteket.dk/types/program_broadcast/0/1/#");
    }

    /**
     * Ingests or updates a program object.
     *
     * Synchronized on this object, not class, as the xpath system for reading the source file is not thread safe
     *
     *
     * @param radioTVMetadata Bibliographical metadata about the program.
     * @param filename the name of the source file. Is only used for logging and doms audit comments
     * @return PID of the newly created program object, created by the DOMS.
     *
     * @throws OperationFailed    if creation or manipulation of the program object fails.
     * @throws MalformedURLException if a file element contains an invalid URL.
     */
    public synchronized String ingestProgram(Document radioTVMetadata, String filename) throws MalformedURLException, OverwriteException, OperationFailed {
        try {
            // Get pids of referenced files - do this first, to ensure fail-early in case of missing files.
            List<String> filePIDs = getFilePids(radioTVMetadata);
            log.debug("Found pids {} of referenced files", filePIDs);

            // Find or create program object.
            List<String> oldIdentifiers = getOldIdentifiers(radioTVMetadata);
            log.debug("Found these old identifiers {} in the program to ingest", oldIdentifiers);

            String programObjectPID = alreadyExistsInRepo(oldIdentifiers);
            if (programObjectPID != null) {
                if (check) {
                    log.debug("Preparing to check semantic equivalence of pid={}", programObjectPID);
                    if (checkSemanticIdentity(programObjectPID, radioTVMetadata, filePIDs)) {
                        //check if what is there is identical to what we want to write
                        log.debug("Object pid={} is semantically identical, so no updates is performed.",
                                  programObjectPID);
                        return programObjectPID;
                    } else {
                        log.debug("Object pid={} is not semantically identical.");
                    }
                }
                if (overwrite) { //overwrite whatever is there
                    prepareProgramForOverwrite(programObjectPID, filename, oldIdentifiers);
                } else { //fail
                    throw new OverwriteException(
                            "Found existing object pid='" + programObjectPID + "' and overwrite flag is false");
                }
            } else {
                log.debug("Old identifiers {} did not find a program object in doms", oldIdentifiers);
                programObjectPID = createNewProgramObject(filename, oldIdentifiers);
            }

            //Set label as title
            setTitle(radioTVMetadata, filename, programObjectPID);

            //Add/update the datastreams
            String datastreamComment = Util.domsCommenter(filename, "updated datastream");
            addPBCore(radioTVMetadata, programObjectPID, datastreamComment);
            addRitzau(radioTVMetadata, programObjectPID, datastreamComment);
            addGallup(radioTVMetadata, programObjectPID, datastreamComment);
            addBroadcast(radioTVMetadata, programObjectPID, datastreamComment);

            //Set the relations to the data files
            setFileRelations(programObjectPID, filePIDs, filename);

            return programObjectPID;
        } catch (PIDGeneratorException | TransformerException | ObjectIsWrongTypeException| BackendInvalidResourceException | BackendInvalidCredsException | BackendMethodFailedException e){
            throw new OperationFailed(e);
        }
    }

    private boolean checkSemanticIdentity(String programObjectPID, Document radioTVMetadata, List<String> filePIDs) throws BackendInvalidResourceException, BackendInvalidCredsException, BackendMethodFailedException {
        //Title
        String expectedTitle = getTitle(radioTVMetadata);
        String actualTitle = domsClient.getLimitedObjectProfile(programObjectPID,null).getLabel();
        boolean titleIdentical = expectedTitle.equals(actualTitle);

        //PBCore
        Document pbCoreExpected = createDocumentFromNode(radioTVMetadata, PBCORE_DESCRIPTION_ELEMENT);
        Document pbCoreActual = DOM.stringToDOM(domsClient.getXMLDatastreamContents(programObjectPID, PROGRAM_PBCORE_DS_ID),true);
        boolean pbcoreIdentical = compareDocuments(pbCoreExpected, pbCoreActual, programObjectPID);

        //Ritzau
        Document ritzauExpected = createDocumentFromNode(radioTVMetadata,"//program/originals/ritzau:ritzau_original");
        Document ritzauActual = DOM.stringToDOM(domsClient.getXMLDatastreamContents(programObjectPID, RITZAU_ORIGINAL_DS_ID), true);
        boolean ritzauIdentical = compareDocuments(ritzauExpected, ritzauActual, programObjectPID);

        //Gallup
        Document gallupExpected = createDocumentFromNode(radioTVMetadata, "//program/originals/gallup:gallup_original|//program/originals/gallup:tvmeterProgram");
        Document gallupActual = DOM.stringToDOM(domsClient.getXMLDatastreamContents(programObjectPID, GALLUP_ORIGINAL_DS_ID), true);
        boolean gallupIdentical = compareDocuments(gallupExpected, gallupActual, programObjectPID);

        //Broadcast
        Document broadcastExpected = createDocumentFromNode(radioTVMetadata, "//program/pb:programBroadcast");
        Document broadcastActual = DOM.stringToDOM(domsClient.getXMLDatastreamContents(programObjectPID,PROGRAM_BROADCAST_DS_ID),true);
        boolean broadcastIdentical = compareDocuments(broadcastExpected, broadcastActual, programObjectPID);

        //Relations
        boolean relationsIdentical = checkFileRelations(programObjectPID,filePIDs);

        return titleIdentical && pbcoreIdentical && ritzauIdentical && gallupIdentical && broadcastIdentical && relationsIdentical;

    }


    private boolean compareDocuments(Document expected, Document actual, String pid){
        Source control = Input.fromDocument(expected).build();
        Source test = Input.fromDocument(actual).build();

        Diff d = Util.xmlDiff(control,test);

        if (!d.hasDifferences()) {
            return true;
        } else {
            log.debug("Differences from object {}.,differences='{}' ", pid, d.toString());
            return false;
        }
    }


    private void setFileRelations(String programObjectPID, List<String> filePIDs, String filename) throws BackendMethodFailedException, BackendInvalidResourceException, BackendInvalidCredsException {
        List<FedoraRelation> relations = domsClient.getNamedRelations(
                programObjectPID, HAS_FILE_RELATION, null);

        HashSet<String> existingRels = new HashSet<String>();
        for (FedoraRelation relation : relations) {
            String subjectPid = relation.getSubject(); //This should be prograObjectPid
            String predicate = relation.getPredicate(); //This should be HAS_FILE_RELATION
            String objectPid = relation.getObject(); //This should be the pid of the file object

                log.debug("Found relation {},'{}',{}", subjectPid, predicate, objectPid);
                if (!filePIDs.contains(objectPid)) {
                    log.debug("Removing relation relation {},'{}',{}", subjectPid, predicate, objectPid);
                    String comment = Util.domsCommenter(filename, "removed relation '{0}' to '{1}'",
                                                        predicate,
                                                        objectPid);
                    domsClient.deleteRelation(programObjectPID,subjectPid,predicate,objectPid,false,comment);
                } else {
                    existingRels.add(objectPid);
                }

        }
        for (String filePID : filePIDs) {
            if (!existingRels.contains(filePID)) {
                log.debug("Adding relation {},'{}',{}", programObjectPID, HAS_FILE_RELATION, filePID);
                String comment = Util.domsCommenter(filename, "added relation '{0}' to '{1}'",
                                                    HAS_FILE_RELATION, filePID);
                domsClient.addRelation(programObjectPID,programObjectPID, HAS_FILE_RELATION, filePID, false, comment);

            }
        }
    }


    private boolean checkFileRelations(String programObjectPID, List<String> filePIDs) throws BackendMethodFailedException, BackendInvalidResourceException, BackendInvalidCredsException {
        boolean identical = true;

        List<FedoraRelation> relations = domsClient.getNamedRelations(
                programObjectPID, HAS_FILE_RELATION, null);
        HashSet<String> existingRels = new HashSet<String>();
        for (FedoraRelation relation : relations) {
                String predicate = relation.getPredicate(); //This should be HAS_FILE_RELATION
                String objectPid = relation.getObject(); //This should be prograObjectPid
                String subjectPid = relation.getSubject();
                log.debug("Found relation {},'{}',{}", subjectPid, predicate, objectPid);
                if (!filePIDs.contains(objectPid)) {
                    log.debug("Found extranous relation {},'{}',{}", subjectPid, predicate, objectPid);
                    identical = false;
                } else {
                    existingRels.add(objectPid);
                }

        }
        for (String filePID : filePIDs) {
            if (!existingRels.contains(filePID)) {
                log.debug("Missing relation {},'{}',{}", programObjectPID, HAS_FILE_RELATION, filePID);
                identical = false;
            }
        }
        return identical;
    }

    private void addBroadcast(Document radioTVMetadata, String objectPID, String comment) throws TransformerException, BackendMethodFailedException, BackendInvalidResourceException, BackendInvalidCredsException {
        // Add the program broadcast datastream
        log.debug("Adding/Updating {} datastream", PROGRAM_BROADCAST_DS_ID);
        Document programBroadcastDocument = createDocumentFromNode(radioTVMetadata, "//program/pb:programBroadcast");
        String broadcastString = DOM.domToString(programBroadcastDocument);
        domsClient.modifyDatastreamByValue(objectPID, PROGRAM_BROADCAST_DS_ID, broadcastString,null, comment);
    }

    private void addGallup(Document radioTVMetadata, String objectPID, String comment) throws TransformerException, BackendMethodFailedException, BackendInvalidResourceException, BackendInvalidCredsException {
        // Add the Gallup datastream
        log.debug("Adding/Updating {} datastream", GALLUP_ORIGINAL_DS_ID);
        Document gallupOriginalDocument = createDocumentFromNode(radioTVMetadata,
                                                                 "//program/originals/gallup:gallup_original|//program/originals/gallup:tvmeterProgram");
        String gallupString = DOM.domToString(gallupOriginalDocument);
        domsClient.modifyDatastreamByValue(objectPID, GALLUP_ORIGINAL_DS_ID, gallupString,null, comment);
    }

    private void addRitzau(Document radioTVMetadata, String objectPID, String comment) throws BackendMethodFailedException, BackendInvalidResourceException, BackendInvalidCredsException, TransformerException {
        // Add Ritzau datastream
        log.debug("Adding/Updating {} datastream", RITZAU_ORIGINAL_DS_ID);
        Document ritzauOriginalDocument = createDocumentFromNode(radioTVMetadata,
                                                                 "//program/originals/ritzau:ritzau_original");
        String ritzauString = DOM.domToString(ritzauOriginalDocument);
        domsClient.modifyDatastreamByValue(objectPID, RITZAU_ORIGINAL_DS_ID, ritzauString,null, comment);
    }

    private void addPBCore(Document radioTVMetadata, String objectPID, String comment) throws TransformerException, BackendMethodFailedException, BackendInvalidResourceException, BackendInvalidCredsException {
        // Add PBCore datastream
        log.debug("Adding/Updating {} datastream", PROGRAM_PBCORE_DS_ID);
        Document pbCoreDataStreamDocument = createDocumentFromNode(radioTVMetadata, PBCORE_DESCRIPTION_ELEMENT);
        String pbCoreAsString = DOM.domToString(pbCoreDataStreamDocument);
        domsClient.modifyDatastreamByValue(objectPID, PROGRAM_PBCORE_DS_ID, pbCoreAsString,null, comment);
    }

    private void setTitle(Document radioTVMetadata, String filename, String objectPID) throws BackendMethodFailedException, BackendInvalidResourceException, BackendInvalidCredsException {
        String programTitle = getTitle(radioTVMetadata);

        log.debug("Found program title '{}', setting this as label on {}", programTitle, objectPID);
        String comment = Util.domsCommenter(filename, "added program title '{0}' object label", programTitle);
        domsClient.modifyObjectLabel(objectPID, programTitle, comment);
    }

    private String getTitle(Document radioTVMetadata) {
        // Get the program title from the PBCore metadata and use that as the
        // object label for this program object.
        Node titleNode = xPathSelector.selectNode(radioTVMetadata,
                                                  "//pbc:pbcoreTitle[pbc:titleType=\"titel\"]/pbc:title");
        return titleNode.getTextContent();
    }

    private void prepareProgramForOverwrite(String existingPid, String filename, List<String> oldIdentifiers) throws BackendMethodFailedException, BackendInvalidResourceException, BackendInvalidCredsException, TransformerException {
        log.debug("Found existing object {}, to be overwritten", existingPid);
        String comment = Util.domsCommenter(filename, "unpublished object to allow for changes");
        domsClient.modifyObjectState(existingPid,EnhancedFedora.STATE_INACTIVE,comment);
        log.debug("Existing object {} unpublished", existingPid);
        addOldPids(existingPid, oldIdentifiers, filename);
        log.debug("Old identifiers added to program object {}", existingPid);
    }

    private String createNewProgramObject(String filename, List<String> oldIdentifiers) throws BackendMethodFailedException, PIDGeneratorException, ObjectIsWrongTypeException, BackendInvalidResourceException, BackendInvalidCredsException {
        // Create a program object in the DOMS
        String comment = Util.domsCommenter(filename, "creating Program Object");
        String programObjectPID = domsClient.cloneTemplate(PROGRAM_TEMPLATE_PID, oldIdentifiers, comment);
        log.debug("Created new program object with pid {}", programObjectPID);
        return programObjectPID;
    }

    private void addOldPids(String existingPid, List<String> oldIdentifiers, String filename) throws BackendMethodFailedException, BackendInvalidResourceException, BackendInvalidCredsException, TransformerException {
        Document dcDataStream = DOM.stringToDOM(domsClient.getXMLDatastreamContents(existingPid, DC_DS_ID),true);
        NodeList existingIDNodes = xPathSelector.selectNodeList(dcDataStream, "//dc:identifier");
        Set<String> idsToAdd = new HashSet<String>(oldIdentifiers);
        for (int i = 0; i < existingIDNodes.getLength(); i++) {
            idsToAdd.remove(existingIDNodes.item(i).getTextContent());
        }
        log.debug("Object {} is missing these {} old identifiers", existingPid, idsToAdd);
        if (idsToAdd.isEmpty()) {
            return;
        }

        Node firstIdentifierNode = existingIDNodes.item(0);
        for (String id : idsToAdd) {
            Element newIdentifier = dcDataStream.createElementNS(DC_NAMESPACE, "identifier");
            dcDataStream.setTextContent(id);
            firstIdentifierNode.getParentNode().insertBefore(newIdentifier, firstIdentifierNode);
            log.debug("Adding {} to dc identifiers for object {}", id, existingPid);
        }
        log.debug("Updating {} datastream with new old identifiers {}", DC_DS_ID, oldIdentifiers);
        String comment = Util.domsCommenter(filename, "added old identifiers {0}", oldIdentifiers);
        String dcDatastreamAsString = DOM.domToString(dcDataStream);
        domsClient.modifyDatastreamByValue(existingPid, DC_DS_ID, dcDatastreamAsString,null, comment);
    }

    /**
     * Utility method to create a document to ingest from a node.
     *
     * @param radioTVMetadata Bibliographical metadata about the program.
     * @param xpath           XPath expression to select and ingest in node.
     * @return A document containing the metadata form the node selected by xpath.
     */
    private Document createDocumentFromNode(Document radioTVMetadata, String xpath) {
        final Node nodeToIngest = xPathSelector.selectNode(radioTVMetadata, xpath);

        // Build a data document for the data stream in the program object.
        final Document document = documentBuilder.newDocument();
        document.appendChild(document.importNode(nodeToIngest, true));
        return document;
    }

    /**
     * Lookup a program in DOMS.
     * If program exists, returns the PID of the program. Otherwise returns null.
     *
     * @param oldIdentifiers List of old identifiers to look up.
     * @return PID of program, if found. Null otherwise
     */
    private String alreadyExistsInRepo(List<String> oldIdentifiers) throws BackendInvalidCredsException, BackendMethodFailedException {
        for (String oldId : oldIdentifiers) {
            //TODO Remove this when fixed in doms central RI query
            oldId = oldId.replaceAll("'", Matcher.quoteReplacement("\\'"));

            List<SearchResult> hits = domsClient.fieldsearch(
                    oldId, 0, 1);

            if (!hits.isEmpty()){
                return hits.get(0).getPid();
            }
        }
        return null;
    }

    /**
     * Find old identifiers in program metadata, and use them for looking up programs in DOMS.
     *
     * @param radioTVMetadata The document containing the program metadata.
     * @return Old indentifiers found.
     */
    private List<String> getOldIdentifiers(Document radioTVMetadata) {
        List<String> result = new ArrayList<String>();

        Node radioTVPBCoreElement = xPathSelector.selectNode(radioTVMetadata, PBCORE_DESCRIPTION_ELEMENT);

        Node oldRitzauPIDNode = xPathSelector.selectNode(radioTVPBCoreElement,
                                                         "pbc:pbcoreIdentifier[pbc:identifierSource=\"id\"]/pbc:identifier");

        if (oldRitzauPIDNode != null && !oldRitzauPIDNode.getTextContent().isEmpty()) {
            result.add(oldRitzauPIDNode.getTextContent());
        }

        Node oldGallupPIDNode = xPathSelector.selectNode(radioTVPBCoreElement,
                                                         "pbc:pbcoreIdentifier[pbc:identifierSource=\"tvmeter\"]/pbc:identifier");

        if (oldGallupPIDNode != null && !oldGallupPIDNode.getTextContent().isEmpty()) {
            result.add(oldGallupPIDNode.getTextContent());
        }
        return result;
    }

    /**
     * Get the PIDs for all the file URLs.
     *
     * @param radioTVMetadata Metadata XML document containing the file information.
     * @return A <code>List</code> of PIDs of the radio-tv file objects found in DOMS. If no pids is found, an empty list is returned
     * @throws MalformedURLException if a file element contains an invalid URL.
     */
    private List<String> getFilePids(Document radioTVMetadata) throws MalformedURLException, BackendInvalidCredsException, BackendMethodFailedException {
        // Get the recording files XML element and process the file information.
        NodeList recordingFileURLs = xPathSelector.selectNodeList(radioTVMetadata, "//program/fileUrls/fileUrl");

        // Find the pids for all referenced file urls.
        List<String> fileObjectPIDs = new ArrayList<String>();
        for (int nodeIndex = 0; nodeIndex < recordingFileURLs.getLength(); nodeIndex++) {
            // Lookup file object.
            Node item = recordingFileURLs.item(nodeIndex);
            String fileUrl = item.getTextContent();
            log.debug("Found file url {} from metadata", fileUrl);

            List<String> pids = domsClient.listObjectsWithThisLabel(fileUrl);
            if (pids != null && !pids.isEmpty()) {
                String fileObjectPID = pids.get(0);
                fileObjectPIDs.add(fileObjectPID);
                log.debug("Found file object pid {} for file url {}", fileObjectPID, fileUrl);
            } else {
                log.warn("Found no file objects for file url {}",fileUrl);
            }
        }
        return fileObjectPIDs;
    }
}
