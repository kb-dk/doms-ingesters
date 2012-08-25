package dk.statsbiblioteket.doms.ingesters.ffprobeMetadataInjector.ingester;

import dk.statsbiblioteket.doms.client.DomsWSClient;
import dk.statsbiblioteket.doms.client.DomsWSClientImpl;
import dk.statsbiblioteket.doms.client.NoObjectFound;
import dk.statsbiblioteket.doms.client.ServerOperationFailed;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: eab
 * Date: 11/22/11
 *
 * Code commented and made to launch in the same way as the radio/tv ingester
 * - by jrg
 */
public class Ingester {
    /**
     * Main method to launch the mainInstance method.
     *
     * @param args Command-line arguments - see mainInstance.
     */
    public static void main(String[] args) {
        new Ingester().mainInstance(args);
    }

    /**
     * Assuming they have been generated beforehand, inject the FFProbe output
     * XML files into the relevant DOMS File Object for each. These FFProbe XML
     * files provide technical metadata about the radio/tv file of each File
     * Object.
     *
     * @param args Command-line arguments. Seven are expected: input dir,
     * done dir, doms wsdl, doms username, doms password, bart urlprefix, and
     * ffprobe options.
     */
    private void mainInstance(String[] args) {
        // Check command-line arguments
        if (args.length != 7) {
            System.err.println("Ingester takes exactly seven arguments: \n"
            + "input dir, done dir, doms wsdl, doms username, doms password,"
            + " bart urlprefix, and ffprobe options.");
            System.exit(128);
        }

        // Get config from command-line arguments
        File inDir = new File(args[0]);
        File doneDir = new File(args[1]); // Where to move done input files to
        File[] inputFiles = inDir.listFiles();
        Properties prop = new Properties();
        prop.setProperty("doms.wsdl", args[2]);
        prop.setProperty("doms.user", args[3]);
        prop.setProperty("doms.password", args[4]);
        prop.setProperty("bart.urlprefix", args[5]);
        prop.setProperty("ffprobe.options", args[6]);

        // Get DOMS webservice client
        DomsWSClient dClient = new DomsWSClientImpl();
        try {
            dClient.setCredentials(
                    new URL(prop.getProperty("doms.wsdl")),
                    prop.getProperty("doms.user"),
                    prop.getProperty("doms.password"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Get DocumentBuilder for creating document from input FFProbe XML
        DocumentBuilderFactory docBuilderFactory
                = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
        try {
            docBuilder = docBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            System.err.println("Failed to create docBuilder");
            System.exit(64);
        }

        // Inject each input FFProbe XML file into its relevant DOMS File Object
        for (File inputFile : inputFiles) {
            Document outputDoc;
            String fileName = inputFile.getName();
            String originalFileName
                    = fileName.substring(0, fileName.length() - 4);
            URL url;
            String PID = "";
            // Publish message will be re-defined in case of errors
            String publishMessage = "all done injecting ffprobe metadata";

            // Read and parse input XML file
            try {
                outputDoc = docBuilder.parse(inputFile);
            } catch (SAXException e) {
                e.printStackTrace();
                continue;
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            // Create URL to the relevant radio/tv file
            try {
                url = new URL(prop.getProperty("bart.urlprefix")
                        + originalFileName);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                System.err.println("URL malformed using filename: "
                        + originalFileName);
                continue;
            }

            // Make the DOMS File Object for the relevant radio/tv file editable
            try {
                PID = dClient.getFileObjectPID(url);
                dClient.unpublishObjects("Unpublishing in order to inject "
                        + "ffprobe metadata", PID);
            } catch (NoObjectFound noObjectFound) {
                noObjectFound.printStackTrace();
                continue;
            } catch (ServerOperationFailed serverOperationFailed) {
                serverOperationFailed.printStackTrace();
                System.err.println("Failed un-publishing pid: " + PID);
                continue;
            }

            // Add the FFProbe technical metadata to the File Object
            try {
                dClient.updateDataStream(PID, "FFPROBE", outputDoc,
                        "injected the ffprobe metadata");
            } catch (ServerOperationFailed serverOperationFailed) {
                System.err.println("datastream update failed");
                publishMessage = "modifications failed, "
                        + "republishing the document";
                serverOperationFailed.printStackTrace();
            }

            // Publish the change made to the File Object
            try {
                dClient.publishObjects(publishMessage, PID);
            } catch (ServerOperationFailed serverOperationFailed) {
                serverOperationFailed.printStackTrace();
                // Retry
                try {
                    dClient.publishObjects(publishMessage, PID);
                } catch (ServerOperationFailed sof) {
                    sof.printStackTrace();
                }
            }

            // Move input file to the done directory
            inputFile.renameTo(new File(doneDir.getPath() + File.separator
                    + fileName));
        }
    }

}
