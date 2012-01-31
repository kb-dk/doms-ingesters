package dk.statsbiblioteket.doms.ingesters.handleRegistrar;

import java.io.File;

/**
 * Tool, that given an RDF query that returns a list of objects, an URL
 * pattern and a configuration file registers all objects matching the RDF query
 * as handles resolving to the URL pattern.
 */
public class HandleRegistrarTool {
    public static void main(String[] args) {
        BasicHandleRegistrar registrar = new BasicHandleRegistrar(
                new PropertyBasedRegistrarConfiguration(new File(args[2])));
        registrar.addHandles(args[0], args[1]);
    }
}
