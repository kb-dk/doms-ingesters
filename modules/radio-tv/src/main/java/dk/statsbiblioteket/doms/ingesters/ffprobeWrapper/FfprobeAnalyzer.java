package dk.statsbiblioteket.doms.ingesters.ffprobeWrapper;


import org.xml.sax.SAXException;

import java.io.*;

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
public class FfprobeAnalyzer extends Thread{

    String fileName = null;
    CallBackEventHandler cbHandler;

    /**
     * This 
     */
    public FfprobeAnalyzer(CallBackEventHandler cbHandler, String fileName)
    {
        this.cbHandler = cbHandler;
        this.fileName = fileName;
    }

    public void run() {
        System.out.println("Hello from a thread!");
        String xmlLines = null;
        InputStream xmlInputStream = null;
        String errorLines = null;
        try {
            Runtime rt = Runtime.getRuntime();
            //Process pr = rt.exec("cmd /c dir");

            String executionString = "ffprobe -show_format -show_streams -print_format xml -i file:"+fileName;

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

//            int exitVal = pr.waitFor();
//            System.out.println("Exited with error code "+exitVal);

        } catch(Exception e) {
            try {
                cbHandler.incommingEvent(fileName, xmlInputStream, errorLines, e);
            } catch (IOException e1) {
                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (SAXException e1) {
                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            // Errors are returned to the callee, and must be handled there.
        }
    }


}
