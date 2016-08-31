package dk.statsbiblioteket.doms.ingesters.radiotv;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by abr on 31-08-16.
 */
public abstract class FolderWatcherClient implements Closeable{

    public void fileAdded(Path addedFile) throws IOException {

    }

    public void fileModified(Path modifiedFile) throws IOException {

    }

    public void fileDeleted(Path deletedFile) throws IOException {

    }

    public void close(){

    }

}
