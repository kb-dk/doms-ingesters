package dk.statsbiblioteket.doms.folderwatching;

import java.io.Closeable;
import java.nio.file.Path;

/**
 * Clients extending this class can be registered to receive events when files in a given folder are created, modified
 * or deleted.
 *
 * Important: Implementations must be thread safe
 * <br/>
 * Important. When the system shuts down, it interrupts all currently working clients. It then waits a indefinately
 * for the threads to shutdown. You implementation should not use interruptable methods, if you want to
 * be sure to run to the end. Beware that indefinately might not be so very long in practice, so be done quickly.
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
