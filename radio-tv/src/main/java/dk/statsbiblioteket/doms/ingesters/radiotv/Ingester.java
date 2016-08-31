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

import dk.statsbiblioteket.doms.central.InvalidCredentialsException;
import dk.statsbiblioteket.doms.central.MethodFailedException;
import dk.statsbiblioteket.doms.client.DomsWSClient;
import dk.statsbiblioteket.doms.client.DomsWSClientImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Ingester {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * @param args
     * @throws MalformedURLException
     * @throws MethodFailedException
     * @throws InvalidCredentialsException
     */
    public static void main(String[] args) throws Exception {
        new Ingester().mainInstance(args);
    }

    //TODO better arg parsing than this...
    private void mainInstance(String[] args) throws IOException,
            InvalidCredentialsException, MethodFailedException,
            InterruptedException, SAXException {

        log.info("Ingester starting up ");

        Path hotFolder = Paths.get("radioTVMetaData");

        Path coldFolder = Paths.get("processedFiles");
        Path lukewarmFolder = Paths.get("/tmp/failedFiles");
        Path stopFolder = Paths.get("stopFolder");

        Path preIngestFileSchemaFile = Paths.get(
                "src/main/resources/exportedRadioTVProgram.xsd");

        URL domsAPIWSLocation = new URL(
                "http://localhost:7880/centralWebservice-service/central/?wsdl");

        String username = "fedoraAdmin";
        String password = "fedoraAdminPass";

        boolean overwrite = false;

        for (String arg : args) {
            if (arg.startsWith("-hotfolder=")) {
                hotFolder = Paths.get(arg.substring("-hotfolder=".length()));
            } else if (arg.startsWith("-lukefolder=")) {
                lukewarmFolder = Paths.get(arg.substring("-lukefolder="
                        .length()));
            } else if (arg.startsWith("-coldfolder=")) {
                coldFolder = Paths.get(arg.substring("-coldfolder=".length()));
            } else if (arg.startsWith("-wsdl=")) {
                domsAPIWSLocation = new URL(arg.substring("-wsdl=".length()));
            } else if (arg.startsWith("-username=")) {
                username = arg.substring("-username=".length());
            } else if (arg.startsWith("-stopfolder=")) {
                stopFolder = Paths.get(arg.substring("-stopfolder=".length()));
            } else if (arg.startsWith("-password=")) {
                password = arg.substring("-password=".length());
            } else if (arg.startsWith("-preingestschema=")) {
                preIngestFileSchemaFile = Paths.get(arg.substring("-preingestschema=".length()));
            } else if (arg.startsWith("-overwrite=")) {
                overwrite = Boolean.parseBoolean(arg.substring("-overwrite=".length()));
            }
        }
        log.info("Ingester started with the following configuration "
                + "detatils:");
        log.info("hotFolder = " + hotFolder);
        log.info("lukewarmFolder = "
                + lukewarmFolder);
        log.info("coldFolder = " + coldFolder);
        log.info("stopFolder = " + stopFolder);
        log.info("preIngestFileSchemaFile = "
                + preIngestFileSchemaFile);
        log.info("domsAPIWSLocation = "
                + domsAPIWSLocation.toString());
        log.info("username = " + username);
        log.info("password = " + password);
        log.info("overwrite = " + overwrite);

        // Make sure that all the necessary folders exist.
        Files.createDirectories(hotFolder);

        Files.createDirectories(lukewarmFolder);

        Files.createDirectories(coldFolder);

        Files.createDirectories(stopFolder);

        startScanner(hotFolder, coldFolder, lukewarmFolder, stopFolder, preIngestFileSchemaFile,
                     domsAPIWSLocation,
                     username, password, overwrite);
    }

    private void startScanner(Path hotFolder,
                              Path coldFolder,
                              Path lukewarmFolder,
                              Path stopFolder,
                              Path preIngestFileSchemaFile,
                              URL domsAPIWSLocation,
                              String username,
                              String password,
                              boolean overwrite)
            throws SAXException, IOException, InterruptedException {


        final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        final Schema preIngestFileSchema = schemaFactory.newSchema(preIngestFileSchemaFile.toFile());

        DomsWSClient domsClient = new DomsWSClientImpl();
        domsClient.setCredentials(domsAPIWSLocation,username,password);

        final RadioTVFolderWatcherClient radioTVHotFolderClient = new RadioTVFolderWatcherClient(
                domsClient, lukewarmFolder, coldFolder,
                preIngestFileSchema, overwrite);

        final HotFolderScanner folderScanner = new HotFolderScanner();
        folderScanner.startScanning(hotFolder, stopFolder, radioTVHotFolderClient);
        folderScanner.waitForStop();
    }
}
