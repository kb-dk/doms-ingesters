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
import dk.statsbiblioteket.doms.client.DomsWSClientImpl;
import dk.statsbiblioteket.doms.client.exceptions.NoObjectFound;
import dk.statsbiblioteket.doms.client.exceptions.ServerOperationFailed;
import dk.statsbiblioteket.doms.client.exceptions.XMLParseException;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/** On added xml files with radio/tv metadata, add objects to DOMS describing these files. */
public class RadioTVMetadataProcessor extends MultiThreadedProcessor implements HotFolderScannerClient {


    /** How many times we failed during ingest. */
    private int exceptionCount = 0;

    /** Folder to move failed files to. */
    private final File failedFilesFolder;
    /** Folder to move processed files to. */
    private final File processedFilesFolder;
    private Schema preIngestFileSchema;
    private final boolean overwrite;


    /** Client for communicating with DOMS. */
    private final DomsWSClient domsClient;



    /**
     * Initialise the processor.
     *
     * @param domsLoginInfo Information used for contacting DOMS.
     * @param failedFilesFolder Folder to move failed files to.
     * @param processedFilesFolder Folder to move processed files to.
     * @param preIngestFileSchema Schema for Raio/TV metadata to process.

     */
    public RadioTVMetadataProcessor(DOMSLoginInfo domsLoginInfo, File failedFilesFolder, File processedFilesFolder,
                                    Schema preIngestFileSchema, boolean overwrite) {
        super(5);
        this.failedFilesFolder = failedFilesFolder;
        this.processedFilesFolder = processedFilesFolder;
        this.preIngestFileSchema = preIngestFileSchema;
        this.overwrite = overwrite;
        this.domsClient = new DomsWSClientImpl();
        this.domsClient.setCredentials(domsLoginInfo.getDomsWSAPIUrl(), domsLoginInfo.getLogin(),
                domsLoginInfo.getPassword());

    }

    private DocumentBuilder getFileParser(Schema preIngestFileSchema) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setSchema(preIngestFileSchema);
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder preingestFilesBuilder;
        try {
            preingestFilesBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
            fatalException();
            throw new RuntimeException();// will never be reached, but no matter
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
     * @param addedFile Full path to the new file.
     */
    @Override
    public synchronized void fileAdded(final File addedFile) {

        Runnable handler = new Runnable() {
            @Override
            public void run() {
                List<String> pidsInProgress = new ArrayList<String>();
                //This method acts as fault barrier
                try {
                    Document radioTVMetadata = getFileParser(preIngestFileSchema).parse(addedFile);
                    createRecord(radioTVMetadata, addedFile, pidsInProgress);
                } catch (Exception e) {
                    // Handle anything unanticipated.
                    failed(addedFile, pidsInProgress);
                    e.printStackTrace();
                    incrementFailedTries();
                }
            }
        };
        pool.submit(handler);
    }

    @Override
    public void fileDeleted(File deletedFile) {
        // Not relevant.
    }

    /**
     * Acts exactly as fileAdded.
     * @param modifiedFile  Full path to the modified file.
     */
    @Override
    public void fileModified(File modifiedFile) {
        fileAdded(modifiedFile);
    }

    /**
     * Create objects in DOMS for given program metadata. On success, the originating file will be moved to the folder
     * for processed files. On failure, a file in the folder of failed files will contain the pids that were not
     * published.
     *
     * @param radioTVMetadata The Metadata for the program.
     * @param addedFile The file containing the program metadata
     * @param pidsInProgress Initially empty list of pids to update with pids collected during process, to be published
     * or reported as failed in the end.
     *
     * @throws IOException On io trouble communicating.
     * @throws ServerOperationFailed On trouble updating DOMS.
     * @throws URISyntaxException Should never happen. Means shard URI generated is invalid.
     * @throws XPathExpressionException Should never happen. Means program is broken with wrong XPath exception.
     * @throws XMLParseException On trouble parsing XML.
     */
    private void createRecord(Document radioTVMetadata, File addedFile, List<String> pidsInProgress)
            throws IOException, ServerOperationFailed, URISyntaxException, XPathExpressionException, XMLParseException, JAXBException, ParseException, ParserConfigurationException, NoObjectFound {
        // Create or update program object for this program
        String programPID = new RecordCreator(domsClient, overwrite).ingestProgram(radioTVMetadata);
        pidsInProgress.add(programPID);
        File allWrittenPIDs = writePIDs(failedFilesFolder, addedFile, pidsInProgress);

        // Publish the objects created in the process
        domsClient.publishObjects(Common.COMMENT, pidsInProgress.toArray(new String[pidsInProgress.size()]));

        // The ingest was successful, if we make it here...
        // Move the processed file to the finished files folder.
        moveFile(addedFile, processedFilesFolder);

        // And it is now safe to delete the "in progress" PID file.
        allWrittenPIDs.delete();

    }

    /**
     * Move <code>fileToMove</code> to the folder specified by
     * <code>destinationFolder</code>.
     *
     * @param fileToMove        Path to the file to move to <code>destinationFolder</code>.
     * @param destinationFolder Path of the destination folder to move the file to.
     */
    private void moveFile(File fileToMove, File destinationFolder) {
        fileToMove.renameTo(new File(destinationFolder.getAbsolutePath(), fileToMove.getName()));
    }




    /**
     * Iteratively (over)write the pid the <code>InProcessPIDs</code> file
     * associated with the <code>preIngestFile</code>.
     *
     * @param outputDirectory absolute path to the directory where the PID file must be
     *                        written.
     * @param preIngestFile   The file containing the Metadata for a program.
     * @param PIDs            A list of PIDs to write.
     * @return The <code>File</code> which the PIDs were written to.
     *
     * @throws IOException thrown if the file cannot be written to.
     */
    private File writePIDs(File outputDirectory, File preIngestFile, List<String> PIDs) throws IOException {
        final File pidFile = new File(outputDirectory, preIngestFile.getName() + ".InProcessPIDs");

        if (!pidFile.exists()) {
            pidFile.createNewFile();
        }

        if (pidFile.canWrite()) {
            final PrintWriter writer = new PrintWriter(pidFile);
            for (String currentPID : PIDs) {
                writer.println(currentPID);
            }
            writer.flush();
            writer.close();
            return pidFile;
        } else {
            throw new IOException("Cannot write file " + pidFile.getAbsolutePath());
        }
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
     */
    private void failed(File addedFile, List<String> pidsToPublish) {
        try {
            moveFile(addedFile, failedFilesFolder);

            // Rename the in-progress PIDs to failed PIDs.
            writeFailedPIDs(addedFile);
            domsClient.deleteObjects(Common.FAILED_COMMENT, pidsToPublish.toArray(new String[pidsToPublish.size()]));
        } catch (Exception exception) {
            // If this bail-out error handling fails, then nothing can save
            // us...
            exception.printStackTrace(System.err);
            throw new Error("Unrecoverable error during ingesting", exception);
        }
    }


    /**
     * Rename the file with a list of PIDs in progress and never published to a file with a list of failed PIDs.
     * @param failedMetadataFile The originating file.
     *
     */
    private synchronized void writeFailedPIDs(File failedMetadataFile) {
        final File activePIDsFile = new File(failedFilesFolder, failedMetadataFile.getName() + ".InProcessPIDs");
        final File failedPIDsFile = new File(failedFilesFolder, failedMetadataFile.getName() + ".failedPIDs");
        activePIDsFile.renameTo(failedPIDsFile);
    }

    /**
     * ends all attempts to ingest from the current list of file descriptions in
     * the pre-ingest file Violent exit needed "system.exit()
     */
    private void fatalException() {
        System.exit(-1);
    }

    /** The number of tries is incremented by one.
     * If this exceeds the maximum number of allowed failures,  */
    private synchronized void incrementFailedTries() {
        exceptionCount += 1;
        if (exceptionCount >= Common.MAX_FAIL_COUNT) {
            System.err.println("Too many errors (" + exceptionCount + ") in ingest. Exiting.");
            fatalException();
        }
    }
}
