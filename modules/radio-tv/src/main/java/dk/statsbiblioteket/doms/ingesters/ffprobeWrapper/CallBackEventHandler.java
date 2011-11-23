package dk.statsbiblioteket.doms.ingesters.ffprobeWrapper;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA.
 * User: eab
 * Date: 11/17/11
 * Time: 1:04 PM
 * To change this template use File | Settings | File Templates.
 */
public interface CallBackEventHandler {

    /**
     * This method is a callee and it handles the results from the analysis
     * carried out by ffprobe and will recieve all relevant data from the
     * caller class wrapping ffrpobe in a threaded fasion.
     * @param fileName the file that have been analysed
     * @param analysisResult Any output returned by ffprobe
     * @param analysisErrors Any errors that occurred during analysis
     * @param error Any java exceptions returned
     * @throws IOException
     * @throws SAXException
     */
    public void incommingEvent(String fileName, InputStream analysisResult,
                               String analysisErrors, Exception error)
            throws IOException, SAXException;
}
