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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static dk.statsbiblioteket.doms.ingesters.radiotv.Named.nameThread;


public class FolderWatcher implements Callable<Void> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Path folderToWatch;
    private final long timeoutInMS;
    private final FolderWatcherClient client;
    private final int threadPoolSize;
    private Path stopFolder;
    private boolean closed = false;

    public FolderWatcher(Path folderToWatch, long timeoutInMS, FolderWatcherClient client, int threadPoolSize, Path stopFolder) {
        this.folderToWatch = folderToWatch;
        this.timeoutInMS = timeoutInMS;
        this.client = client;
        this.threadPoolSize = threadPoolSize;
        this.stopFolder = stopFolder;
    }

    public static ExecutorService daemonPool(int threads){
        ExecutorService executorService = Executors.newFixedThreadPool(threads,
                                                                    r -> { //Why cant these threads be configured as daemons????
                                                                        Thread t = new Thread(r);
                                                                        t.setDaemon(true);
                                                                        return t;
                                                                    });
        return executorService;
    }


    public Void call() throws IOException, InterruptedException {
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

                Map<Path,Callable<Path>> scheduledEvents = new HashMap<>();

                for (WatchEvent<?> event : wk.pollEvents()) {
                    shouldStopNow(); //Check stop for each event, as there can be quite a lot of events in the queue

                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        log.warn("Watch overflow {}, so resyncing contents of folder {} and restarting watches", event,
                                 folderToWatch);
                        resolveEvents(scheduledEvents.values()); //Resolve events gathered so far
                        syncWithFolderContents(client); //Then sync the files in the folder
                        wk.reset(); //When we get an Overflow, the rest of the pollEvents should not be meaningful, so reset the key and listen again
                        continue watcherLoop; //And leave this loop to get a new key
                    } else {
                        //Collect the events in a list, for eventual submission to a thread pool
                        Path file = folderToWatch.resolve((Path) event.context());
                        Callable<Path> handler = () -> {
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
                            return file;
                        };
                        //Scheduled events is a map, so if several events consern the same file, we overwrite the value
                        //This way, we only get the latest. This is relevant as the multithreading does not guarantee order
                        scheduledEvents.put(file,handler);
                    }
                }

                //Submit all the events to the executor
                resolveEvents(scheduledEvents.values());

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

    protected void resolveEvents(Collection<Callable<Path>> scheduledEvents) throws StoppedException{
        if (scheduledEvents.isEmpty()){//Short circuit to avoid unnessesary pool creation
            return;
        }
        ExecutorService pool = daemonPool(threadPoolSize); //Why can this not be autoclosable??
        try {
            //Submit all events to the pool
            List<Future<Path>> futures = scheduledEvents.stream().map(pool::submit).collect(Collectors.toList());

            //Funky construct. The point is to check stop flag periodically, while waiting for results
            //For each scheduled event (file added)
            for (Future<Path> future : futures) {
                Path result = null;
                do { //Do this (get the result) until you actually get the result.
                    try {
                        shouldStopNow(); //Check before waiting
                        result = future.get(timeoutInMS, TimeUnit.MILLISECONDS);
                    } catch (TimeoutException e) { //If we timeout while waiting for the result, check stop
                        shouldStopNow(); //Check after timeout ran out.
                    } catch (ExecutionException e) { //New runtime exception to stop all the original exception
                        throw new RuntimeException(e.getCause());
                    } catch (InterruptedException e) { //Just die here
                        throw new RuntimeException(e);
                    }
                } while (result == null); //This do while runs until we except or get a result
            }
        } finally {
            pool.shutdownNow();
            //TODO better shutdown?
        }


    }

    private void syncWithFolderContents(FolderWatcherClient client) throws IOException {
        List<Path> preFiles = Files.list(folderToWatch).sorted(sortOnLastModified()).collect(Collectors.toList());
        if (!preFiles.isEmpty()){
            log.info("Found {} files in {}, handling these", preFiles.size(), folderToWatch);
        }

        Map<Path,Callable<Path>> scheduledEvents = new HashMap<>();
        for (Path preFile : preFiles) {
            Callable<Path> handler = () -> {
                try (Named ignored = Named.nameThread(preFile)) { //Trick to rename the thread and name it back
                    log.debug("file was found");
                    client.fileAdded(preFile);
                }
                return preFile;
            };
            scheduledEvents.put(preFile,handler);
        }
        resolveEvents(scheduledEvents.values());

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

    protected void shouldStopNow() throws StoppedException {
        if (isClosed() || Files.exists(stopFolder.resolve("stoprunning"))) {
            throw new StoppedException();
        }
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }


    /**
     * Throwing this method signals that an overly shutdown have been requested
     */
    protected class StoppedException extends RuntimeException {
    }

}
