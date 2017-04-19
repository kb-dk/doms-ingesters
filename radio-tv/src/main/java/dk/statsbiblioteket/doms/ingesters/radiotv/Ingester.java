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
import dk.statsbiblioteket.doms.folderwatching.FolderWatcher;
import dk.statsbiblioteket.doms.folderwatching.FolderWatcherClient;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Ingester {

    private static final Logger log = LoggerFactory.getLogger(Ingester.class);

    /**
     * @param args
     * @throws MalformedURLException
     * @throws MethodFailedException
     * @throws InvalidCredentialsException
     */
    public static void main(String[] args) throws Exception {
        mainMethod(args);
    }

    private static void mainMethod(String[] args) throws IOException,
            InvalidCredentialsException, MethodFailedException,
            InterruptedException, SAXException, ParseException {

        log.info("Ingester starting up ");

        CommandLine cmd = setupCommandLine(args);

        log.info("Ingester started with the following configuration details:");

        Path hotFolder = parseHotfolder(cmd);

        Path lukewarmFolder = parseLukefolder(cmd);

        Path coldFolder = parseColdfolder(cmd);

        Path stopFolder = parseStopfolder(cmd);

        Path preIngestFileSchemaFile = parseSchema(cmd);

        URL domsAPIWSLocation = parseWSDL(cmd);

        String username = parseUsername(cmd);

        String password = parsePassword(cmd);

        boolean overwrite = parseOverwrite(cmd);

        long threadWaitTime = parseThreadWaitTime(cmd);

        int numThreads = parseNumThreads(cmd);

        int maxFails = parseMaxFails(cmd);

        startScanner(hotFolder, coldFolder, lukewarmFolder, stopFolder, preIngestFileSchemaFile,
                     domsAPIWSLocation,
                     username, password, overwrite, numThreads, threadWaitTime, maxFails);
    }

    static int parseMaxFails(CommandLine cmd) {
        int maxFails = Integer.parseInt(cmd.getOptionValue("maxFails", "10"));
        log.info("maxFails = {}", maxFails);
        return maxFails;
    }


    static long parseThreadWaitTime(CommandLine cmd) {
        long threadWaitTime = Long.parseLong(cmd.getOptionValue("threadwaittime", "1000"));
        log.info("threadwaittime = {}", threadWaitTime);
        return threadWaitTime;
    }

    static int parseNumThreads(CommandLine cmd) {
        int numThreads = Integer.parseInt(cmd.getOptionValue("numthreads", "4"));
        log.info("numthreads = {}", numThreads);
        return numThreads;
    }

    static String parseUsername(CommandLine cmd) {
        String username = cmd.getOptionValue("username", "fedoraAdmin");
        log.info("username = {}", username);
        return username;
    }

    static String parsePassword(CommandLine cmd) {
        String password = cmd.getOptionValue("password", "fedoraAdminPass");
        log.info("password = {}", password);
        return password;
    }

    static Path parseStopfolder(CommandLine cmd) throws IOException {
        Path stopFolder = Paths.get(cmd.getOptionValue("stopfolder", "stopFolder"));
        log.info("stopFolder = {}", stopFolder);
        Files.createDirectories(stopFolder);
        return stopFolder;
    }

    static Path parseColdfolder(CommandLine cmd) throws IOException {
        Path coldFolder = Paths.get(cmd.getOptionValue("coldfolder", "processedFiles"));
        log.info("coldFolder = {}", coldFolder);
        Files.createDirectories(coldFolder);
        return coldFolder;
    }

    static Path parseLukefolder(CommandLine cmd) throws IOException {
        Path lukewarmFolder = Paths.get(cmd.getOptionValue("lukefolder", "/tmp/failedFiles"));
        log.info("lukewarmFolder = {}", lukewarmFolder);
        Files.createDirectories(lukewarmFolder);
        return lukewarmFolder;
    }

    static Path parseHotfolder(CommandLine cmd) throws IOException {
        Path hotFolder = Paths.get(cmd.getOptionValue("hotfolder", "radioTVMetaData"));
        log.info("hotFolder = {}", hotFolder);
        Files.createDirectories(hotFolder);
        return hotFolder;
    }

    static Path parseSchema(CommandLine cmd) {
        String defaultValue = fileInClasspath("exportedRadioTVProgram.xsd");
        Path preIngestFileSchemaFile = Paths.get(cmd.getOptionValue("preingestschema", defaultValue));
        log.info("preIngestFileSchemaFile = {}", preIngestFileSchemaFile);
        return preIngestFileSchemaFile;
    }

    /**
     * Find file from the classloader
     * @param name the name of the file
     * @return the path to the file
     */
    private static String fileInClasspath(String name) {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(name);
        if (resource == null){
            return null;
        }
        return new File(resource.getFile()).getAbsolutePath();
    }

    static URL parseWSDL(CommandLine cmd) throws ParseException {
        URL domsAPIWSLocation = (URL) cmd.getParsedOptionValue("wsdl");
        log.info("domsAPIWSLocation = {}", domsAPIWSLocation.toString());
        return domsAPIWSLocation;
    }


    static boolean parseOverwrite(CommandLine cmd) {
        boolean overwrite = Boolean.parseBoolean(cmd.getOptionValue("overwrite", Boolean.FALSE.toString()));
        log.info("overwrite = {}", overwrite);
        return overwrite;
    }

    public static CommandLine setupCommandLine(String[] args) throws ParseException {
        // create Options object
        Options options = new Options();

        options.addOption(Option.builder().longOpt("hotfolder").hasArg().valueSeparator().build());
        options.addOption(Option.builder().longOpt("lukefolder").hasArg().valueSeparator().build());
        options.addOption(Option.builder().longOpt("coldfolder").hasArg().valueSeparator().build());
        options.addOption(Option.builder().longOpt("stopfolder").hasArg().valueSeparator().build());

        options.addOption(Option.builder().longOpt("wsdl").hasArg().type(URL.class).valueSeparator().required().build());
        options.addOption(Option.builder().longOpt("username").hasArg().valueSeparator().build());
        options.addOption(Option.builder().longOpt("password").hasArg().valueSeparator().build());

        options.addOption(Option.builder().longOpt("preingestschema").hasArg().valueSeparator().build());
        options.addOption(Option.builder().longOpt("overwrite").hasArg().valueSeparator().build());

        options.addOption(Option.builder().longOpt("numthreads").hasArg().valueSeparator().build());
        options.addOption(Option.builder().longOpt("threadwaittime").hasArg().valueSeparator().build());
        options.addOption(Option.builder().longOpt("maxFails").hasArg().valueSeparator().build());


        CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

    private static void startScanner(Path hotFolder,
                                     Path coldFolder,
                                     Path lukewarmFolder,
                                     Path stopFolder,
                                     Path preIngestFileSchemaFile,
                                     URL domsAPIWSLocation,
                                     String username,
                                     String password,
                                     boolean overwrite,
                                     int numthreads,
                                     long threadWaitTime,
                                     int maxFails)
            throws SAXException, IOException, InterruptedException {


        final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        final Schema preIngestFileSchema = schemaFactory.newSchema(preIngestFileSchemaFile.toFile());

        DomsWSClient domsClient = new DomsWSClientImpl();
        domsClient.setCredentials(domsAPIWSLocation, username, password);

        final FolderWatcherClient radioTVHotFolderClient = new RadioTVFolderWatcherClient(
                domsClient, lukewarmFolder, coldFolder, preIngestFileSchema, overwrite, maxFails);

        final FolderWatcher folderWatcher = new FolderWatcher(hotFolder, threadWaitTime, radioTVHotFolderClient,
                                                              numthreads, stopFolder);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                folderWatcher.setClosed(true);//Hopefully this will cause the watchers to shut down in time
            }
        });

        folderWatcher.call();
    }
}
