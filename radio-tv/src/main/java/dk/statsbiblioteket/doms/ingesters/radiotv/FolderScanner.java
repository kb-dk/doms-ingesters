package dk.statsbiblioteket.doms.ingesters.radiotv;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by abr on 26-08-16.
 */
public class FolderScanner implements Callable<Void>{


    /**
     * This flag is the bridge that allows the stopFolderWatcher to communicate "stop" to the primaryFolderWatcher
     */
    private boolean stopFlagSet = false;

    private final int timeout;
    private final Path folderToScan;
    private final Path stopFolder;
    private final FolderWatcherClient client;


    /**
     * Start a continuous scanning of the hot folder specified by
     * <code>folderToScan</code> and report any file creations, modifications
     * and deletions to the <code>client</code>.
     *
     * @param folderToScan Full file path to the directory to scan.
     * @param client          Reference to the client to report changes to.
     * @param stopFolder      Full file path to the stop directory.
     */
    public FolderScanner(Path folderToScan, Path stopFolder, FolderWatcherClient client) {
        timeout = 1000;
        this.folderToScan = folderToScan;
        this.stopFolder = stopFolder;
        this.client = client;
    }


    /**
     * Start a continuous scanning of the hot folder and report any file creations, modifications
     * and deletions to the client
     * @return Void, null return value
     * @throws RuntimeException if the folder watcher (client) crashes in any way. Examine the getCause field to
     * see what really went wrong
     */
    @Override
    public Void call() throws RuntimeException {
        ExecutorService watchersPool = Executors.newFixedThreadPool(2, //Exactly 2 threads
                r -> { //Why cant these threads be configured as daemons????
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    return t;
                });

        //Now the watchers have been made
        FolderWatcher stopFolderWatcher = setupStopFolderWatcher(stopFolder, timeout);

        FolderWatcher primaryFolderWatcher = setupPrimaryFolderWatcher(folderToScan, client, timeout);

        Future<Void> stopFolderStopped = watchersPool.submit(stopFolderWatcher);

        Future<Void> primaryFolderStopped = watchersPool.submit(primaryFolderWatcher);

        watchersPool.shutdown();//This just marks the pool as closed for further submission

         //Wait for the two threads to complete
        while (true) {
            try {
                primaryFolderStopped.get(timeout, TimeUnit.MILLISECONDS);
                stopFolderStopped.get(timeout, TimeUnit.MILLISECONDS); //TODO Why do we wait for this? If the primary stopped, why not just shut the whole thing down?
                break;  //if both gets complete, we can break
            } catch (TimeoutException | InterruptedException e) {
                continue;//ignore, try again
            } catch (CancellationException e){
                //Somehow one of them got cancelled, so break the loop, and let finally clean up
                break;
            } catch (ExecutionException e) {
                //one of them failed, so report upwards and let finally clean up
                throw new RuntimeException(e.getCause());
            } finally {
                setStopFlagSet(true);//Set the stop flag, in case anything lives on
                stopFolderStopped.cancel(true); //try to shut down the tasks
                primaryFolderStopped.cancel(true);
            }
        }
        return null;
    }



    private FolderWatcher setupPrimaryFolderWatcher(final Path folderToScan, final FolderWatcherClient client, final long timeout) {
        return new FolderWatcher(folderToScan, timeout, client) {
                @Override
                protected void shouldStopNow() throws StoppedException {
                    if (isStopFlagSet()){
                        throw new StoppedException();
                    }
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
            protected void shouldStopNow() throws StoppedException {
                if (isStopFlagSet()){
                    throw new StoppedException();
                }
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
