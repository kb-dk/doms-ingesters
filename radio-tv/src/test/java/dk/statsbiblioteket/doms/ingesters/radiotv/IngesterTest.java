package dk.statsbiblioteket.doms.ingesters.radiotv;

import org.apache.commons.cli.CommandLine;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * Created by abr on 05-09-16.
 */
public class IngesterTest {
    @Test
    public void testSetupCommandLineRealArguments() throws Exception {

        String commandLine = "-hotfolder=$HOTFOLDER -lukefolder=$LUKEFOLDER -coldfolder=$COLDFOLDER -stopfolder=$STOPFOLDER -wsdl=http://wsdl.net -username=$USERNAME -password=$PASSWORD  -preingestschema=$SCHEMA -overwrite=false";
        CommandLine parsedArgs = Ingester.setupCommandLine(commandLine.split(" +"));

        assertEquals(Ingester.parseHotfolder(parsedArgs), Paths.get("$HOTFOLDER"));
        assertEquals(Ingester.parseLukefolder(parsedArgs),Paths.get("$LUKEFOLDER"));
        assertEquals(Ingester.parseColdfolder(parsedArgs),Paths.get("$COLDFOLDER"));
        assertEquals(Ingester.parseStopfolder(parsedArgs),Paths.get("$STOPFOLDER"));
        assertEquals(Ingester.parseWSDL(parsedArgs), new URL("http://wsdl.net"));
        assertEquals(Ingester.parseUsername(parsedArgs),"$USERNAME");
        assertEquals(Ingester.parsePassword(parsedArgs),"$PASSWORD");
        assertEquals(Ingester.parseSchema(parsedArgs),Paths.get("$SCHEMA"));
        assertEquals(Ingester.parseOverwrite(parsedArgs),false);
    }

    @Test
    public void testSetupCommandLineDefaults() throws Exception {

        String commandLine = "-wsdl=http://wsdl.net";
        CommandLine parsedArgs = Ingester.setupCommandLine(commandLine.split(" +"));

        assertEquals(Ingester.parseHotfolder(parsedArgs), Paths.get("radioTVMetaData"));
        assertEquals(Ingester.parseLukefolder(parsedArgs),Paths.get("/tmp/failedFiles"));
        assertEquals(Ingester.parseColdfolder(parsedArgs),Paths.get("processedFiles"));
        assertEquals(Ingester.parseStopfolder(parsedArgs),Paths.get("stopFolder"));

        assertEquals(Ingester.parseWSDL(parsedArgs), new URL("http://wsdl.net"));
        assertEquals(Ingester.parseUsername(parsedArgs),"fedoraAdmin");
        assertEquals(Ingester.parsePassword(parsedArgs),"fedoraAdminPass");

        Path defaultSchema = Paths.get(getClass().getClassLoader().getResource("").getPath()).resolveSibling("classes/exportedRadioTVProgram.xsd");
        assertEquals(Ingester.parseSchema(parsedArgs), defaultSchema);

        assertEquals(Ingester.parseOverwrite(parsedArgs),false);
    }


}