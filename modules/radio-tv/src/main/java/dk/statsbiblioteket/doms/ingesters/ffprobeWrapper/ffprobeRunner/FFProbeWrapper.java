package dk.statsbiblioteket.doms.ingesters.ffprobeWrapper.ffprobeRunner;

/**
 * Created by IntelliJ IDEA.
 * User: eab
 * Date: 11/22/11
 * Time: 8:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class FFProbeWrapper {
    public static void main (String[] args) {
        String ffprobeArgs = "";

        if (args.length == 0) {
            System.exit(128);
        }

        if (args.length > 1) {
            for (int i = 0; i < args.length - 2; i++)
                ffprobeArgs += args[i] + " ";
        }
        
    }
}
