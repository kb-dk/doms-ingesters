/*
 * $Id: TestHotFolderWatcher.java 1442 2011-01-10 13:08:18Z thomassh $
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
package dk.statsbiblioteket.doms.folderwatching;

import dk.statsbiblioteket.doms.folderwatching.FolderWatcher;
import dk.statsbiblioteket.doms.folderwatching.FolderWatcherClient;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import org.mockito.InOrder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author tsh
 */
public class TestFolderWatcher {

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

        FolderWatcherClient FolderWatcherClient = mock(FolderWatcherClient.class);

        FolderWatcher FolderWatcher = new FolderWatcher(tempTestDir, 1000, FolderWatcherClient, 1, stopFolder);


        ExecutorService background = Executors.newSingleThreadExecutor();

        background.submit(FolderWatcher);
        background.shutdown();

        // Wait for the scanner to start up
        Thread.sleep(1000);

        //Stop the scanner
        FolderWatcher.setClosed(true);
        assertTrue(background.awaitTermination(10, TimeUnit.SECONDS));

        InOrder order = inOrder(FolderWatcherClient);
        order.verify(FolderWatcherClient).fileAdded(tempTestFile);
        order.verify(FolderWatcherClient).close();
        order.verifyNoMoreInteractions();
    }


    @Test
    public void testFull() throws Exception {

        tempTestDir = Files.createTempDirectory("hotFolder");
        stopFolder = Files.createTempDirectory("stopFolder");

        // Create a test file. It must be an XML file as the folder scanner
        // filters out XML files.
        Path tempTestFile = createTempFile();

        FolderWatcherClient FolderWatcherClient = mock(FolderWatcherClient.class);

        FolderWatcher FolderWatcher = new FolderWatcher(tempTestDir, 1000, FolderWatcherClient, 1, stopFolder);


        ExecutorService background = Executors.newSingleThreadExecutor();

        background.submit(FolderWatcher);
        background.shutdown();

        // Wait for the scanner to start up
        Thread.sleep(1000);

        InOrder order = inOrder(FolderWatcherClient);
        order.verify(FolderWatcherClient).fileAdded(tempTestFile);



        // Create a test file. It must be an XML file as the folder scanner
        // filters out non-XML files.
        Path tempTestFile2 = createTempFile();

        // Wait for the scanner to detect the change.
        Thread.sleep(1000);

        //Stop the scanner
        FolderWatcher.setClosed(true);
        assertTrue(background.awaitTermination(10, TimeUnit.SECONDS));

        order.verify(FolderWatcherClient).fileAdded(tempTestFile2);
        order.verify(FolderWatcherClient).close();
        order.verifyNoMoreInteractions();
    }


    @Test
    public void testFileAdded() throws Exception {

        tempTestDir = Files.createTempDirectory("hotFolder");
        stopFolder = Files.createTempDirectory("stopFolder");


        FolderWatcherClient FolderWatcherClient = new FolderWatcherClient() {
            @Override
            public void fileAdded(Path addedFile) throws Exception {
                super.fileAdded(addedFile);
                Files.deleteIfExists(addedFile);
            }
        };

        FolderWatcher FolderWatcher = new FolderWatcher(tempTestDir, 1000, FolderWatcherClient, 1, stopFolder);


        ExecutorService background = Executors.newSingleThreadExecutor();

        background.submit(FolderWatcher);
        background.shutdown();

        // Wait for the scanner to start up
        Thread.sleep(1000);

        // Create a test file. It must be an XML file as the folder scanner
        // filters out non-XML files.
        Path tempTestFile2 = createTempFile();
        assertTrue(Files.exists(tempTestFile2));

        // Wait for the scanner to detect the change.
        Thread.sleep(1000);

        //Stop the scanner
        FolderWatcher.setClosed(true);
        assertTrue(background.awaitTermination(10, TimeUnit.SECONDS));

        assertFalse(Files.exists(tempTestFile2));

    }


    @Test
    public void testMultiThreading() throws Exception {

        tempTestDir = Files.createTempDirectory("hotFolder");
        stopFolder = Files.createTempDirectory("stopFolder");

        long handlingTime = 100;

        Set<Long> threadIDs = new HashSet<>();
        List<Path> handlingOrder = new ArrayList<>();

        FolderWatcherClient FolderWatcherClient = new FolderWatcherClient() {
            @Override
            public void fileAdded(Path addedFile) throws Exception {
                super.fileAdded(addedFile);
                handlingOrder.add(addedFile);
                Thread.sleep(handlingTime);
                threadIDs.add(Thread.currentThread().getId());
            }
        };

        int numThreads = 2;
        FolderWatcher FolderWatcher = new FolderWatcher(tempTestDir, 1000, FolderWatcherClient, numThreads, stopFolder);


        ExecutorService background = Executors.newSingleThreadExecutor();

        background.submit(FolderWatcher);
        background.shutdown();

        // Create a test file. It must be an XML file as the folder scanner
        // filters out non-XML files.
        List<Path> creationOrder = new ArrayList<>();
        for (int i = 0; i< 10; i++){
            creationOrder.add(createTempFile(i));
        }

        do {// Wait for the scanner to handle all the files
            System.out.println("Handled files: "+handlingOrder.size());
            Thread.sleep(handlingTime);
        } while (handlingOrder.size() < 10);
        System.out.println("Handled files: "+handlingOrder.size());


        //Stop the scanner
        FolderWatcher.setClosed(true);
        assertTrue(background.awaitTermination(1, TimeUnit.SECONDS));

        creationOrder.removeAll(handlingOrder);
        assertEquals("All created files should have been handled", Arrays.asList(), creationOrder);

        assertEquals(threadIDs.size(), numThreads);

    }


    @Test
    public void testStopFolder() throws Exception {

        tempTestDir = Files.createTempDirectory("hotFolder");
        stopFolder = Files.createTempDirectory("stopFolder");

        long handlingTime = 100;

        List<Path> handlingOrder = new ArrayList<>();

        FolderWatcherClient FolderWatcherClient = new FolderWatcherClient() {
            @Override
            public void fileAdded(Path addedFile) throws Exception {
                super.fileAdded(addedFile);
                handlingOrder.add(addedFile);
                Thread.sleep(handlingTime);
            }
        };

        int numThreads = 1;
        FolderWatcher FolderWatcher = new FolderWatcher(tempTestDir, 1000, FolderWatcherClient, numThreads, stopFolder);


        ExecutorService background = Executors.newSingleThreadExecutor();

        background.submit(FolderWatcher);
        background.shutdown();

        // Create a test file. It must be an XML file as the folder scanner
        // filters out non-XML files.
        List<Path> creationOrder = new ArrayList<>();
        for (int i = 0; i< 2; i++){
            creationOrder.add(createTempFile(i));
        }

        do {// Wait for the scanner to handle all the files
            System.out.println("Handled files: "+handlingOrder.size());
            Thread.sleep(handlingTime);
        } while (handlingOrder.size() < 2);
        System.out.println("Handled files: "+handlingOrder.size());


        //Stop the scanner
        Path stoprunning = stopFolder.resolve("stoprunning");
        Files.createFile(stoprunning);
        assertTrue(background.awaitTermination(1, TimeUnit.SECONDS));

        creationOrder.removeAll(handlingOrder);
        assertEquals("All created files should have been handled", Arrays.asList(), creationOrder);

    }



    private Path createTempFile() throws IOException {
        Path tempTestFile = tempTestDir.resolve(UUID.randomUUID().toString() + ".xml");
        Files.createFile(tempTestFile);
        return tempTestFile;
    }

    private Path createTempFile(int id) throws IOException {
        Path tempTestFile = tempTestDir.resolve(id + ".xml");
        Files.createFile(tempTestFile);
        return tempTestFile;
    }

}
