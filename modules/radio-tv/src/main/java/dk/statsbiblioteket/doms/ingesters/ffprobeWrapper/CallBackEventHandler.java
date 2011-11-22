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
    public void incommingEvent(String fileName, InputStream analysisResult,
                               String analysisErrors, Exception error)
            throws IOException, SAXException;
}
