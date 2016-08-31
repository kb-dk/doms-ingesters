/*
 * $Id: TestHotFolderScanner.java 1442 2011-01-10 13:08:18Z thomassh $
 * $Revision: 1442 $
 * $Date: 2011-01-10 14:08:18 +0100 (Mon, 10 Jan 2011) $
 * $Author: thomassh $
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author tsh
 */
public class TestHotFolderScanner {

    private HotFolderScanner folderScanner;
    private Path clientFeedbackAddedFile;

    private Path tempTestDir;
    private Path tempTestFile;
    private Path stopFolder;

    // TODO: Add tests for the detection of modified and deleted files.
    @SuppressWarnings("unused")
    private Path clientFeedbackModifiedFile;
    @SuppressWarnings("unused")
    private Path clientFeedbackDeletedFile;

    // TODO: Also test file modification and deletion.

    private final FolderWatcherClient folderScannerClient = new FolderWatcherClient() {

        @Override
        public void fileAdded(Path addedFile) throws IOException {
            System.out.println("File " + addedFile + " was added");
            clientFeedbackAddedFile = addedFile;
            Files.deleteIfExists(clientFeedbackAddedFile);
        }

        @Override
        public void fileDeleted(Path deletedFile) throws IOException {
            System.out.println("File " + deletedFile + " was deleted");
            clientFeedbackDeletedFile = deletedFile;
            Files.deleteIfExists(clientFeedbackAddedFile);
        }

        @Override
        public void fileModified(Path modifiedFile) throws IOException {
            System.out.println("File " + modifiedFile + " was modified");
            clientFeedbackModifiedFile = modifiedFile;
            Files.deleteIfExists(clientFeedbackAddedFile);
        }
    };

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        folderScanner = new HotFolderScanner();
        clientFeedbackAddedFile = null;
    }

    @After
    public void tearDown() throws Exception {
        if (tempTestFile != null) {
            Files.deleteIfExists(tempTestFile);
        }

        if (tempTestDir != null) {
            Files.deleteIfExists(tempTestDir);
        }
        if (stopFolder != null) {
            Files.deleteIfExists(stopFolder);
        }
    }

    @Test
    public void testStartScanning() throws IOException, InterruptedException {
        tempTestDir = Files.createTempDirectory(null);
        stopFolder = Files.createTempDirectory(null);

        // Create a test file. It must be an XML file as the folder scanner
        // filters out XML files.
        Path tempTestFile2 = tempTestDir.resolve(UUID.randomUUID().toString() + ".xml");
        Files.createFile(tempTestFile2);

        //TODO mock folderScannerClient
        folderScanner.startScanning(tempTestDir, stopFolder,
                                    folderScannerClient);

        System.out.println("The file "+tempTestFile2+" should be found first");

        // Create a test file. It must be an XML file as the folder scanner
        // filters out XML files.
        tempTestFile = tempTestDir.resolve(UUID.randomUUID().toString() + ".xml");
        Files.createFile(tempTestFile);

        // Wait for the scanner to detect the change.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ir) {
            // Never mind that....
        }
        folderScanner.setStopFlagSet(true);
        folderScanner.waitForStop();

        assertEquals(
                "The created test file was not detected by the hot folder scanner.",
                tempTestFile, clientFeedbackAddedFile);

        // TODO: It wouldn't hurt testing the scanner with more than one file...
    }
}
