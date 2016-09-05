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

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import org.mockito.InOrder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author tsh
 */
public class TestFolderScanner {

    private Path tempTestDir;
    private Path stopFolder;




    @After
    public void tearDown() throws Exception {
        if (tempTestDir != null) {
            FileUtils.deleteDirectory(tempTestDir.toFile());
        }
        if (stopFolder != null) {
            FileUtils.deleteDirectory(stopFolder.toFile());
        }
    }

    @Test
    public void testStartScanning() throws Exception {

        tempTestDir = Files.createTempDirectory("hotFolder");
        stopFolder = Files.createTempDirectory("stopFolder");

        // Create a test file. It must be an XML file as the folder scanner
        // filters out XML files.
        Path tempTestFile = createTempFile();

        FolderWatcherClient folderScannerClient = mock(FolderWatcherClient.class);

        FolderScanner folderScanner = new FolderScanner(tempTestDir, stopFolder,
                                                        folderScannerClient);


        ExecutorService background = Executors.newSingleThreadExecutor();

        background.submit(folderScanner);
        background.shutdown();

        // Wait for the scanner to start up
        Thread.sleep(1000);

        // Create a test file. It must be an XML file as the folder scanner
        // filters out non-XML files.
        Path tempTestFile2 = createTempFile();

        // Wait for the scanner to detect the change.
        Thread.sleep(1000);

        //Stop the scanner
        folderScanner.setStopFlagSet(true);
        assertTrue(background.awaitTermination(10, TimeUnit.SECONDS));

        InOrder order = inOrder(folderScannerClient);
        order.verify(folderScannerClient).fileAdded(tempTestFile);
        order.verify(folderScannerClient).fileAdded(tempTestFile2);
        order.verify(folderScannerClient).close();
        order.verifyNoMoreInteractions();
    }

    private Path createTempFile() throws IOException {
        Path tempTestFile = tempTestDir.resolve(UUID.randomUUID().toString() + ".xml");
        Files.createFile(tempTestFile);
        return tempTestFile;
    }
}
