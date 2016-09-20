package dk.statsbiblioteket.doms.ingesters.radiotv;

import org.apache.commons.cli.CommandLine;
import org.junit.After;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * Created by abr on 05-09-16.
 */
public class IngesterTest {
    @Test
    public void testSetupCommandLineRealArguments() throws Exception {

        //These paths will get made in the test
        Path $HOTFOLDER = Paths.get("$HOTFOLDER");
        Path $LUKEFOLDER = Paths.get("$LUKEFOLDER");
        Path $COLDFOLDER = Paths.get("$COLDFOLDER");
        Path $STOPFOLDER = Paths.get("$STOPFOLDER");

        //register them for cleanup
        $HOTFOLDER.toFile().deleteOnExit();
        $LUKEFOLDER.toFile().deleteOnExit();
        $COLDFOLDER.toFile().deleteOnExit();
        $STOPFOLDER.toFile().deleteOnExit();

        String commandLine = "-hotfolder=$HOTFOLDER -lukefolder=$LUKEFOLDER -coldfolder=$COLDFOLDER -stopfolder=$STOPFOLDER -wsdl=http://wsdl.net -username=$USERNAME -password=$PASSWORD  -preingestschema=$SCHEMA -overwrite=false -numthreads=5 -threadwaittime=1200";
        CommandLine parsedArgs = Ingester.setupCommandLine(commandLine.split(" +"));

        assertEquals(Ingester.parseHotfolder(parsedArgs), $HOTFOLDER);
        assertEquals(Ingester.parseLukefolder(parsedArgs), $LUKEFOLDER);
        assertEquals(Ingester.parseColdfolder(parsedArgs), $COLDFOLDER);
        assertEquals(Ingester.parseStopfolder(parsedArgs), $STOPFOLDER);

        assertEquals(Ingester.parseWSDL(parsedArgs), new URL("http://wsdl.net"));
        assertEquals(Ingester.parseUsername(parsedArgs),"$USERNAME");
        assertEquals(Ingester.parsePassword(parsedArgs),"$PASSWORD");

        assertEquals(Ingester.parseSchema(parsedArgs),Paths.get("$SCHEMA"));
        assertEquals(Ingester.parseOverwrite(parsedArgs),false);

        assertEquals(Ingester.parseNumThreads(parsedArgs),5);
        assertEquals(Ingester.parseThreadWaitTime(parsedArgs),1200);
    }

    @Test
    public void testSetupCommandLineDefaults() throws Exception {
        //These paths will get made in the test
        Path radioTVMetaData = Paths.get("radioTVMetaData");
        Path failedFiles = Paths.get("/tmp/failedFiles");
        Path processedFiles = Paths.get("processedFiles");
        Path stopFolder = Paths.get("stopFolder");

        //Register them for cleanup
        radioTVMetaData.toFile().deleteOnExit();
        failedFiles.toFile().deleteOnExit();
        processedFiles.toFile().deleteOnExit();
        stopFolder.toFile().deleteOnExit();

        String commandLine = "-wsdl=http://wsdl.net";
        CommandLine parsedArgs = Ingester.setupCommandLine(commandLine.split(" +"));

        assertEquals(Ingester.parseHotfolder(parsedArgs), radioTVMetaData);
        assertEquals(Ingester.parseLukefolder(parsedArgs), failedFiles);
        assertEquals(Ingester.parseColdfolder(parsedArgs), processedFiles);
        assertEquals(Ingester.parseStopfolder(parsedArgs), stopFolder);

        assertEquals(Ingester.parseWSDL(parsedArgs), new URL("http://wsdl.net"));
        assertEquals(Ingester.parseUsername(parsedArgs),"fedoraAdmin");
        assertEquals(Ingester.parsePassword(parsedArgs),"fedoraAdminPass");

        Path defaultSchema = Paths.get(getClass().getClassLoader().getResource("").getPath()).resolveSibling("classes/exportedRadioTVProgram.xsd");
        assertEquals(Ingester.parseSchema(parsedArgs), defaultSchema);
        assertTrue(Files.isReadable(defaultSchema));

        assertEquals(Ingester.parseOverwrite(parsedArgs),false);

        assertEquals(Ingester.parseNumThreads(parsedArgs),4);
        assertEquals(Ingester.parseThreadWaitTime(parsedArgs),1000);
    }

}