package dk.statsbiblioteket.doms.ingesters.ffprobeWrapper.ffprobeRunner;


import org.xml.sax.SAXException;

import java.io.*;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: eab
 * Date: 11/17/11
 * Time: 10:37 AM
 *
 * This class asumes that ffprobe is available on the systems <code>PATH</code>
 * further more it must be compiled using the patch(1) enabling XML output.
 *
 * Please note that all this stuff is at best experimental.
 *
 * 1 patch: http://cache.gmane.org//gmane/comp/video/ffmpeg/devel/136199-001.bin
 * patch thread: http://thread.gmane.org/gmane.comp.video.ffmpeg.devel/135857/focus=136199
 */
public class FFProbeAnalyzer extends Thread{

    String fileName = null;
    CallBackEventHandler cbHandler;
    private Properties prop;

    /**
     * This class is responsible for wrapping the ffprobe tool and calling the
     * callee method in the calling object.
     */
    public FFProbeAnalyzer(CallBackEventHandler cbHandler, String fileName)
    {
        this.cbHandler = cbHandler;
        this.fileName = fileName;
        prop = new Properties();
        String configFileName = "technicalMetadataInjector.config";
        try {
            InputStream is = new FileInputStream(configFileName);
            prop.load(is);
        } catch (IOException e) {
            System.err.println("You must supply a file named '"+ configFileName
                    +"' containing: 'ffprobe.options'");
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void run() {
        String xmlLines = null;
        InputStream xmlInputStream = null;
        String errorLines = null;
        try {
            Runtime rt = Runtime.getRuntime();

            String executionString = prop.getProperty("ffprobe.options")+fileName;

            Process pr = rt.exec(executionString);

            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            BufferedReader errorIn = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
            String tmp = null;
            while((tmp = input.readLine()) != null) {
                xmlLines = xmlLines.concat(tmp);
            }
            xmlInputStream =  new ByteArrayInputStream(
                    xmlLines.getBytes());
            tmp = null;
            while((tmp = errorIn.readLine()) != null){
                errorLines = errorLines.concat(tmp);
            }

            cbHandler.incommingEvent(fileName, xmlInputStream, errorLines, null);


        } catch(Exception e) {
            try {
                cbHandler.incommingEvent(fileName, xmlInputStream, errorLines, e);
            } catch (IOException e1) {
            } catch (SAXException e1) {
            }
            // Errors are returned to the callee, and must be handled there.
        }
    }


}
