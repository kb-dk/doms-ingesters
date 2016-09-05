package dk.statsbiblioteket.doms.ingesters.radiotv;


import org.mockito.internal.matchers.StartsWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public abstract class FolderWatcher implements Callable<Void> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Path hotFolderToScan;
    private final long timeoutInMS;
    private FolderWatcherClient client;

    public FolderWatcher(Path hotFolderToScan, long timeoutInMS, FolderWatcherClient client) {
        this.hotFolderToScan = hotFolderToScan;
        this.timeoutInMS = timeoutInMS;
        this.client = client;
    }


    public Void call() throws Exception {
        String threadName = "FolderWatcher-" + hotFolderToScan.getFileName();
        try (AutoCloseable ignored = Common.namedThread(threadName);
             final WatchService hotFolderWatcher = FileSystems.getDefault().newWatchService()) {

            log.debug("Registering a watcher for folder '{}' ",hotFolderToScan);
            hotFolderToScan.register(hotFolderWatcher, StandardWatchEventKinds.ENTRY_MODIFY,
                                     StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);

            //Handle existing files first
            //TODO signal the user that the folder is now free?
            List<Path> preFiles = Files.list(hotFolderToScan).sorted(sortOnLastModified()).collect(
                    Collectors.toList());

            if (!preFiles.isEmpty()){
                log.info("Found {} files in {}, handling these first",preFiles.size(),hotFolderToScan);
            }

            for (Path preFile : preFiles) {
                if (!shouldStop()) {
                    log.debug("Preexisting file {} was found",preFile);
                    try (AutoCloseable ignored2 = Common.namedThread(preFile.toFile().getName())) { //Trick to rename the thread and name it back
                        client.fileAdded(preFile);
                    }
                } else {
                    return null;
                }
            }
            log.info("All preexisting files in {} have been handled, so proceeding to listen for changes",hotFolderToScan);

            //Then watch for changes
            while (!shouldStop()) {  //We run until this is true or we except
                //Handle the hotfolder watcher
                final WatchKey wk = hotFolderWatcher.poll(timeoutInMS, TimeUnit.MILLISECONDS);
                if (wk == null) { //If we reached the timeout, the key is null, so go again
                    continue;
                }

                for (WatchEvent<?> event : wk.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        log.warn("Watch overflow {}", event);
                        //What to do here??
                    } else {

                        Path file = hotFolderToScan.resolve((Path) event.context());
                        try (AutoCloseable ignored2 = Common.namedThread(file.toFile().getName())) {

                            if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                                log.debug("File was added");
                                client.fileAdded(file);

                            } else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                                log.debug("File was modified");
                                client.fileModified(file);

                            } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                                log.debug("File was deleted");
                                client.fileDeleted(file);
                            }
                        }
                    }
                }
                // reset the key
                boolean valid = wk.reset();
                if (!valid) {
                    throw new RuntimeException("Key "+wk+" have been invalidated, so no more watching of folder "+hotFolderToScan);
                }
            }
        } finally {
            if (shouldStop()){
                log.debug("Stop flag set, so shutting down");
            } else {
                log.warn("Stop flag not set, but shutting down");
            }
            client.close();
        }

        return null;//To make Void returntype happy
    }


    protected Comparator<Path> sortOnLastModified() {
        return (Path o1, Path o2) -> {
            try {
                return Files.getLastModifiedTime(o1).compareTo(Files.getLastModifiedTime(o2));
            } catch (IOException e) { //I h8 u2, Streams and IOExceptions
                throw new UncheckedIOException(e);
            }
        };
    }

    protected abstract boolean shouldStop();
}
