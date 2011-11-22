package dk.statsbiblioteket.doms.ingesters.ffprobeWrapper;


import dk.statsbiblioteket.doms.client.DomsWSClient;
import dk.statsbiblioteket.doms.client.DomsWSClientImpl;
import dk.statsbiblioteket.doms.client.NoObjectFound;
import dk.statsbiblioteket.doms.client.ServerOperationFailed;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: eab
 * Date: 11/22/11
 * Time: 9:12 AM
 * To change this template use File | Settings | File Templates.
 */
public class Ingester {

    public static void main(String[] args) {
        new Ingester().mainInstance(args);
    }

    public static String URLPrefix = "http://bitfinder.statsbiblioteket.dk/bart/";

    private void mainInstance(String[] args){
        if (args.length != 2){
            System.err.println("Ingester takes exactly two arguments");
            System.exit(128);
        }

        File inDir = new File(args[0]);
        File doneDir = new File(args[1]);
        File[] inFiles = inDir.listFiles();
        DomsWSClient dClient = new DomsWSClientImpl();
        try {
            dClient.setCredentials(
                    new URL("http://alhena:7880/centralWebservice-service/central/?wsdl"),
                    "fedoraAdmin", "fedoraAdminPass");
        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
        Document outputDoc = null;
        String PID = "";
        try {
            docBuilder = docBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            System.err.println("Failed to create docBuilder");
            System.exit(64);
        }

        for (File file : inFiles) {
            String fileName = file.getName();
            String originalFileName = fileName.substring(0,fileName.length()-4);
            URL url = null;
            String publishMessage = "all done injecting ffprobe metadata";
            try {
                url = new URL(URLPrefix+originalFileName);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                System.err.println("Url malformed using filename: " +
                        originalFileName);
                continue;
            }
            try {
                outputDoc = docBuilder.parse(file);
            } catch (SAXException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                continue;
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                continue;
            }
            try {
                PID = dClient.getFileObjectPID(url);
                dClient.unpublishObjects("Unpublishing in order to inject " +
                        "ffprobe metadata", PID);
            } catch (NoObjectFound noObjectFound) {
                noObjectFound.printStackTrace();
                continue;
            } catch (ServerOperationFailed serverOperationFailed) {
                serverOperationFailed.printStackTrace();
                System.err.println("Failed un-publishing pid: " + PID);
                continue;
            }
            try{
                dClient.updateDataStream(PID, "FFPROBE", outputDoc,
                        "injected the ffprobe metadata");
            }
            catch (ServerOperationFailed serverOperationFailed) {
                System.err.println("datastream update failed");
                publishMessage = "modifications failes, republishing the document";
                serverOperationFailed.printStackTrace();
            }
            try{
                dClient.publishObjects(publishMessage,
                        PID);
            } catch (ServerOperationFailed serverOperationFailed) {
                serverOperationFailed.printStackTrace();
                // Re try
                try{
                    dClient.publishObjects(publishMessage,
                            PID);
                } catch (ServerOperationFailed sof) {
                    sof.printStackTrace();
                }
            }
            // move to the done folder
            file.renameTo(new File(doneDir.getPath()+File.separator+fileName));

        }
    }

}
