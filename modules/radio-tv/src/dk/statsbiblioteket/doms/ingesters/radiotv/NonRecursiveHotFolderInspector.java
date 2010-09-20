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

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;

/**
 * This class performs a shallow (i.e. non-recursive) scan of a directory for
 * file modifications. That is, detection of creation, modification or deletion
 * of files in the directory, a.k.a. hot folder.
 *
 * @author &lt;tsh@statsbiblioteket.dk&gt;
 */
public class NonRecursiveHotFolderInspector extends TimerTask {

    /**
     * Full path to the hot folder to scan.
     */
    private final File folderToScan;

    /**
     * Map containing paths and timestamps for all files found in the hot folder
     * at the previous scanning.
     */
    private final HashMap<File, Long> previousFolderContents;

    /**
     * Reference to the client to call when any changes are detected.
     */
    // TODO: We could make this class observable instead. However, we do not
    // need that now.
    private final HotFolderScannerClient callBackClient;

    /**
     * Create a <code>NonRecursiveHotFolderInspector</code> instance which scans
     * the folder specified by <code>hotFolderToScan</code> and notifies the
     * client specified by <code>client</code> about any changes, whenever the
     * <code>{@link #run()}</code> method is executed.
     *
     * @param hotFolderToScan
     *            File path to a hot folder to scan.
     * @param client
     *            Reference to a client to notify about changes in the folder.
     */
    public NonRecursiveHotFolderInspector(File hotFolderToScan,
                                          HotFolderScannerClient client) {
        folderToScan = hotFolderToScan;
        callBackClient = client;
        previousFolderContents = new HashMap<File, Long>();
    }

    /**
     * Scan the hot folder for any created, modified or deleted files and notify
     * the client about this.
     */
    @Override
    public void run() {
        // Scan the hot folder for file addition, deletion or modification.
        final List<File> currentFolderContents = Arrays.asList(folderToScan
                .listFiles());

        for (File currentFile : currentFolderContents) {

            final Long previousTimeStamp = previousFolderContents
                    .get(currentFile);

            if (previousTimeStamp == null) {
                // A new file has been created.
                previousFolderContents.put(currentFile, currentFile
                        .lastModified());
                callBackClient.fileAdded(currentFile);
            } else if (!previousTimeStamp.equals(currentFile.lastModified())) {
                // The file has been modified since the previous scan. Update
                // the time stamps and notify the client.
                previousFolderContents.put(currentFile, currentFile
                        .lastModified());
                callBackClient.fileModified(currentFile);
            }
        }

        // Remove information about any deleted files and notify the client.
        Set<File> deletedFiles = new HashSet<File>(previousFolderContents
                .keySet());
        deletedFiles.removeAll(currentFolderContents);
        for (File deletedFile : deletedFiles) {
            previousFolderContents.remove(deletedFile);
            callBackClient.fileDeleted(deletedFile);
        }
    }
}
