package dk.statsbiblioteket.doms.ingesters.radiotv;


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

import static dk.statsbiblioteket.doms.ingesters.radiotv.Named.nameThread;


public abstract class FolderWatcher implements Callable<Void> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Path folderToWatch;
    private final long timeoutInMS;
    private final FolderWatcherClient client;

    public FolderWatcher(Path folderToWatch, long timeoutInMS, FolderWatcherClient client) {
        this.folderToWatch = folderToWatch;
        this.timeoutInMS = timeoutInMS;
        this.client = client;
    }


    public Void call() throws Exception {
        try (Named threadNamer = nameThread("FolderWatcher-" + folderToWatch.getFileName());
             FolderWatcherClient client = this.client; //Trick to autoclose the client when done
             WatchService watchService = FileSystems.getDefault().newWatchService()) {

            log.debug("Registering a watcher for folder '{}' ", folderToWatch);
            folderToWatch.register(watchService,
                                   StandardWatchEventKinds.ENTRY_MODIFY,
                                   StandardWatchEventKinds.ENTRY_CREATE,
                                   StandardWatchEventKinds.ENTRY_DELETE);

            //Handle existing files first
            syncWithFolderContents(client);

            //Then watch for changes
            watcherLoop:
            while (true) {
                //We run until this throws stoppedException
                shouldStopNow();

                //Handle the hotfolder watcher
                final WatchKey wk = watchService.poll(timeoutInMS, TimeUnit.MILLISECONDS);
                if (wk == null) { //If we reached the timeout, the key is null, so go again
                    continue;
                }

                for (WatchEvent<?> event : wk.pollEvents()) {
                    shouldStopNow(); //Check stop for each event, as there can be quite a lot of events in the queue

                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        log.warn("Watch overflow {}, so resyncing contents of folder {} and restarting watches", event,
                                 folderToWatch);
                        syncWithFolderContents(client);
                        wk.reset(); //When we get an Overflow, the rest of the pollEvents should not be meaningful, so reset the key and listen again
                        continue watcherLoop;
                    } else {

                        Path file = folderToWatch.resolve((Path) event.context());
                        try (Named ignored2 = nameThread(file)) {

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
                    throw new RuntimeException(
                            "Key " + wk + " has been invalidated, so no more watching of folder " +
                            folderToWatch);
                }
            }

        } catch (StoppedException e) { //Stopped exception stops here
            log.debug("Stop flag set, so shutting down");
            return null;
        }
    }

    private void syncWithFolderContents(FolderWatcherClient client) throws Exception {
        List<Path> preFiles = Files.list(folderToWatch).sorted(sortOnLastModified()).collect(Collectors.toList());
        if (!preFiles.isEmpty()){
            log.info("Found {} files in {}, handling these", preFiles.size(), folderToWatch);
        }

        for (Path preFile : preFiles) {
            shouldStopNow();
            try (Named ignored = nameThread(preFile)) { //Trick to rename the thread and name it back
                log.debug("file was found");
                client.fileAdded(preFile);
            }
        }
        log.info("All files in {} have been handled, so proceeding to listen for changes",
                 folderToWatch);
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

    protected abstract void shouldStopNow() throws StoppedException;



    /**
     * Throwing this method signals that an overly shutdown have been requested
     */
    protected class StoppedException extends RuntimeException {
    }

}
