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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.ws.BindingProvider;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import dk.statsbiblioteket.doms.centralWebservice.CentralWebservice;
import dk.statsbiblioteket.doms.centralWebservice.CentralWebserviceService;
import dk.statsbiblioteket.doms.centralWebservice.InvalidCredentialsException;
import dk.statsbiblioteket.doms.centralWebservice.MethodFailedException;

/**
 * @author tsh
 * 
 */
public class RadioTVMetadataProcessor implements HotFolderScannerClient {

    private final File failedFilesFolder;
    private final File processedFilesFolder;
    private final File foxMLFolder;

    public RadioTVMetadataProcessor(File failedFilesFolder,
	    File processedFilesFolder, File foxMLFolder) {
	this.failedFilesFolder = failedFilesFolder;
	this.processedFilesFolder = processedFilesFolder;
	this.foxMLFolder = foxMLFolder;
    }

    /* (non-Javadoc)
     * @see dk.statsbiblioteket.doms.ingesters.radiotv.HotFolderScannerClient#fileAdded(java.io.File)
     */
    @Override
    public void fileAdded(File addedFile) {

	try {
	    final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
		    .newInstance();
	    final DocumentBuilder documentBuilder = documentBuilderFactory
		    .newDocumentBuilder();
	    final Document radioTVMetadata = documentBuilder.parse(addedFile);

	    final FoxMLBuilder foxMLBuilder = new FoxMLBuilder();

	    final URL domsAPIWSLocation = new URL(
		    "http://localhost:8080/centralDomsWebservice/central/?wsdl");

	    final DOMSClient domsClient = new DOMSClient();
	    domsClient.login(domsAPIWSLocation, "fedoraAdmin",
		    "fedoraAdminPass");
	    ingestShards(domsClient, radioTVMetadata);

	    // build file FoxMLs
	    // build program FoxML
	    // ingest FoxMLs

	    /*
	     * - getObjectForFile() or create object: newObject(FileTemplate)
	     * - addFile(uri, pid)
	     * - getDS(Metadata)
	     * - modifyDS(metadata, content)
	     * - newObject(shardToUpdate)
	     */

	    // Move the processed file to the finished files folder.
	    moveFile(addedFile, processedFilesFolder);

	} catch (ParserConfigurationException pce) {
	    moveFile(addedFile, failedFilesFolder);
	    // TODO: Log this
	    // TODO: we should not allow endless failures....
	} catch (SAXException se) {
	    moveFile(addedFile, failedFilesFolder);
	    // TODO: Log this
	    // TODO: we should not allow endless failures....

	} catch (IOException ioe) {
	    moveFile(addedFile, failedFilesFolder);
	    // TODO: Log this
	    // TODO: we should not allow endless failures....
	}

    }

    /* (non-Javadoc)
     * @see dk.statsbiblioteket.doms.ingesters.radiotv.HotFolderScannerClient#fileDeleted(java.io.File)
     */
    @Override
    public void fileDeleted(File deletedFile) {
	// Not relevant.
    }

    /* (non-Javadoc)
     * @see dk.statsbiblioteket.doms.ingesters.radiotv.HotFolderScannerClient#fileModified(java.io.File)
     */
    @Override
    public void fileModified(File modifiedFile) {
	// Not relevant.
    }

    private void ingestShards(DOMSClient domsClient, Document radioTVMetadata) {
	

    }

    private void ingestFile(DOMSClient domsClient, Document radioTVMetadata) {
	//System.out.println(domsClient.createFileObject(new URL("doms:Template_RadioTVFile")));

    }
    /**
     * Move <code>fileToMove</code> to the folder specified by
     * <code>destinationFolder</code>.
     * 
     * @param fileToMove
     *            Path to the file to move to <code>destinationFolder</code>.
     * @param destinationFolder
     *            Path of the destination folder to move the file to.
     */
    private void moveFile(File fileToMove, File destinationFolder) {
	fileToMove.renameTo(new File(destinationFolder.getAbsolutePath()
	        + fileToMove.getName()));
    }

}
