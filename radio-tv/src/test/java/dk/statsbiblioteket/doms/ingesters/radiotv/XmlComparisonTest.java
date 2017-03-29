package dk.statsbiblioteket.doms.ingesters.radiotv;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * Created by abr on 29-03-17.
 */
public class XmlComparisonTest {
    @Test
    public void isAlreadyHandledSimple() throws Exception {
        //These paths will get made in the test
        Path alreadyIngested = Paths.get("alreadyIngested");
        Path newFiles = Paths.get("newFiles");

        //register them for cleanup
        alreadyIngested.toFile().deleteOnExit();
        newFiles.toFile().deleteOnExit();

        Path file = newFiles.resolve("file");
        try (InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("2012-11-15_09-40-00_dr1.xml")) {
            File file1 = alreadyIngested.resolve("file").toFile();

            FileUtils.copyInputStreamToFile(resourceAsStream, file1);
        }
        try (InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("2012-11-15_09-40-00_dr1.xml")) {
            FileUtils.copyInputStreamToFile(resourceAsStream, file.toFile());
        }

        RadioTVFolderWatcherClient client = new RadioTVFolderWatcherClient(null, null, alreadyIngested, null, false);
        assertTrue(client.isAlreadyHandled(file));
    }

    @Test
    public void isAlreadyHandledPrefixChange() throws Exception {
        //These paths will get made in the test
        Path alreadyIngested = Paths.get("alreadyIngested");
        Path newFiles = Paths.get("newFiles");

        //register them for cleanup
        alreadyIngested.toFile().deleteOnExit();
        newFiles.toFile().deleteOnExit();


        try (InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("2012-11-15_09-40-00_dr1.xml")) {
            File file1 = alreadyIngested.resolve("file").toFile();

            FileUtils.copyInputStreamToFile(resourceAsStream, file1);
        }
        Path file = newFiles.resolve("file");
        try (InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("2012-11-15_09-40-00_dr1-changedPrefix.xml")) {
            FileUtils.copyInputStreamToFile(resourceAsStream, file.toFile());
        }

        RadioTVFolderWatcherClient client = new RadioTVFolderWatcherClient(null, null, alreadyIngested, null, false);
        assertTrue(client.isAlreadyHandled(file));
    }


    @Test
    public void isAlreadyHandledNamespaceChange() throws Exception {
        //These paths will get made in the test
        Path alreadyIngested = Paths.get("alreadyIngested");
        Path newFiles = Paths.get("newFiles");

        //register them for cleanup
        alreadyIngested.toFile().deleteOnExit();
        newFiles.toFile().deleteOnExit();


        try (InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("2012-11-15_09-40-00_dr1.xml")) {
            File file1 = alreadyIngested.resolve("file").toFile();

            FileUtils.copyInputStreamToFile(resourceAsStream, file1);
        }
        Path file = newFiles.resolve("file");
        try (InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("2012-11-15_09-40-00_dr1-changedNamespace.xml")) {
            FileUtils.copyInputStreamToFile(resourceAsStream, file.toFile());
        }

        RadioTVFolderWatcherClient client = new RadioTVFolderWatcherClient(null, null, alreadyIngested, null, false);
        assertFalse(client.isAlreadyHandled(file));
    }

}