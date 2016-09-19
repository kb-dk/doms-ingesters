package dk.statsbiblioteket.doms.folderwatching;


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
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static dk.statsbiblioteket.doms.folderwatching.Named.nameThread;


public class FolderWatcher implements Callable<Void> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Path folderToWatch;
    private final long timeoutInMS;
    private final FolderWatcherClient client;
    private final int threadPoolSize;
    private final Path stopFolder;
    private final ThreadFactory threadFactory;


    private boolean closed = false;

    private int filesAdded = 0;
    private int filesModified = 0;
    private int filesDeleted = 0;



    public FolderWatcher(Path folderToWatch, long timeoutInMS, FolderWatcherClient client, int threadPoolSize, Path stopFolder) {
        this.folderToWatch = folderToWatch;
        this.timeoutInMS = timeoutInMS;
        this.client = client;
        this.threadPoolSize = threadPoolSize;
        this.stopFolder = stopFolder;

        final AtomicInteger threadNumber = new AtomicInteger(1);
        final ThreadGroup threadGroup = new ThreadGroup("Worker");
        threadFactory = runnable -> {
            Thread thread = new Thread(threadGroup, runnable, threadGroup.getName()+threadNumber.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }




    public Void call() throws IOException {
        try (Named threadNamer = nameThread("Watcher-" + folderToWatch.getFileName());
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
                    log.trace("Timeout while waiting for new events, so restarting");
                    continue;
                }

                Map<Path,Callable<Path>> scheduledEvents = new TreeMap<>(); //Treemap as to keep ordering


                List<WatchEvent<?>> watchEvents = wk.pollEvents();
                log.debug("Found {} watch events",watchEvents.size());
                for (WatchEvent<?> event : watchEvents) {
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
                        Callable<Path> handler = null;

                        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                            log.debug("File {} was added. Scheduling work",file.getFileName());
                            handler = () -> {
                                try (Named threadNamer2 = nameThread(file)) {
                                    log.debug("Starting work on added file");
                                    client.fileAdded(file);
                                    incrementFilesAdded(file);
                                    log.debug("Finished work on added file");
                                }
                                return file;
                            };

                        } else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                            log.debug("File {} was modified. Scheduling work",file.getFileName());
                            handler = () -> {
                                try (Named threadNamer2 = nameThread(file)) {
                                    log.debug("Starting work on modified file");
                                    client.fileModified(file);
                                    incrementFilesModified(file);
                                    log.debug("Finished work on modified file");
                                }
                                return file;
                            };

                        } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                            log.debug("File {} was deleted. Scheduling work",file.getFileName());
                            handler = () -> {
                                try (Named threadNamer2 = nameThread(file)) {
                                    log.debug("Starting work on deleted file");
                                    client.fileDeleted(file);
                                    incrementFilesDeleted(file);
                                    log.debug("Finished work on deleted file");
                                }
                                return file;
                            };
                        }

                        //Scheduled events is a map, so if several events consern the same file, we overwrite the value
                        //This way, we only get the latest. This is relevant as the multithreading does not guarantee order
                        if (handler != null) {
                            scheduledEvents.put(file, handler);
                        }
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
                } else {
                    log.debug("Requeueing any events that have accumulated while working on this batch");
                }
            }

        } catch (StoppedException e) { //Stopped exception stops here
            log.info("Stop flag set, so attempting orderly shutdown");
            return null;
        } catch (InterruptedException e){
            log.info("Interrupted so attempting orderly shutdown");
            return null;
        }
    }

    private void incrementFilesDeleted(Path file) {
        filesAdded++;
        if (filesAdded % 100 == 0){
            log.info("{} files have now been added ({})",filesAdded,file);
        }

    }

    private void incrementFilesModified(Path file) {
        filesModified++;
        if (filesModified % 100 == 0){
            log.info("{} files have now been modified ({})",filesModified,file);
        }

    }

    private void incrementFilesAdded(Path file) {
        filesDeleted++;
        if (filesDeleted % 100 == 0){
            log.info("File nr {} have been deleted ({})",filesDeleted,file);
        }

    }

    protected void resolveEvents(Collection<Callable<Path>> scheduledEvents) throws StoppedException, InterruptedException {
        if (scheduledEvents.isEmpty()){//Short circuit to avoid unnessesary pool creation
            return;
        }
        log.debug("Preparing to resolve a batch of {} events with {} threads",scheduledEvents.size(),threadPoolSize);
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize,
                                                                       threadFactory);
        ExecutorService pool = executorService; //Why can this not be autoclosable??
        try {
            //Submit all events to the pool
            List<Future<Path>> futures = scheduledEvents.stream().map(pool::submit).collect(Collectors.toList());

            //Funky construct. The point is to check stop flag periodically, while blocking for results
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
                    }
                } while (result == null); //This do while runs until we except or get a result
            }
        } finally {
            List<Runnable> leftOvers = pool.shutdownNow();
            if (!leftOvers.isEmpty()) {
                log.warn("Shutting down execution pool. Some tasks were never started: {}", leftOvers);
            }
            pool.awaitTermination(timeoutInMS*10,TimeUnit.MILLISECONDS);
        }
    }

    private int syncWithFolderContents(FolderWatcherClient client) throws IOException, InterruptedException {
        List<Path> preFiles = Files.list(folderToWatch).sorted(sortOnLastModified()).collect(Collectors.toList());
        if (preFiles.isEmpty()){
            return 0;
        }

        log.info("Found {} preexisting files in {}, handling these", preFiles.size(), folderToWatch);
        Map<Path,Callable<Path>> scheduledEvents = new TreeMap<>();
        for (Path preFile : preFiles) {
            shouldStopNow(); //Check for each file, as this can take a while
            Callable<Path> handler = () -> {
                try (Named ignored = nameThread(preFile)) { //Trick to rename the thread and name it back
                    log.debug("Starting work on preexisting file");
                    client.fileAdded(preFile);
                    incrementFilesAdded(preFile);
                    log.debug("Finishing work on preexisting file");
                }
                return preFile;
            };
            scheduledEvents.put(preFile,handler);
        }
        resolveEvents(scheduledEvents.values());

        log.info("All preexisting files in {} have been handled, so proceeding to listen for changes",
                 folderToWatch);
        return scheduledEvents.size();
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
