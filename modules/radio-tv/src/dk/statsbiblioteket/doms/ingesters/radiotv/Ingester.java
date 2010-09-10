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
import java.net.MalformedURLException;

import dk.statsbiblioteket.doms.centralWebservice.InvalidCredentialsException;
import dk.statsbiblioteket.doms.centralWebservice.MethodFailedException;

/**
 * @author tsh
 * 
 */
public class Ingester {

    /**
     * @param args
     * @throws MalformedURLException
     * @throws MethodFailedException
     * @throws InvalidCredentialsException
     */
    public static void main(String[] args) throws MalformedURLException,
	    InvalidCredentialsException, MethodFailedException, InterruptedException {
	
	final File HOT_FOLDER = new File("/tmp/radioTVMetaData");
	final File LUKEWARM_FOLDER = new File("/tmp/failedFiles");
	final File COLD_FOLDER = new File("/tmp/processedFiles");
	final File FOXML_FOLDER = new File("/tmp/ingestedFoxMLFiles");

	// Make sure that all the necessary folders exist.
	if (!HOT_FOLDER.exists()) {
	    HOT_FOLDER.mkdirs();
	}

	if (!LUKEWARM_FOLDER.exists()) {
	    LUKEWARM_FOLDER.mkdirs();
	}
	
	if (!COLD_FOLDER.exists()) {
	    COLD_FOLDER.mkdirs();
	}

	if (!FOXML_FOLDER.exists()) {
	    FOXML_FOLDER.mkdirs();
	}

	final HotFolderScanner hotFolderScanner = new HotFolderScanner();
	final RadioTVMetadataProcessor metadataProcessor= new RadioTVMetadataProcessor(LUKEWARM_FOLDER, COLD_FOLDER, FOXML_FOLDER);
	hotFolderScanner.startScanning(HOT_FOLDER, metadataProcessor);

	// Hang forever....
	Thread.currentThread().wait();
    }
}
