package dk.statsbiblioteket.doms.ingesters.radiotv;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by abr on 26-08-16.
 */
public class HotFolderScanner {


    /**
     * This flag is the bridge that allows the stopFolderWatcher to communicate "stop" to the primaryFolderWatcher
     */
    private boolean stopFlagSet = false;

    private final ExecutorService watchersPool;
    private final int timeout;
    private Future<Void> stopFolderStopped;
    private Future<Void> primaryFolderStopped;


    public HotFolderScanner() {
        timeout = 1000;
        watchersPool = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });

    }

    /**
     * Start a continuous scanning of the hot folder specified by
     * <code>folderToScan</code> and report any file creations, modifications
     * and deletions to the <code>client</code>.
     *
     * @param folderToScan Full file path to the directory to scan.
     * @param client          Reference to the client to report changes to.
     * @param stopFolder      Full file path to the stop directory.
     */
    public void startScanning(Path folderToScan, Path stopFolder,
                              FolderWatcherClient client) throws IOException {


        //Now the watchers have been made
        FolderWatcher primaryFolderWatcher = setupPrimaryFolderWatcher(folderToScan, client, timeout
        );
        FolderWatcher stopFolderWatcher = setupStopFolderWatcher(stopFolder, timeout);


        stopFolderStopped = watchersPool.submit(stopFolderWatcher);

        primaryFolderStopped = watchersPool.submit(primaryFolderWatcher);

        watchersPool.shutdown();//This just marks the pool as closed for further submission
    }

    public void waitForStop() throws InterruptedException {
        try { //Wait for the two threads to complete
            stopFolderStopped.get();
            primaryFolderStopped.get();
        } catch (ExecutionException e) {
            throw new RuntimeException();
        }
    }


    private FolderWatcher setupPrimaryFolderWatcher(final Path folderToScan, final FolderWatcherClient client, final long timeout) {
        return new FolderWatcher(folderToScan, timeout, client) {
                @Override
                protected boolean shouldStop() {
                    return isStopFlagSet();
                }
            };
    }

    private FolderWatcher setupStopFolderWatcher(final Path folderToScan, final long timeout) {
        FolderWatcherClient stopFolderWacherClient = new FolderWatcherClient() {
            @Override
            public void fileAdded(Path addedFile) {
                if (Files.isRegularFile(addedFile)) {
                    if (addedFile.getFileName().endsWith("stoprunning")) {
                        setStopFlagSet(true);
                    }
                }
            }
        };
        return new FolderWatcher(folderToScan, timeout, stopFolderWacherClient){
            @Override
            protected boolean shouldStop() {
                return isStopFlagSet();
            }
        };
    }

    public synchronized boolean isStopFlagSet() {
        return stopFlagSet;
    }

    public void setStopFlagSet(boolean stopFlagSet) {
        this.stopFlagSet = stopFlagSet;
    }
}
