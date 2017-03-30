package dk.statsbiblioteket.doms.ingesters.radiotv;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * Tests of the method {@link RadioTVFolderWatcherClient#isAlreadyHandled} method.
 * We want to ensure that meaningful changes are not falsely ignored, while meaningless changes are ignored.
 * @see RadioTVFolderWatcherClient#isAlreadyHandled(Path)
 */
public class XmlComparisonTest {
    @Test
    public void isAlreadyHandledSimple() throws Exception {
        Path processedFilesFolder = Files.createTempDirectory("processedFilesFolder");
        Path hotFolder = Files.createTempDirectory("hotFolder");

        //register them for cleanup
        Files.deleteIfExists(processedFilesFolder);
        Files.deleteIfExists(hotFolder);

        Path file = hotFolder.resolve("file");
        try (InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("2012-11-15_09-40-00_dr1.xml")) {
            File file1 = processedFilesFolder.resolve("file").toFile();

            FileUtils.copyInputStreamToFile(resourceAsStream, file1);
        }
        try (InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("2012-11-15_09-40-00_dr1.xml")) {
            FileUtils.copyInputStreamToFile(resourceAsStream, file.toFile());
        }

        RadioTVFolderWatcherClient client = new RadioTVFolderWatcherClient(null, null, processedFilesFolder, null, false);
        assertTrue(client.isAlreadyHandled(file));
    }

    @Test
    public void isAlreadyHandledPrefixChange() throws Exception {
        Path processedFilesFolder = Files.createTempDirectory("processedFilesFolder");
        Path hotFolder = Files.createTempDirectory("hotFolder");

        //register them for cleanup
        Files.deleteIfExists(processedFilesFolder);
        Files.deleteIfExists(hotFolder);


        try (InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("2012-11-15_09-40-00_dr1.xml")) {
            File file1 = processedFilesFolder.resolve("file").toFile();

            FileUtils.copyInputStreamToFile(resourceAsStream, file1);
        }
        Path file = hotFolder.resolve("file");
        try (InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("2012-11-15_09-40-00_dr1-changedPrefix.xml")) {
            FileUtils.copyInputStreamToFile(resourceAsStream, file.toFile());
        }

        RadioTVFolderWatcherClient client = new RadioTVFolderWatcherClient(null, null, processedFilesFolder, null, false);
        assertTrue(client.isAlreadyHandled(file));
    }


    @Test
    public void isAlreadyHandledNamespaceChange() throws Exception {
        Path processedFilesFolder = Files.createTempDirectory("processedFilesFolder");
        Path hotFolder = Files.createTempDirectory("hotFolder");

        //register them for cleanup
        Files.deleteIfExists(processedFilesFolder);
        Files.deleteIfExists(hotFolder);


        try (InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("2012-11-15_09-40-00_dr1.xml")) {
            File file1 = processedFilesFolder.resolve("file").toFile();

            FileUtils.copyInputStreamToFile(resourceAsStream, file1);
        }
        Path file = hotFolder.resolve("file");
        try (InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("2012-11-15_09-40-00_dr1-changedNamespace.xml")) {
            FileUtils.copyInputStreamToFile(resourceAsStream, file.toFile());
        }

        RadioTVFolderWatcherClient client = new RadioTVFolderWatcherClient(null, null, processedFilesFolder, null, false);
        assertFalse(client.isAlreadyHandled(file));
    }



    @Test
    public void isAlreadyHandledUnimportantWhitespace() throws Exception {
        Path processedFilesFolder = Files.createTempDirectory("processedFilesFolder");
        Path hotFolder = Files.createTempDirectory("hotFolder");

        //register them for cleanup
        Files.deleteIfExists(processedFilesFolder);
        Files.deleteIfExists(hotFolder);


        try (InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("2012-11-15_09-40-00_dr1.xml")) {
            File file1 = processedFilesFolder.resolve("file").toFile();

            FileUtils.copyInputStreamToFile(resourceAsStream, file1);
        }
        Path file = hotFolder.resolve("file");
        try (InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("2012-11-15_09-40-00_dr1-unimportantWhitespace.xml")) {
            FileUtils.copyInputStreamToFile(resourceAsStream, file.toFile());
        }

        RadioTVFolderWatcherClient client = new RadioTVFolderWatcherClient(null, null, processedFilesFolder, null, false);
        assertTrue(client.isAlreadyHandled(file));
    }


    @Test
    public void isAlreadyHandledImportantWhitespace() throws Exception {
        Path processedFilesFolder = Files.createTempDirectory("processedFilesFolder");
        Path hotFolder = Files.createTempDirectory("hotFolder");

        //register them for cleanup
        Files.deleteIfExists(processedFilesFolder);
        Files.deleteIfExists(hotFolder);


        try (InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("2012-11-15_09-40-00_dr1.xml")) {
            File file1 = processedFilesFolder.resolve("file").toFile();

            FileUtils.copyInputStreamToFile(resourceAsStream, file1);
        }
        Path file = hotFolder.resolve("file");
        try (InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("2012-11-15_09-40-00_dr1-importantWhitespace.xml")) {
            FileUtils.copyInputStreamToFile(resourceAsStream, file.toFile());
        }

        RadioTVFolderWatcherClient client = new RadioTVFolderWatcherClient(null, null, processedFilesFolder, null, false);
        assertFalse(client.isAlreadyHandled(file));
    }

}