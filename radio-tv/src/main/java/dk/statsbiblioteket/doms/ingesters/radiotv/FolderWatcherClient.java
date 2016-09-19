package dk.statsbiblioteket.doms.ingesters.radiotv;

import java.io.Closeable;
import java.nio.file.Path;

/**
 * Clients extending this class can be registered to receive events when files in a given folder are created, modified
 * or deleted.
 */
public abstract class FolderWatcherClient implements Closeable{
    //abstract class over interface so you do not have to implement everything

    /**
     * Called when a file is added to the watched folder
     * @param addedFile the added file
     * @throws Exception if handling this file failed
     */
    public void fileAdded(Path addedFile) throws Exception {

    }

    /**
     * Called when a file in the watched folder is modified
     * @param modifiedFile the modified file
     * @throws Exception
     */
    public void fileModified(Path modifiedFile) throws Exception {

    }

    /**
     * Called when a file in the watched folder is deleted
     * @param deletedFile the deleted file (which do not currently exists)
     * @throws Exception if handling this file failed
     */
    public void fileDeleted(Path deletedFile) throws Exception {

    }

    /**
     * Will be invoked before the folderwatcher is finished. Implementers should use this method to close any
     * acquired resources
     */
    public void close() {

    }

}
