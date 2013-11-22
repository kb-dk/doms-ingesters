package dk.statsbiblioteket.doms.ingesters.radiotv;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: abr
 * Date: 8/19/13
 * Time: 4:57 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class MultiThreadedProcessor  implements HotFolderScannerClient {
    protected ExecutorService pool;
    private int poolSize;

    protected MultiThreadedProcessor(int poolSize) {
        this.poolSize = poolSize;
    }

    @Override
      public void waitForThreads() {
          pool.shutdown();

          try {
              pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
          } catch (InterruptedException e) {

          }

      }


    @Override
    public void startEngine() {
        if (pool != null){
            waitForThreads();
        }
        pool = Executors.newFixedThreadPool(poolSize);
    }
}
