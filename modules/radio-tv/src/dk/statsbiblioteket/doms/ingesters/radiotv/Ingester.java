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

import dk.statsbiblioteket.doms.centralWebservice.InvalidCredentialsException;
import dk.statsbiblioteket.doms.centralWebservice.MethodFailedException;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author &lt;tsh@statsbiblioteket.dk&gt;
 * 
 */
public class Ingester {

    /**
     * @param args
     * @throws MalformedURLException
     * @throws MethodFailedException
     * @throws InvalidCredentialsException
     */
    public static void main(String[] args) throws Exception {
        new Ingester().mainInstance(args);
    }

    private void mainInstance(String[] args) throws MalformedURLException,
            InvalidCredentialsException, MethodFailedException,
            InterruptedException, SAXException {

        File HOT_FOLDER = new File("/tmp/radioTVMetaData");
        File LUKEWARM_FOLDER = new File("/tmp/failedFiles");
        File COLD_FOLDER = new File("/tmp/processedFiles");

        File PRE_INGEST_FILE_SCHEMA_FILE = new File(
                "config/preingestedRadioTVProgram.xsd");

        URL domsAPIWSLocation = new URL(
                "http://alhena:7980/centralDomsWebservice/central/?wsdl");

        String username = "fedoraAdmin";
        String password = "fedoraAdminPass";

        for (String arg : args) {
            if (arg.startsWith("-hotfolder=")) {
                HOT_FOLDER = new File(arg.substring("-hotfolder=".length()));
            } else if (arg.startsWith("-lukefolder=")) {
                LUKEWARM_FOLDER = new File(arg.substring("-lukefolder="
                        .length()));
            } else if (arg.startsWith("-coldfolder=")) {
                COLD_FOLDER = new File(arg.substring("-coldfolder=".length()));
            } else if (arg.startsWith("-wsdl=")) {
                domsAPIWSLocation = new URL(arg.substring("-wsdl=".length()));
            } else if (arg.startsWith("-username=")) {
                username = arg.substring("-username=".length());
            } else if (arg.startsWith("-password=")) {
                password = arg.substring("-password=".length());
            } else if (arg.startsWith("-preingestschema=")) {
                PRE_INGEST_FILE_SCHEMA_FILE = new File(arg
                        .substring("-preingestschema=".length()));
            }
        }
        System.out.println("Ingester started with the following configuration "
                            + "detatils:");
        System.out.println("HOT_FOLDER = " + HOT_FOLDER.getAbsolutePath());
        System.out.println("LUKEWARM_FOLDER = " + LUKEWARM_FOLDER.getAbsolutePath());
        System.out.println("COLD_FOLDER = " + COLD_FOLDER.getAbsolutePath());
        System.out.println("PRE_INGEST_FILE_SCHEMA_FILE = " +
                            PRE_INGEST_FILE_SCHEMA_FILE.getAbsolutePath());
        System.out.println("domsAPIWSLocation = "+domsAPIWSLocation.toString());
        System.out.println("username = " + username);
        System.out.println("password = " + password);


        // Make sure that all the necessary folders exist.
        if (!HOT_FOLDER.exists()) {
            HOT_FOLDER.mkdirs();
            System.out.println("HOT_FOLDER: " + HOT_FOLDER.getName() + " did "
                                + "not exist. Has been created.");
        }

        if (!LUKEWARM_FOLDER.exists()) {
            LUKEWARM_FOLDER.mkdirs();
            System.out.println("LUKEWARM_FOLDER: " + LUKEWARM_FOLDER.getName()
                                + " did not exist. Has been created.");
        }

        if (!COLD_FOLDER.exists()) {
            COLD_FOLDER.mkdirs();
            System.out.println("COLD_FOLDER: " + COLD_FOLDER.getName() +
                                " did not exist. Has been created.");
        }

        final HotFolderScanner hotFolderScanner = new HotFolderScanner();

        final DOMSLoginInfo domsLoginInfo = new DOMSLoginInfo(
                domsAPIWSLocation, username, password);

        final SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        final Schema preIngestFileSchema = schemaFactory
                .newSchema(PRE_INGEST_FILE_SCHEMA_FILE);

        final RadioTVMetadataProcessor metadataProcessor = new RadioTVMetadataProcessor(
                domsLoginInfo, LUKEWARM_FOLDER, COLD_FOLDER,
                preIngestFileSchema);
        hotFolderScanner.startScanning(HOT_FOLDER, metadataProcessor);

        // Hang forever....
        synchronized (this) {
            wait();
        }
    }
}
