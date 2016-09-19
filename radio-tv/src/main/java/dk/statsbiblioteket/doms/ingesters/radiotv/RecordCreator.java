package dk.statsbiblioteket.doms.ingesters.radiotv;

import dk.statsbiblioteket.doms.client.DomsWSClient;
import dk.statsbiblioteket.doms.client.exceptions.NoObjectFound;
import dk.statsbiblioteket.doms.client.exceptions.ServerOperationFailed;
import dk.statsbiblioteket.doms.client.exceptions.XMLParseException;
import dk.statsbiblioteket.doms.client.relations.LiteralRelation;
import dk.statsbiblioteket.doms.client.relations.Relation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Code to create programs.
 */
public class RecordCreator {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private DomsWSClient domsClient;
    private boolean overwrite;
    private DocumentBuilder documentBuilder;

    public RecordCreator(DomsWSClient domsClient, boolean overwrite) {
        this.domsClient = domsClient;
        this.overwrite = overwrite;
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Failed to understand xml...",e);
        }
    }

    /**
     * Ingests or updates a program object
     *
     * @param radioTVMetadata Bibliographical metadata about the program.
     * @param filename
     * @return PID of the newly created program object, created by the DOMS.
     *
     * @throws ServerOperationFailed    if creation or manipulation of the program object fails.
     * @throws XMLParseException        if any errors were encountered while processing the
     *                                  <code>radioTVMetadata</code> XML document.
     * @throws MalformedURLException if a file element contains an invalid URL.
     * @throws NoObjectFound         if a URL is referenced, which is not found in DOMS.
     */
    public String ingestProgram(Document radioTVMetadata, String filename) throws NoObjectFound, ServerOperationFailed, MalformedURLException, OverwriteException, XMLParseException {
        // Get pids of referenced files - do this first, to ensure fail-early in case of missing files.
        List<String> filePIDs = getFilePids(radioTVMetadata);
        log.debug("Found pids {} of referenced files",filePIDs);

        // Find or create program object.
        List<String> oldIdentifiers = getOldIdentifiers(radioTVMetadata);
        log.debug("Found these old identifiers {} in the program to ingest",oldIdentifiers);

        String programObjectPID = alreadyExistsInRepo(oldIdentifiers);
        if (programObjectPID != null){
            if (overwrite){
                prepareProgramForOverwrite(programObjectPID, filename, oldIdentifiers);
            } else {
                throw new OverwriteException("Attempted to overwrite pid='"+programObjectPID+"");
            }
        } else {
            log.debug("Old identifiers {} did not find a program object in doms",oldIdentifiers);
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
    }


    private void setFileRelations(String programObjectPID, List<String> filePIDs, String filename) throws ServerOperationFailed, XMLParseException {
        List<Relation> relations = domsClient.listObjectRelations(programObjectPID, Common.HAS_FILE_RELATION_TYPE);
        HashSet<String> existingRels = new HashSet<String>();
        for (Relation relation : relations) {
            log.debug("Found relation {},'{}',{}",programObjectPID,relation.getPredicate(),relation.getSubjectPid());
            if (!filePIDs.contains(relation.getSubjectPid())) {
                log.debug("Removing relation relation {},'{}',{}",programObjectPID,relation.getPredicate(),relation.getSubjectPid());
                domsClient.removeObjectRelation((LiteralRelation) relation,
                                                Util.domsCommenter(filename, "removed relation '" + relation.getPredicate() + "' to '" + relation.getSubjectPid() + "'"));
            } else {
                existingRels.add(relation.getSubjectPid());
            }
        }
        for (String filePID : filePIDs) {
            if (!existingRels.contains(filePID)) {
                log.debug("Adding relation {},'{}',{}",programObjectPID,Common.HAS_FILE_RELATION_TYPE,filePID);
                domsClient.addObjectRelation(programObjectPID, Common.HAS_FILE_RELATION_TYPE, filePID, Util.domsCommenter(filename, "added relation '" + Common.HAS_FILE_RELATION_TYPE + "' to '" + filePID + "'") );

            }
        }
    }

    private void addBroadcast(Document radioTVMetadata, String programObjectPID, String comment) throws ServerOperationFailed {
        // Add the program broadcast datastream
        log.debug("Adding/Updating {} datastream", Common.PROGRAM_BROADCAST_DS_ID);
        Document programBroadcastDocument = createDocumentFromNode(radioTVMetadata, Common.PROGRAM_BROADCAST_ELEMENT);
        domsClient.updateDataStream(programObjectPID, Common.PROGRAM_BROADCAST_DS_ID, programBroadcastDocument,
                                    comment);
    }

    private void addGallup(Document radioTVMetadata, String programObjectPID, String comment) throws ServerOperationFailed {
        // Add the Gallup datastream
        log.debug("Adding/Updating {} datastream", Common.GALLUP_ORIGINAL_DS_ID);
        Document gallupOriginalDocument = createDocumentFromNode(radioTVMetadata, Common.GALLUP_ORIGINALS_ELEMENT);
        domsClient.updateDataStream(programObjectPID, Common.GALLUP_ORIGINAL_DS_ID, gallupOriginalDocument,
                                    comment);
    }

    private void addRitzau(Document radioTVMetadata, String programObjectPID, String comment) throws ServerOperationFailed {
        // Add Ritzau datastream
        log.debug("Adding/Updating {} datastream", Common.RITZAU_ORIGINAL_DS_ID);
        Document ritzauOriginalDocument = createDocumentFromNode(radioTVMetadata, Common.RITZAU_ORIGINALS_ELEMENT);
        domsClient.updateDataStream(programObjectPID, Common.RITZAU_ORIGINAL_DS_ID, ritzauOriginalDocument,
                                    comment);
    }

    private void addPBCore(Document radioTVMetadata, String programObjectPID, String comment) throws ServerOperationFailed {
        // Add PBCore datastream
        log.debug("Adding/Updating {} datastream", Common.PROGRAM_PBCORE_DS_ID);
        Document pbCoreDataStreamDocument = createDocumentFromNode(radioTVMetadata, Common.PBCORE_DESCRIPTION_ELEMENT);
        domsClient.updateDataStream(programObjectPID, Common.PROGRAM_PBCORE_DS_ID, pbCoreDataStreamDocument,
                                    comment);
    }

    private void setTitle(Document radioTVMetadata, String filename, String programObjectPID) throws ServerOperationFailed {
        // Get the program title from the PBCore metadata and use that as the
        // object label for this program object.
        Node titleNode = Common.XPATH_SELECTOR.selectNode(radioTVMetadata, Common.PBCORE_TITLE_ELEMENT);
        String programTitle = titleNode.getTextContent();
        log.debug("Found program title '{}', setting this as label on {}",programTitle,programObjectPID);
        domsClient.setObjectLabel(programObjectPID, programTitle, Util.domsCommenter(filename, "added program title '" +
                                                                                               programTitle +
                                                                                               "' object label"));
    }

    private void prepareProgramForOverwrite(String existingPid, String filename, List<String> oldIdentifiers) throws ServerOperationFailed {
        log.debug("Found existing object {}, to be overwritten", existingPid);
        domsClient.unpublishObjects(Util.domsCommenter(filename, "unpublished object to allow for changes"), existingPid);
        log.debug("Existing object {} unpublished",existingPid);
        addOldPids(existingPid, oldIdentifiers, filename);
        log.debug("Old identifiers added to program object {}",existingPid);
    }

    private String createNewProgramObject(String filename, List<String> oldIdentifiers) throws ServerOperationFailed {
        String programObjectPID;// Create a program object in the DOMS and update the PBCore metadata
        // datastream with the PBCore metadata from the pre-ingest file.
        programObjectPID = domsClient.createObjectFromTemplate(Common.PROGRAM_TEMPLATE_PID, oldIdentifiers, Util.domsCommenter(filename, "creating Program Object"));
        log.debug("Creating new program object with pid {}",programObjectPID);
        return programObjectPID;
    }

    private void addOldPids(String existingPid, List<String> oldIdentifiers, String filename) throws ServerOperationFailed {
        Document dcDataStream = domsClient.getDataStream(existingPid, Common.DC_DS_ID);
        NodeList existingIDNodes = Common.XPATH_SELECTOR.selectNodeList(dcDataStream, Common.DC_IDENTIFIER_ELEMENT);
        Set<String> idsToAdd = new HashSet<String>(oldIdentifiers);
        for (int i = 0; i < existingIDNodes.getLength(); i++) {
            idsToAdd.remove(existingIDNodes.item(i).getTextContent());
        }
        log.debug("Object {} is missing these {} old identifiers",existingPid, idsToAdd);
        if (idsToAdd.isEmpty()) {
            return;
        }

        Node identifier = Common.XPATH_SELECTOR.selectNode(dcDataStream, Common.DC_IDENTIFIER_ELEMENT);
        for (String id : idsToAdd) {
            Element newIdentifier = dcDataStream.createElementNS(Common.DC_NAMESPACE, "identifier");
            dcDataStream.setTextContent(id);
            identifier.getParentNode().insertBefore(newIdentifier, identifier);
            log.debug("Adding {} to dc identifiers for object {}", id, existingPid);
        }
        log.debug("Updating {} datastream with new old identifiers {}",Common.DC_DS_ID,oldIdentifiers);
        domsClient.updateDataStream(existingPid, Common.DC_DS_ID, dcDataStream, Util.domsCommenter(filename, "added old identifiers " + oldIdentifiers));
    }

    /**
     * Utility method to create a document to ingest from a node.
     *
     * @param radioTVMetadata Bibliographical metadata about the program.
     * @param xpath           XPath expression to select and ingest in node.
     * @return A document containing the metadata form the node selected by xpath.
     */
    private Document createDocumentFromNode(Document radioTVMetadata, String xpath) {
        final Node nodeToIngest = Common.XPATH_SELECTOR.selectNode(radioTVMetadata, xpath);

        // Build a data document for the data stream in the program object.
        final Document document = documentBuilder.newDocument();
        document.appendChild(document.importNode(nodeToIngest, true));
        return document;
    }

    /**
     * Lookup a program in DOMS.
     * If program exists, returns the PID of the program. Otherwise returns null.
     *
     * @return PID of program, if found. Null otherwise
     *
     * @throws ServerOperationFailed Could not communicate with DOMS.
     * @param oldIdentifiers List of old identifiers to look up.
     */
    private String alreadyExistsInRepo(List<String> oldIdentifiers) throws ServerOperationFailed {
        for (String oldId : oldIdentifiers) {
            try {
                //TODO Remove this when fixed in doms central RI query
                oldId = oldId.replaceAll("'", Matcher.quoteReplacement("\\'"));
                List<String> pids = domsClient.getPidFromOldIdentifier(oldId);
                if (!pids.isEmpty() && !pids.get(0).isEmpty()) {
                    if (pids.size() > 1){
                        log.warn("Found {} pids for old identifiers {}, returning the first", pids,oldId);
                    }
                    return pids.get(0);
                }
            } catch (NoObjectFound e) {
                // Ignore, then
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
    private List<String> getOldIdentifiers(Document radioTVMetadata)  {
        List<String> result = new ArrayList<String>();

        Node radioTVPBCoreElement = Common.XPATH_SELECTOR.selectNode(radioTVMetadata, Common.PBCORE_DESCRIPTION_ELEMENT);

        Node oldRitzauPIDNode = Common.XPATH_SELECTOR.selectNode(radioTVPBCoreElement, Common.PBCORE_RITZAU_IDENTIFIER_ELEMENT);

        if (oldRitzauPIDNode != null && !oldRitzauPIDNode.getTextContent().isEmpty()) {
            result.add(oldRitzauPIDNode.getTextContent());
        }

        Node oldGallupPIDNode = Common.XPATH_SELECTOR.selectNode(radioTVPBCoreElement, Common.PBCORE_GALLUP_IDENTIFIER_ELEMENT);

        if (oldGallupPIDNode != null && !oldGallupPIDNode.getTextContent().isEmpty()) {
            result.add(oldGallupPIDNode.getTextContent());
        }
        return result;
    }

    /**
     * Get the PIDs for all the file URLs.
     *
     * @param radioTVMetadata Metadata XML document containing the file information.
     * @return A <code>List</code> of PIDs of the radio-tv file objects found in DOMS.
     *
     * @throws MalformedURLException if a file element contains an invalid URL.
     * @throws ServerOperationFailed if looking up file URL failed.
     * @throws NoObjectFound         if a URL is referenced, which is not found in DOMS.
     */
    private List<String> getFilePids(Document radioTVMetadata) throws MalformedURLException, NoObjectFound, ServerOperationFailed {
        // Get the recording files XML element and process the file information.
        NodeList recordingFileURLs = Common.XPATH_SELECTOR.selectNodeList(radioTVMetadata, Common.RECORDING_FILES_URLS);

        // Find the pids for all referenced file urls.
        List<String> fileObjectPIDs = new ArrayList<String>();
        for (int nodeIndex = 0; nodeIndex < recordingFileURLs.getLength(); nodeIndex++) {
            // Lookup file object.
            Node item = recordingFileURLs.item(nodeIndex);
            String itemTextContent = item.getTextContent();
            log.debug("Found file url {} from metadata", itemTextContent);
            URL fileURL = new URL(itemTextContent);
            String fileObjectPID = domsClient.getFileObjectPID(fileURL);
            fileObjectPIDs.add(fileObjectPID);
            log.debug("Found file object pid {} for file url {}",fileObjectPID,fileURL);
        }
        return fileObjectPIDs;
    }
}
