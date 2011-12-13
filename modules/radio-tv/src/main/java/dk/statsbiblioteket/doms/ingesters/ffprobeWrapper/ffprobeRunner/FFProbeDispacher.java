package dk.statsbiblioteket.doms.ingesters.ffprobeWrapper.ffprobeRunner;

import dk.statsbiblioteket.doms.client.DomsWSClient;
import dk.statsbiblioteket.doms.client.DomsWSClientImpl;
import dk.statsbiblioteket.doms.client.NoObjectFound;
import dk.statsbiblioteket.doms.client.ServerOperationFailed;
import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XPathSelector;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: eab
 * Date: 11/17/11
 * Time: 1:06 PM
 *
 * This class facilitates a threaded batch processing of digitalAV material
 * the results return asynchronously, and data is processed as it arrives.
 *
 * Errors are handled here in when pushed from the Analyzer.
 *
 * ~ GENERAL SEQUENCE DIAGRAM ~
 * FfprobeDispatcher:      ffd
 * FFprobeAnalyzer:        ffa
 * CallBackEventHandler:   cbh
 *   ---------------       -------
 *   | ffd  :  cbh |       | ffa |
 *   ---------------       -------
 *      |       |             |
 *     | |      |             |
 *     | |------+----------->| |
 *      |       |            | |
 *      |      | |<----------| |
 *     | |<--- | |            |
 *      |       |             |
 *
 *
 */
public class FFProbeDispacher implements CallBackEventHandler {

    private Map<String, String> files;  // maps <fileName, shardPID>
    private Map<String, String> failedFiles;  // maps <fileName, analysisError>
    private Map<String, Thread> runningThreads;  // maps <fileName, Thread>
    private XPathSelector xpathSelector;
    private DocumentBuilder docBuilder;
    private DomsWSClient dClient;
    private Properties prop = null;

    /**
     * Creates a FfprobeDispatcher object
     * @param ffprobeSchema Schema the ffprobe result adheers to
     * @param files a map of fileName: PIDs
     */
    public void FfprobeDispacher(Schema ffprobeSchema, HashMap<String, String> files){
         DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
                .newInstance();
        documentBuilderFactory.setSchema(ffprobeSchema);
        documentBuilderFactory.setNamespaceAware(true);
        dClient = new DomsWSClientImpl();
        prop = new Properties();
        String configFileName = "technicalMetadataInjector.config";
        try {
            InputStream is = new FileInputStream(configFileName);
            prop.load(is);
        } catch (IOException e) {
            System.err.println("You must supply a file named '"+ configFileName
                    +"' containing: 'bart.urlprefix', 'doms.wsdl', 'doms.user'" +
                    " & 'doms.passwod'");
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        try {
            dClient.setCredentials(
                    new URL(prop.getProperty("doms.wsdl")),
                    prop.getProperty("doms.user"),
                    prop.getProperty("doms.password"));
        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        this.files = files;
        runningThreads = new HashMap<String, Thread>();
        failedFiles = new HashMap<String, String>();
        try {
            docBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
            // TODO: Log this
            // TODO: we should not allow endless failures....
            // TODO: FATAL
            fatalException();
        }
        xpathSelector = DOM.createXPathSelector("ffprobe",
                "http://www.ffmpeg.org/schema/ffprobe");
    }

    private void fatalException() {
        System.exit(-1);
    }

    /**
     * Starts a thread per file, that calls the <code>incommingEvents</code>
     * method to handle the technical metadata.
     */
    public void startBatch(){

        while(!this.files.isEmpty()){
            for (String name : files.keySet()) {
            runningThreads.put(name, new FFProbeAnalyzer(this, name));
            runningThreads.get(name).start();
            } 
        }
    }

    /**
     * Adds files to the list of files held ready for processing.
     * @param files to add
     */
    public void addFiles(Map<String, String> files){
        this.files.putAll(files);
    }

    /**
     * Adds files and starts processing.
     * @param files to process
     */
    public void processFiles(Map<String, String> files){
        addFiles(files);
        startBatch();
    }

    /**
     * Callback routine for handeling errors and successes
     * @param fileName the file for which the results apply
     * @param analysisResult the results created in XML form
     * @param analysisErrors error text generated by the CLI tool
     * @param error errors raised by the callee 
     */
    @Override
    synchronized public void incommingEvent(String fileName,
                                            InputStream analysisResult,
                                            String analysisErrors,
                                            Exception error) throws IOException,
            SAXException {
        runningThreads.remove(fileName);
        if (error != null) {
            failedFiles.put(fileName, analysisErrors);
            error.printStackTrace();
            return;
        }
        Document ffprobeDoc = docBuilder.parse(analysisResult);
        URL url = new URL(prop.getProperty("bart.urlprefix")+"/"+fileName);
        try {
            ingest(ffprobeDoc, "FFPROBE", url);
        } catch (ServerOperationFailed serverOperationFailed) {
            serverOperationFailed.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NoObjectFound noObjectFound) {
            noObjectFound.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    /**
     * Ingests the ffprobe result
     * @param ffprobeDoc the result
     * @param dataStream Where to put the result
     * @param url of the file
     * @throws ServerOperationFailed
     * @throws NoObjectFound
     */
    private void ingest(Document ffprobeDoc, String dataStream, URL url) throws ServerOperationFailed, NoObjectFound {
        String pid = dClient.getFileObjectPID(url);
        dClient.updateDataStream(pid, dataStream, ffprobeDoc, "Injected " +
                "ffProbe data");
    }
}