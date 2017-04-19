package dk.statsbiblioteket.doms.ingesters.radiotv;

import org.apache.commons.cli.CommandLine;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by abr on 05-09-16.
 */
public class IngesterTest {
    @Test
    public void testSetupCommandLineRealArguments() throws Exception {

        Path coldFolder = Files.createTempDirectory("coldFolder").toAbsolutePath();
        Path hotFolder = Files.createTempDirectory("hotFolder").toAbsolutePath();
        Path lukeFolder = Files.createTempDirectory("lukeFolder").toAbsolutePath();
        Path stopFolder = Files.createTempDirectory("stopFolder").toAbsolutePath();

        //register them for cleanup
        coldFolder.toFile().deleteOnExit();
        hotFolder.toFile().deleteOnExit();
        lukeFolder.toFile().deleteOnExit();
        stopFolder.toFile().deleteOnExit();

        String commandLine = MessageFormat.format(
                "-hotfolder={0} -lukefolder={1} -coldfolder={2} -stopfolder={3} -wsdl=http://wsdl.net -username=$USERNAME -password=$PASSWORD  -preingestschema=$SCHEMA -overwrite=false -numthreads=5 -threadwaittime=1200 -maxFails=8",
                hotFolder, lukeFolder, coldFolder, stopFolder);

        CommandLine parsedArgs = Ingester.setupCommandLine(commandLine.split(" +"));

        assertEquals(Ingester.parseHotfolder(parsedArgs), hotFolder);
        assertEquals(Ingester.parseLukefolder(parsedArgs), lukeFolder);
        assertEquals(Ingester.parseColdfolder(parsedArgs), coldFolder);
        assertEquals(Ingester.parseStopfolder(parsedArgs), stopFolder);

        assertEquals(Ingester.parseWSDL(parsedArgs), new URL("http://wsdl.net"));
        assertEquals(Ingester.parseUsername(parsedArgs),"$USERNAME");
        assertEquals(Ingester.parsePassword(parsedArgs),"$PASSWORD");

        assertEquals(Ingester.parseSchema(parsedArgs),Paths.get("$SCHEMA"));
        assertEquals(Ingester.parseOverwrite(parsedArgs),false);

        assertEquals(Ingester.parseNumThreads(parsedArgs),5);
        assertEquals(Ingester.parseThreadWaitTime(parsedArgs),1200);
        assertEquals(Ingester.parseMaxFails(parsedArgs),8);
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
        assertEquals(Ingester.parseMaxFails(parsedArgs),10);
    }

}