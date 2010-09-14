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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author tsh
 * 
 */
public class RadioTVMetadataProcessor implements HotFolderScannerClient {

    private static final String RECORDING_FILES_FILE_ELEMENT = "//program_recording_files/file";
    private static final String FILE_URL_ELEMENT = "file_url";
    private static final String FILE_NAME_ELEMENT = "file_name";
    private static final String FORMAT_URI_ELEMENT = "format_uri";
    private static final String MD5_SUM_ELEMENT = "md5_sum";

    private static final String META_FILE_TEMPLATE_PID = "doms:Template_Shard";
    private static final String RADIO_TV_FILE_TEMPLATE_PID = "doms:Template_RadioTVFile";
    private static final String META_FILE_METADATA_DS_ID = "SHARD_METADATA";

    private final File failedFilesFolder;
    private final File processedFilesFolder;
    private final File foxMLFolder;
    private final DOMSLoginInfo domsLoginInfo;
    private final XPathFactory xPathFactory;

    public RadioTVMetadataProcessor(DOMSLoginInfo domsLoginInfo,
	    File failedFilesFolder, File processedFilesFolder, File foxMLFolder) {
	this.failedFilesFolder = failedFilesFolder;
	this.processedFilesFolder = processedFilesFolder;
	this.foxMLFolder = foxMLFolder;
	this.domsLoginInfo = domsLoginInfo;

	xPathFactory = XPathFactory.newInstance();
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

	    final DOMSClient domsClient = new DOMSClient();
	    domsClient.login(domsLoginInfo.getDomsWSAPIUrl(), domsLoginInfo
		    .getLogin(), domsLoginInfo.getPassword());

	    final List<String> filePIDs = ingestFiles(domsClient,
		    radioTVMetadata);
	    final String metaFilePID = ingestMetaFile(domsClient,
		    radioTVMetadata, filePIDs);
	    final String programPID = ingestProgram(domsClient,
		    radioTVMetadata, metaFilePID);

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
	} catch (ServerError se) {
	    // Failed calling the DOMS server
	    moveFile(addedFile, failedFilesFolder);
	    // TODO: Log this
	    // TODO: we should not allow endless failures....
	} catch (XPathExpressionException xpee) {
	    // Failed parsing the Radio-TV XML document...
	    moveFile(addedFile, failedFilesFolder);
	    // TODO: Log this
	    // TODO: we should not allow endless failures....

	} catch (URISyntaxException use) {
	    // Failed parsing the Radio-TV XML document...
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

    private String ingestProgram(DOMSClient domsClient,
	    Document radioTVMetadata, String shardPID) {
	// TODO Auto-generated method stub
	return null;
    }

    private String ingestMetaFile(DOMSClient domsClient,
	    Document radioTVMetadata, List<String> filePIDs)
	    throws ServerError, IOException, SAXException,
	    ParserConfigurationException {

	final String metaFilePID = domsClient
	        .createObjectFromTemplate(META_FILE_TEMPLATE_PID);

	final Document metadataDataStream = domsClient.getDataStream(metaFilePID,
	        META_FILE_METADATA_DS_ID);

	System.out.println("This does not work, does it? " + metadataDataStream.getNextSibling().getTextContent());
	
	domsClient.updateDataStream(metaFilePID, META_FILE_METADATA_DS_ID, metadataDataStream);
	return metaFilePID;
    }

    /**
     * Ingest any missing file objects into the DOMS and return a list of PIDs
     * for all the DOMS file objects corresponding to the files listed in the
     * <code>radioTVMetadata</code> document.
     * 
     * @param domsClient
     *            Reference to the DOMS client to communicate through.
     * @param radioTVMetadata
     *            Metadata XML document containing the file information.
     * @return A <code>List</code> of DOMS file object PIDs.
     * @throws XPathExpressionException
     *             if any errors were encountered while processing the
     *             <code>radioTVMetadata</code> XML document.
     * @throws MalformedURLException
     *             if a file element contains an invalid URL.
     * @throws IOException
     *             if a file specified in the <code>radioTVMetadata</code>
     *             document could not be read for checksum calculation.
     * @throws URISyntaxException
     *             if the format URI for the file is invalid.
     */
    private List<String> ingestFiles(DOMSClient domsClient,
	    Document radioTVMetadata) throws XPathExpressionException,
	    MalformedURLException, ServerError, IOException, URISyntaxException {

	// Get the recording files XML element and process the file information.
	final XPath xPath = this.xPathFactory.newXPath();
	final NodeList recordingFiles = (NodeList) xPath.evaluate(
	        RECORDING_FILES_FILE_ELEMENT, radioTVMetadata,
	        XPathConstants.NODESET);

	// Ensure that the DOMS contains a file object for each recording file
	// element in the radio-tv XML document.
	final List<String> fileObjectPIDs = new ArrayList<String>();
	for (int nodeIndex = 0; nodeIndex < recordingFiles.getLength(); nodeIndex++) {

	    final Node currentFileNode = recordingFiles.item(nodeIndex);

	    final Node fileURLNode = (Node) xPath.evaluate(FILE_URL_ELEMENT,
		    currentFileNode, XPathConstants.NODE);
	    final String fileURLString = fileURLNode.getTextContent();
	    final URL fileURL = new URL(fileURLString);

	    String fileObjectPID;
	    try {
		fileObjectPID = domsClient.getFileObjectPID(fileURL);
	    } catch (NoObjectFound nof) {
		// The DOMS contains no file object for this file URL.
		// Create a new one now.
		final Node fileNameNode = (Node) xPath
		        .evaluate(FILE_NAME_ELEMENT, currentFileNode,
		                XPathConstants.NODE);

		final String fileName = fileNameNode.getTextContent();

		final Node formatURINode = (Node) xPath.evaluate(
		        FORMAT_URI_ELEMENT, currentFileNode,
		        XPathConstants.NODE);

		final URI formatURI = new URI(formatURINode.getTextContent());

		final Node md5SumNode = (Node) xPath.evaluate(MD5_SUM_ELEMENT,
		        currentFileNode, XPathConstants.NODE);
		final String md5String = md5SumNode.getTextContent();

		final FileInfo fileInfo = new FileInfo(fileName, fileURL,
		        md5String, formatURI);

		fileObjectPID = domsClient.createFileObject(
		        RADIO_TV_FILE_TEMPLATE_PID, fileInfo);
	    }
	    fileObjectPIDs.add(fileObjectPID);
	}
	return fileObjectPIDs;
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
	        + File.separator + fileToMove.getName()));
    }

}
