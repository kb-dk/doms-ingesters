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


import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.EnhancedFedora;
import dk.statsbiblioteket.doms.folderwatching.FolderWatcherClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * On added xml files with radio/tv metadata, add objects to DOMS describing these files.
 */
public class RadioTVFolderWatcherClient extends FolderWatcherClient {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final DocumentBuilderFactory documentBuilderFactory;
    /**
     * Folder to move failed files to.
     */
    private final Path failedFilesFolder;
    /**
     * Folder to move processed files to.
     */
    private final Path processedFilesFolder;
    /**
     * If true, will overwrite objects in doms
     */
    private final boolean overwrite;
    /**
     * If true will report ingest successful if object in doms is semantically identical to the one we want to ingest.
     * If neiter overwrite or check is true, the ingester will fail if the object is already in doms.
     */
    private boolean check;

    /**
     * Client for communicating with DOMS.
     */
    private final EnhancedFedora domsClient;
    /**
     * Max number of exceptions before we shut down the watcher
     */
    private final int maxFails;
    /**
     * How many times we failed during ingest.
     */
    private int exceptionCount = 0;



    /**
     * Initialise the processor.
     *  @param domsClient           Client used for contacting DOMS.
     * @param failedFilesFolder    Folder to move failed files to.
     * @param processedFilesFolder Folder to move processed files to.
     * @param preIngestFileSchema  Schema for Raio/TV metadata to process.
     * @param overwrite            if true, will overwrite existing programs. If false, will throw OverwriteExceptions instead
     * @param maxFails
     */
    public RadioTVFolderWatcherClient(EnhancedFedora domsClient, Path failedFilesFolder, Path processedFilesFolder,
                                      Schema preIngestFileSchema, boolean overwrite, int maxFails, boolean check) {
        this.domsClient = domsClient;
        this.maxFails = maxFails;
        this.check = check;
        log.debug("Creating {} with params domsClient, failedFilesFolder={}, processedFilesFolder={}, overwrite={}, maxFails={}, check={}",
                  getClass().getName(), failedFilesFolder, processedFilesFolder, overwrite, maxFails, check);
        this.failedFilesFolder = failedFilesFolder;
        this.processedFilesFolder = processedFilesFolder;
        this.overwrite = overwrite;

        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setSchema(preIngestFileSchema);
        documentBuilderFactory.setNamespaceAware(true);
    }

    /**
     * Create the xml file parser
     *
     * @return a document builder
     */
    private synchronized DocumentBuilder getFileParser() {
        DocumentBuilder preingestFilesBuilder;
        try {
            preingestFilesBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            throw new RuntimeException(pce);// will never be reached, but no matter
        }

        ErrorHandler documentErrorHandler = new ErrorHandler() { //Any errors are rethrown for max breakage

            @Override
            public void warning(SAXParseException exception) throws SAXException {
                throw exception;
            }

            @Override
            public void error(SAXParseException exception) throws SAXException {
                throw exception;
            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
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
            handleAddedOrModifiedFile(addedFile);
        }
    }

    /**
     * Acts exactly as fileAdded.
     *
     * @param modifiedFile Full path to the modified file.
     */
    @Override
    public void fileModified(Path modifiedFile) {
        if (isXmlFile(modifiedFile)) {
            handleAddedOrModifiedFile(modifiedFile);
        }
    }

    @Override
    public void fileDeleted(Path deletedFile) {
        //Note, this will be invoked with this client moves the files out of the hotFolder....
    }

    /**
     * Checks if the path is a regular file with the extension .xml
     *
     * @param file the file to check
     * @return true if an xml file
     */
    private boolean isXmlFile(Path file) {
        return file != null && Files.isRegularFile(file) && file.getFileName().toString().endsWith(".xml");
    }

    /**
     * Handle a file that is added or modified, as this is the same for this client
     *
     * @param file the file to handle
     */
    private void handleAddedOrModifiedFile(Path file) {
        if (!isAlreadyHandled(file)) {
            handleFile(file);
        }
    }

    /**
     * Checks if the file is already handled, by looking in the processedFilesFolder
     *
     * @param file the file to examine
     * @return true if we already handled the file
     */
    protected boolean isAlreadyHandled(Path file) {
        try {
            Path possibleCopy = processedFilesFolder.resolve(file.getFileName());
            if (Files.isRegularFile(possibleCopy)) {
                log.debug("Found possible copy of file {} in {}", file, processedFilesFolder);


                Source control = Input.fromFile(file.toFile()).build();
                Source test = Input.fromFile(possibleCopy.toFile()).build();

                Diff d = Util.xmlDiff(control, test);

                if (!d.hasDifferences()) {
                    log.info("Found exact duplicate of file={} in processedFolder={}, so deleting file={}",
                             file, processedFilesFolder, file);
                    Files.deleteIfExists(file);
                    return true;
                } else {
                    log.debug("File={} has differences from file {} in processedFolder={}, so continuing ingest: differences='{}' ",file,file,processedFilesFolder,d.toString());
                }
            }
        } catch (IOException e) {
            log.warn("IOException while trying to find duplicate of file={} in processedFilesFolder={}",
                     file, processedFilesFolder, e);
            incrementFailedTries();
            //Note, we return false in this case and tries to ingest the file
        }
        return false;
    }

    /**
     * Handles the file, by ingesting it in DOMS as a Program object
     *
     * @param file the xml file to ingest
     */
    private void handleFile(Path file) {
        List<String> pidsInProgress = new ArrayList<>();

        try { //Trick to rename the thread and name it back
            log.debug("Creating xml parser");
            DocumentBuilder fileParser = getFileParser();

            log.debug("Parsing xml file");
            Document radioTVMetadata = fileParser.parse(file.toFile());

            log.debug("Creating doms record");
            Path tempFile = createRecord(radioTVMetadata, file, pidsInProgress);

            moveToProcessed(file, tempFile);

            log.debug("Ingest complete");
        } catch (Exception e) {
            // Handle anything unanticipated.
            failed(file, pidsInProgress, e);
        }
    }

    private void moveToProcessed(Path ingested_file, Path tempFile) throws IOException {
        log.trace("Ingest was successful, so move file {} to the processedFilesFolder={}", ingested_file,
                  processedFilesFolder);
        // The ingest was successful, if we make it here...
        // Move the processed file to the finished files folder.
        Files.move(ingested_file, processedFilesFolder.resolve(ingested_file.getFileName()), StandardCopyOption.REPLACE_EXISTING);

        // And it is now safe to delete the "in progress" PID file.
        Files.deleteIfExists(tempFile);

        try {
            // If the file also exists in failedFilesFolder, and that file is identical to the one we just ingested, delete it
            Path failedFile = failedFilesFolder.resolve(ingested_file.getFileName());
            if (Files.exists(failedFile)){
                log.info("File {} also found in failedFolder",ingested_file,failedFilesFolder);
                Source control = Input.fromFile(ingested_file.toFile()).build();
                Source test = Input.fromFile(failedFile.toFile()).build();

                Diff d = Util.xmlDiff(control, test);

                if (!d.hasDifferences()) {
                    log.info("Successfully ingested file {} is identical to file {} in failedFolder={}, so deleting {}",
                             ingested_file, failedFilesFolder, failedFile);
                    Files.deleteIfExists(failedFile);
                } else {
                    log.info("Successfully ingested file {} is different from file {} in failedFolder={}. Just Saying: differences='{}' ",
                              ingested_file,failedFile,failedFilesFolder,d.toString());
                }
            }
        } catch (IOException e) {
            log.warn("Failed to check for semantic equivalent file in {} after successful ingest of {}",failedFilesFolder,ingested_file,e);
        }

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
     * @throws IOException           On io trouble communicating.
     */
    private Path createRecord(Document radioTVMetadata,
                              Path addedFile,
                              List<String> pidsInProgress) throws OperationFailed, IOException {
        String filename = addedFile.getFileName().toString();
        // Create or update program object for this program
        log.debug("Starting to create doms record for file");

        log.trace("Creating record creator");
        RecordCreator recordCreator = new RecordCreator(domsClient, overwrite, check);

        log.trace("Ingesting program");
        String programPID = recordCreator.ingestProgram(radioTVMetadata, filename);
        log.trace("Program ingested with pid={}", programPID);

        pidsInProgress.add(programPID);
        Path allWrittenPIDs = writePIDs(addedFile, pidsInProgress);

        // Publish the objects created in the process
        log.trace("Publishing objects {}", pidsInProgress);

        for (String inProgress : pidsInProgress) {
            try {
                domsClient.modifyObjectState(inProgress,EnhancedFedora.STATE_ACTIVE,"Publishing objects " + pidsInProgress + " as part of ingest of program " + addedFile.getFileName());
            } catch (BackendInvalidCredsException | BackendMethodFailedException | BackendInvalidResourceException e) {
                throw new OperationFailed(e);
            }
        }

        return allWrittenPIDs;
    }

    /**
     * Iteratively (over)write the pid the <code>InProcessPIDs</code> file
     * associated with the <code>preIngestFile</code>.
     *
     * @param preIngestFile The file containing the Metadata for a program.
     * @param PIDs          A list of PIDs to write.
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
     * @param exception     the exception that mark the failure
     */
    private void failed(Path addedFile, List<String> pidsToPublish, Exception exception) {
        try {
            String filename = addedFile.getFileName().toString();

            log.error("Ingest failed with exception, attempting cleanup for file={} and pids={}", addedFile,
                      pidsToPublish, exception);

            Files.move(addedFile, failedFilesFolder.resolve(addedFile.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            log.trace("Moved file {} to failedFilesFolder={}", addedFile, failedFilesFolder);

            // Rename the in-progress PIDs to failed PIDs.
            renamePidLists(addedFile);

            log.trace("Attempting to delete objects {} from doms", pidsToPublish);
            String deleteComment = Util.domsCommenter(filename, " deleted objects {0} due to ingest failure",
                                                      pidsToPublish);
            for (String toPublish : pidsToPublish) {
                domsClient.modifyObjectState(toPublish,EnhancedFedora.STATE_DELETED,deleteComment);
            }

            log.error("Cleanup succeeded for file={} and pids={}", addedFile, pidsToPublish);

            incrementFailedTries();
        } catch (Exception exception2) {
            // If this bail-out error handling fails, then nothing can save us...
            log.error("Unrecoverable error during ingesting", exception2);
            throw new RuntimeException("Unrecoverable error during ingesting", exception2);
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
        if (Files.exists(activePIDsFile)) {
            Files.move(activePIDsFile, failedPIDsFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path failedPids(Path failedMetadataFile) {
        return failedFilesFolder.resolve(failedMetadataFile.getFileName().toString() + ".failedPIDs");
    }

    private Path inProcessPids(Path failedMetadataFile) {
        return failedFilesFolder.resolve(failedMetadataFile.getFileName().toString() + ".InProcessPIDs");
    }


    /**
     * The number of tries is incremented by one.
     * If this exceeds the maximum number of allowed failures,
     */
    private synchronized void incrementFailedTries() {
        exceptionCount += 1;
        if (maxFails > 0 && exceptionCount >= maxFails) {
            throw new RuntimeException("Too many errors (" + exceptionCount + ") in ingest. Exiting.");
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
