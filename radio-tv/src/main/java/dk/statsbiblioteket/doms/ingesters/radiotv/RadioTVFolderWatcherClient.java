/*
 * $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The DOMS project.
 * Copyright (C) 2007-2010  The State and University Library
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package dk.statsbiblioteket.doms.ingesters.radiotv;


import dk.statsbiblioteket.doms.client.DomsWSClient;
import dk.statsbiblioteket.doms.client.exceptions.NoObjectFound;
import dk.statsbiblioteket.doms.client.exceptions.ServerOperationFailed;
import dk.statsbiblioteket.doms.client.exceptions.XMLParseException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * On added xml files with radio/tv metadata, add objects to DOMS describing these files.
 */
public class RadioTVFolderWatcherClient extends FolderWatcherClient {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final DocumentBuilderFactory documentBuilderFactory;


    /**
     * How many times we failed during ingest.
     */
    private int exceptionCount = 0;

    /**
     * Folder to move failed files to.
     */
    private final Path failedFilesFolder;
    /**
     * Folder to move processed files to.
     */
    private final Path processedFilesFolder;
    private final boolean overwrite;


    /**
     * Client for communicating with DOMS.
     */
    private final DomsWSClient domsClient;


    /**
     * Initialise the processor.
     *
     * @param domsClient           Client used for contacting DOMS.
     * @param failedFilesFolder    Folder to move failed files to.
     * @param processedFilesFolder Folder to move processed files to.
     * @param preIngestFileSchema  Schema for Raio/TV metadata to process.
     */
    public RadioTVFolderWatcherClient(DomsWSClient domsClient, Path failedFilesFolder, Path processedFilesFolder,
                                      Schema preIngestFileSchema, boolean overwrite) {
        this.domsClient = domsClient;
        log.debug(
                "Creating {} with params domsClient={}, failedFilesFolder={}, processedFilesFolder={}, preIngestSchema={}, overwrite={}",
                getClass().getName(),
                domsClient,
                failedFilesFolder,
                processedFilesFolder,
                preIngestFileSchema, overwrite);
        this.failedFilesFolder = failedFilesFolder;
        this.processedFilesFolder = processedFilesFolder;
        this.overwrite = overwrite;

        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setSchema(preIngestFileSchema);
        documentBuilderFactory.setNamespaceAware(true);
    }

    private DocumentBuilder getFileParser() {
        DocumentBuilder preingestFilesBuilder;
        try {
            preingestFilesBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            throw new RuntimeException(pce);// will never be reached, but no matter
        }

        ErrorHandler documentErrorHandler = new ErrorHandler() {

            @Override
            public void warning(SAXParseException exception) throws SAXException {
                throw exception;
            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
                throw exception;
            }

            @Override
            public void error(SAXParseException exception) throws SAXException {
                throw exception;
            }
        };
        preingestFilesBuilder.setErrorHandler(documentErrorHandler);
        return preingestFilesBuilder;
    }

    /**
     * Will parse the metadata and add relevant objects to DOMS.
     *
     * @param addedFile Full path to the new file.
     */
    @Override
    public synchronized void fileAdded(Path addedFile) {
        if (isXmlFile(addedFile)) {
            log.debug("File {} added, start processing", addedFile);
            handleAddedOrModifiedFile(addedFile);
        }
    }

    private boolean isXmlFile(Path file) {
        return file != null && Files.isRegularFile(file) && file.getFileName().toString().endsWith(".xml");
    }

    /**
     * Acts exactly as fileAdded.
     *
     * @param modifiedFile Full path to the modified file.
     */
    @Override
    public void fileModified(Path modifiedFile) {
        if (isXmlFile(modifiedFile)) {
            log.debug("File {} modified, start processing", modifiedFile);
            handleAddedOrModifiedFile(modifiedFile);
        }
    }

    private void handleAddedOrModifiedFile(Path addedFile) {
        if (!isAlreadyHandled(addedFile)) {
            handleFile(addedFile);
        }
    }

    private boolean isAlreadyHandled(Path addedFile) {
        try {

            Path possibleCopy = processedFilesFolder.resolve(addedFile.getFileName());
            if (Files.isRegularFile(possibleCopy)) {
                log.debug("Found possible copy of file {} in {}", addedFile,
                          processedFilesFolder);
                long originalSum = FileUtils.checksumCRC32(addedFile.toFile());
                long copySum = FileUtils.checksumCRC32(possibleCopy.toFile());
                if (originalSum == copySum) {
                    log.info("Found exact duplicate of file={} in processedFolder={}, so deleting file={}",
                             addedFile,
                             processedFilesFolder,
                             addedFile);
                    Files.deleteIfExists(addedFile);
                    return true;
                }
            }
        } catch (IOException e) {
            log.warn("IOException while trying to find duplicate of file={} in processedFilesFolder={}",
                     addedFile, processedFilesFolder, e);
            incrementFailedTries();
        }
        return false;
    }

    private void handleFile(Path addedFile) {
        List<String> pidsInProgress = new ArrayList<>();

        try (AutoCloseable ignored = namedThread(addedFile.getFileName().toString())) { //Trick to rename the thread and name it back
            log.debug("Creating xml parser");
            DocumentBuilder fileParser = getFileParser();

            log.debug("Parsing xml file");
            Document radioTVMetadata = fileParser.parse(addedFile.toFile());

            log.debug("Creating doms record");
            createRecord(radioTVMetadata, addedFile, pidsInProgress);

            log.debug("Ingest complete");
        } catch (Exception e) {
            // Handle anything unanticipated.
            failed(addedFile, pidsInProgress, e);
        }
    }

    private Closeable namedThread(String name) {
        String oldName = Thread.currentThread().getName();
        Thread.currentThread().setName(name);
        return () -> Thread.currentThread().setName(oldName);
    }

    @Override
    public void fileDeleted(Path deletedFile) {
        log.debug("File was deleted and I do not care");
        // Not relevant.
    }

    /**
     * Create objects in DOMS for given program metadata. On success, the originating file will be moved to the folder
     * for processed files. On failure, a file in the folder of failed files will contain the pids that were not
     * published.
     *
     * @param radioTVMetadata The Metadata for the program.
     * @param addedFile       The file containing the program metadata
     * @param pidsInProgress  Initially empty list of pids to update with pids collected during process, to be published
     *                        or reported as failed in the end.
     * @throws IOException              On io trouble communicating.
     * @throws ServerOperationFailed    On trouble updating DOMS.
     * @throws XMLParseException        On trouble parsing XML.
     * @throws NoObjectFound            if a URL is referenced, which is not found in DOMS.
     */
    private void createRecord(Document radioTVMetadata,
                              Path addedFile,
                              List<String> pidsInProgress) throws IOException, ServerOperationFailed, XMLParseException, NoObjectFound {
        // Create or update program object for this program
        log.debug("Starting to create doms record");

        log.trace("Creating record creator");
        RecordCreator recordCreator = new RecordCreator(domsClient, overwrite);
        log.trace("Ingesting program");
        String programPID = recordCreator.ingestProgram(radioTVMetadata);
        log.trace("Program ingested with pid={}", programPID);

        pidsInProgress.add(programPID);
        Path allWrittenPIDs = writePIDs(addedFile, pidsInProgress);

        // Publish the objects created in the process
        log.trace("Publishing objects {}", pidsInProgress);

        domsClient.publishObjects(Common.COMMENT, pidsInProgress.toArray(new String[pidsInProgress.size()]));

        log.trace("Ingest was successful, so move file {} to the processedFilesFolder={}", addedFile, processedFilesFolder);
        // The ingest was successful, if we make it here...
        // Move the processed file to the finished files folder.
        Files.move(addedFile, processedFilesFolder);

        // And it is now safe to delete the "in progress" PID file.
        Files.deleteIfExists(allWrittenPIDs);
    }

    /**
     * Iteratively (over)write the pid the <code>InProcessPIDs</code> file
     * associated with the <code>preIngestFile</code>.
     *
     * @param preIngestFile   The file containing the Metadata for a program.
     * @param PIDs            A list of PIDs to write.
     * @return The <code>File</code> which the PIDs were written to.
     * @throws IOException thrown if the file cannot be written to.
     */
    private Path writePIDs(Path preIngestFile, List<String> PIDs) throws IOException {
        Path pidFile = inProcessPids(preIngestFile);

        try (final PrintWriter writer = new PrintWriter(pidFile.toFile());) {
            for (String currentPID : PIDs) {
                writer.println(currentPID);
            }
        }
        return pidFile;
    }

    /**
     * Move the failed pre-ingest file to the "failedFilesFolder" folder and
     * rename the in-progress PID list file to a failed PID list file, also
     * stored in the "failedFilesFolder" folder.
     * <p/>
     * This method should be called before "pulling the plug".
     *
     * @param addedFile     The file attempted to have added to the doms
     * @param pidsToPublish The failed PIDs
     * @param exception the exception that mark the failure
     */
    private void failed(Path addedFile, List<String> pidsToPublish, Exception exception) {
        try {
            log.error("Ingest failed with exception, attempting cleanup for file={} and pids={}", addedFile,
                      pidsToPublish, exception);

            Files.move(addedFile,failedFilesFolder);
            log.trace("Moved file {} to failedFilesFolder={}", addedFile, failedFilesFolder);

            // Rename the in-progress PIDs to failed PIDs.
            renamePidLists(addedFile);

            log.trace("Attempting to delete objects {} from doms", pidsToPublish);
            domsClient.deleteObjects(Common.FAILED_COMMENT, pidsToPublish.toArray(new String[pidsToPublish.size()]));

            log.error("Cleanup succeeded for file={} and pids={}", addedFile, pidsToPublish);

            incrementFailedTries();
        } catch (Exception exception2) {
            // If this bail-out error handling fails, then nothing can save
            // us...
            log.error("Unrecoverable error during ingesting", exception2);
            throw new Error("Unrecoverable error during ingesting", exception2);
        }
    }


    /**
     * Rename the file with a list of PIDs in progress and never published to a file with a list of failed PIDs.
     *
     * @param failedMetadataFile The originating file.
     */
    private synchronized void renamePidLists(Path failedMetadataFile) throws IOException {
        Path activePIDsFile = inProcessPids(failedMetadataFile);
        Path failedPIDsFile = failedPids(failedMetadataFile);
        Files.move(activePIDsFile,failedPIDsFile);
    }

    private Path failedPids(Path failedMetadataFile) {
        return failedFilesFolder.resolve(failedMetadataFile.getFileName().toString() + ".failedPIDs");
    }

    private Path inProcessPids(Path failedMetadataFile) {
        return failedFilesFolder.resolve(
                    failedMetadataFile.getFileName().toString() + ".InProcessPIDs");
    }


    /**
     * The number of tries is incremented by one.
     * If this exceeds the maximum number of allowed failures,
     */
    private synchronized void incrementFailedTries() {
        exceptionCount += 1;
        if (exceptionCount >= Common.MAX_FAIL_COUNT) {
            log.error("Too many errors (" + exceptionCount + ") in ingest. Exiting.");
            throw new Error("TODO"); //TODO stop better
        }
    }

    @Override
    public String toString() {
        return "RadioTVFolderWatcherClient{" +
               "exceptionCount=" + exceptionCount +
               ", failedFilesFolder=" + failedFilesFolder +
               ", processedFilesFolder=" + processedFilesFolder +
               ", overwrite=" + overwrite +
               '}';
    }
}