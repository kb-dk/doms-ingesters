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

/**
 * Clients that wants to be notified by the
 * <code>{@link HotFolderScanner}</code> about file changes in a hot folder,
 * must implement this interface.
 * 
 * @author tsh
 */
public interface HotFolderScannerClient {

    /**
     * The <code>{@link HotFolderScanner}</code> will invoke this method each
     * time a new file has been created in the hot folder.
     * 
     * @param addedFile Full path to the new file.
     */
    void fileAdded(File addedFile);

    /**
     * The <code>{@link HotFolderScanner}</code> will invoke this method each
     * time a file has been modified in the hot folder.
     * 
     * @param addedFile Full path to the modified file.
     */
    void fileModified(File modifiedFile);

    /**
     * The <code>{@link HotFolderScanner}</code> will invoke this method each
     * time a new file has been deleted from the hot folder.
     * 
     * @param addedFile Full path to the deleted file.
     */
    void fileDeleted(File deletedFile);
}
