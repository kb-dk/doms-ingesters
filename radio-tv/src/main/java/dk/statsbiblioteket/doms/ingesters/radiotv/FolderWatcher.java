package dk.statsbiblioteket.doms.ingesters.radiotv;


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
    private Path hotFolderToScan;
    private final long timeoutInMS;
    private FolderWatcherClient client;

    public FolderWatcher(Path hotFolderToScan, long timeoutInMS, FolderWatcherClient client) {
        this.hotFolderToScan = hotFolderToScan;
        this.timeoutInMS = timeoutInMS;
        this.client = client;
    }


    public Void call() throws IOException, InterruptedException {
        try (final WatchService hotFolderWatcher = FileSystems.getDefault().newWatchService()) {

            hotFolderToScan.register(hotFolderWatcher, StandardWatchEventKinds.ENTRY_MODIFY,
                                     StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);

            //Handle existing files first
            //TODO added or modified?
            //TODO Sort order?
            //TODO signal the user that the folder is now free?
            List<Path> preFiles = Files.list(hotFolderToScan).sorted(sortOnLastModified()).collect(
                    Collectors.toList());

            for (Path preFile : preFiles) {
                if (!shouldStop()) {
                    client.fileAdded(preFile);
                } else {
                    return null;
                }
            }

            //Then watch for changes
            while (!shouldStop()) {  //We run until this is true or we except
                //Handle the hotfolder watcher
                final WatchKey wk = hotFolderWatcher.poll(timeoutInMS, TimeUnit.MILLISECONDS);
                if (wk == null) { //If we reached the timeout, the key is null, so go again
                    continue;
                }

                for (WatchEvent<?> event : wk.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path file = hotFolderToScan.resolve((Path) event.context());
                        client.fileAdded(file);
                    } else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                        //we only register "ENTRY_MODIFY" so the context is always a Path.
                        final Path file = hotFolderToScan.resolve((Path) event.context());
                        client.fileModified(file);
                    } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                        final Path file = hotFolderToScan.resolve((Path) event.context());
                        client.fileDeleted(file);
                    } else if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        //What to do now?
                    }
                }
                // reset the key
                boolean valid = wk.reset();
                if (!valid) {
                    //TODO do not use sout
                    System.out.println("Key has been unregistered");
                }
            }
        } finally {
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
