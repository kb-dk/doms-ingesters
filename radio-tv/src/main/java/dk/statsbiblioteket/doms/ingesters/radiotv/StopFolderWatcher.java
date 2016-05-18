package dk.statsbiblioteket.doms.ingesters.radiotv;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;

/**
 * Created with IntelliJ IDEA.
 * User: abr
 * Date: 8/19/13
 * Time: 3:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class StopFolderWatcher extends TimerTask {

    private NonRecursiveHotFolderInspector scanner;
    private File stopFolder;

    public StopFolderWatcher(NonRecursiveHotFolderInspector scanner, File stopFolder) {
        this.scanner = scanner;
        this.stopFolder = stopFolder;
    }

    @Override
    public void run() {

        if (stopSignalGiven()){
            scanner.cancel();
            scanner.setKillFlag();
        }
    }

    private boolean stopSignalGiven() {
        if (!stopFolder.isDirectory()){
            return false;
        }
        File[] stopFiles = stopFolder.listFiles();
        for (File stopFile : stopFiles) {
            if (stopFile.getName().toLowerCase().equals("stoprunning")) {
                return true;
            }
        }
        return false;
    }
}
