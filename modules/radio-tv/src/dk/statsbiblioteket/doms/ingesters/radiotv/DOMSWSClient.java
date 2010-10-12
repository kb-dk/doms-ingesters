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

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.ws.BindingProvider;

import org.w3c.dom.Document;

import dk.statsbiblioteket.doms.centralWebservice.CentralWebservice;
import dk.statsbiblioteket.doms.centralWebservice.CentralWebserviceService;
import dk.statsbiblioteket.util.xml.DOM;

/**
 * TODO: This class and its exceptions and datacarrier objects have been copied
 * to the summa integration. It should be moved into some utility library some
 * time! However, it probably should be the copy that should be chosen, as it
 * has under gone further development.
 * <p/>
 * Utility class for making it simple and easy to access the DOMS server
 * webservice.
 * 
 * @author &lt;tsh@statsbiblioteket.dk&gt;
 */
public class DOMSWSClient {

    /**
     * Reference to the active DOMS webservice client instance.
     */
    private CentralWebservice domsAPI;

    /**
     * Login to the DOMS webservice, using the endpoint <code>URL</code>
     * specified by <code>domsWSAPIEndpoint</code> and the credentials given by
     * <code>userName</code> and <code>password</code>.
     * 
     * @param domsWSAPIEndpoint
     *            <code>URL</code> of the DOMS server webservice endpoint.
     * @param userName
     *            Name of the user to use for identification.
     * @param password
     *            Password of the user to use for identification.
     */
    public void login(URL domsWSAPIEndpoint, String userName, String password) {
        domsAPI = new CentralWebserviceService(domsWSAPIEndpoint, new QName(
                "http://central.doms.statsbiblioteket.dk/",
                "CentralWebserviceService")).getCentralWebservicePort();

        Map<String, Object> domsAPILogin = ((BindingProvider) domsAPI)
                .getRequestContext();
        domsAPILogin.put(BindingProvider.USERNAME_PROPERTY, userName);
        domsAPILogin.put(BindingProvider.PASSWORD_PROPERTY, password);
    }

    /**
     * Create a new DOMS object from an object template already stored in the
     * DOMS.
     * 
     * @param templatePID
     *            PID identifying the template object to use.
     * @return PID of the created object.
     * @throws ServerOperationFailed
     *             if the object creation failed.
     */
    public String createObjectFromTemplate(String templatePID)
            throws ServerOperationFailed {
        try {
            return domsAPI.newObject(templatePID);
        } catch (Exception e) {
            throw new ServerOperationFailed(
                    "Failed creating a new object from template: "
                            + templatePID, e);
        }
    }

    /**
     * Create a new file object from an existing file object template, based on
     * the information provided by the <code>FileInfo</code> instance, in the
     * DOMS.
     * 
     * @param templatePID
     *            The PID of the file template object to use for creation of the
     *            new file object.
     * @param fileInfo
     *            File location, checksum and so on for the physical file
     *            associated with the object.
     * @return PID of the created file object in the DOMS.
     * @throws ServerOperationFailed
     *             if the object creation failed.
     * @see FileInfo
     */
    public String createFileObject(String templatePID, FileInfo fileInfo)
            throws ServerOperationFailed {

        try {
            final String fileObjectPID = createObjectFromTemplate(templatePID);
            addFileToFileObject(fileObjectPID, fileInfo);
            return fileObjectPID;
        } catch (Exception e) {
            throw new ServerOperationFailed(
                    "Failed creating a new file object (template PID: "
                            + templatePID + ") from this file information: "
                            + fileInfo, e);
        }
    }

    /**
     * Add a physical file to an existing file object in the DOMS.
     * <p/>
     * 
     * The existing file object in DOMS with <code>fileObjectPID</code> will be
     * associated with the pysical file described by the <code>FileInfo</code>
     * instance.
     * 
     * @param fileObjectPID
     *            The PID of the DOMS file object to associate the physical file with.
     * @param fileInfo
     *            File location, checksum and so on for the physical file
     *            associated with the file object.
     * @throws ServerOperationFailed
     *             if the operation fails.
     * @see FileInfo
     */
    public void addFileToFileObject(String fileObjectPID, FileInfo fileInfo)
            throws ServerOperationFailed {

        try {
            domsAPI.addFileFromPermanentURL(fileObjectPID, fileInfo
                    .getFileName(), fileInfo.getMd5Sum(), fileInfo
                    .getFileLocation().toString(), fileInfo.getFileFormatURI()
                    .toString());
        } catch (Exception e) {
            throw new ServerOperationFailed(
                    "Failed adding a file to a file object (file object PID: "
                            + fileObjectPID + ") from this file information: "
                            + fileInfo, e);
        }
    }

    /**
     * Get the PID of an existing file object in the DOMS which is associated
     * with the physical file specified by <code>fileURL</code>.
     * 
     * @param fileURL
     *            location of the physical file to find the corresponding DOMS
     *            file object for.
     * @return PID of the DOMS file object.
     * @throws NoObjectFound
     *             if there does not exist DOMS file object associated with
     *             <code>fileURL</code>.
     * @throws ServerOperationFailed
     *             if any errors are encountered while looking up the file
     *             object.
     */
    public String getFileObjectPID(URL fileURL) throws NoObjectFound,
            ServerOperationFailed {

        String pid = null;
        try {
            pid = domsAPI.getFileObjectWithURL(fileURL.toString());
        } catch (Exception exception) {
            throw new ServerOperationFailed(
                    "Unable to retrieve file object with URL: " + fileURL,
                    exception);
        }
        if (pid == null) {
            throw new NoObjectFound("Unable to retrieve file object with URL: "
                    + fileURL);
        }
        return pid;
    }

    /**
     * Get the XML content of the datastream identified by
     * <code>datastreamID</code> of the DOMS object identified by
     * <code>objectPID</code>.
     * 
     * @param objectPID
     *            ID of the DOMS object to retrieve the datastream contents of.
     * @param datastreamID
     *            ID of the datastream to get the contents of.
     * @return <code>Document</code> containing the datastream XML contents.
     * @throws ServerOperationFailed
     *             if the datastream contents cannot be retrieved.
     */
    public Document getDataStream(String objectPID, String datastreamID)
            throws ServerOperationFailed {
        try {
            final String datastreamXML = domsAPI.getDatastreamContents(
                    objectPID, datastreamID);

            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
                    .newInstance();

            final DocumentBuilder documentBuilder = documentBuilderFactory
                    .newDocumentBuilder();

            final ByteArrayInputStream datastreamBytes = new ByteArrayInputStream(
                    datastreamXML.getBytes());
            final Document dataStream = documentBuilder.parse(datastreamBytes);

            return dataStream;
        } catch (Exception exception) {
            throw new ServerOperationFailed("Failed getting datastream (ID: "
                    + datastreamID + ") contents from object (PID: "
                    + objectPID + ")", exception);
        }
    }

    /**
     * Replace any existing datastream contents of the datastream, identified by
     * <code>dataStreamID</code> of the DOMS object identified by
     * <code>objectPID</code>, with the contents provided by the
     * <code>newDataStreamContents</code> <code>Document</code>.
     * 
     * @param objectPID
     *            ID of the DOMS object containing the datastream to replace the
     *            contents of.
     * @param dataStreamID
     *            ID of the datastream to replace the contents of.
     * @param newDataStreamContents
     *            <code>Document</code> containing the new datastream XML
     *            contents.
     * @throws ServerOperationFailed
     *             if the datastream contents cannot be updated.
     */
    public void updateDataStream(String objectPID, String dataStreamID,
            Document newDataStreamContents) throws ServerOperationFailed {
        try {
            domsAPI.modifyDatastream(objectPID, dataStreamID, DOM
                    .domToString(newDataStreamContents));

        } catch (Exception exception) {
            throw new ServerOperationFailed("Failed updating datastream (ID: "
                    + dataStreamID + ") contents from object (PID: "
                    + objectPID + ")", exception);
        }

    }

    /**
     * Add a relation between the objects identified by <code>sourcePID</code>
     * and <code>targetPID</code>. The information about the relation will be
     * stored in the source object identified by <code>sourcePID</code> and the
     * type specified by <code>relationType</code> must be valid according to
     * the content model for the object.
     * 
     * @param sourcePID
     *            ID of the source object in the relation.
     * @param relationType
     *            Relation type ID which is valid according the the content
     *            model for the source object.
     * @param targetPID
     *            ID of the target ("to") object of the relation.
     * @throws ServerOperationFailed
     *             if the relation cannot be added.
     */
    public void addObjectRelation(String sourcePID, String relationType,
            String targetPID) throws ServerOperationFailed {
        try {
            domsAPI.addRelation(sourcePID, "info:fedora/" + sourcePID,
                    relationType, "info:fedora/" + targetPID);
        } catch (Exception exception) {
            throw new ServerOperationFailed(
                    "Failed creating object relation (type: " + relationType
                            + ") from the source object (PID: " + sourcePID
                            + ") to the target object (PID: " + targetPID + ")",
                    exception);
        }
    }

    /**
     * Mark the objects identified by the the PIDs in <code>pidsToPublish</code>
     * as published, and thus viewable from the DOMS.
     * 
     * @param pidsToPublish
     *            <code>List</code> of PIDs for the objects to publish.
     * @throws ServerOperationFailed
     *             if any errors are encountered while publishing the objects.
     */
    public void publishObjects(List<String> pidsToPublish)
            throws ServerOperationFailed {
        try {
            domsAPI.markPublishedObject(pidsToPublish);
        } catch (Exception exception) {
            throw new ServerOperationFailed(
                    "Failed marking objects as published. PIDs: "
                            + pidsToPublish, exception);
        }
    }
}
