package dk.statsbiblioteket.doms.ingesters.radiotv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Named implements AutoClosable but without throwing exceptions. Is used for renaming threads inside try-with-resources
 * blocks
 * @see #nameThread(String)
 */
public interface Named extends AutoCloseable {
    //private static final Logger log = LoggerFactory.getLogger(Named.class);

    @Override
    void close();

    /**
     * A Trick to rename the thread for the duration of a try statement and get it automatically named back when closed
     *
     * use it like this
     * <pre>
      try (Named ignored = nameThread(name)) {
         //Do stuff
      }
     * </pre>
     *
     * Works by returning a Named, which extends AutoClosable. The close method is implemented to set the thread name back
     * to what it was before.
     * @param name the name to apped to the thread name
     * @return an autoclosable instance for resetting the thread name
     */
    static Named nameThread(String name) { //Trick to rename the thread and name it back
        String oldName = Thread.currentThread().getName();
        String newName = oldName + "-" + name;
        //log.debug("Starting work on {} so adapting thread name", name);
        Thread.currentThread().setName(newName);
        return () -> {
            ///log.debug("Finished work on {} so resetting thread name to {}", name, oldName);
            Thread.currentThread().setName(oldName);
        };
    }


    /**
     * Name the thread after the filename of the given path
     * @param path the path
     * @return a Named
     * @see #nameThread(String)
     */
    static Named nameThread(Path path) {
        return nameThread(path.toFile().getName());
    }
}

