package dk.statsbiblioteket.doms.ingesters.radiotv;

/**
 * Created by abr on 06-09-16.
 */
public class Util {
    public static String domsCommenter(String filename, String action) {
        String version = Util.class.getPackage().getImplementationVersion();
        return "RadioTV Digitv Ingester ("+version+") " + action + " as part of ingest of " + filename;
    }
}
