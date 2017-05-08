package dk.statsbiblioteket.doms.ingesters.radiotv;

/**
 * Created by abr on 08-05-17.
 */
public class OperationFailed extends Exception {

    public OperationFailed() {
    }

    public OperationFailed(String message) {
        super(message);
    }

    public OperationFailed(String message, Throwable cause) {
        super(message, cause);
    }

    public OperationFailed(Throwable cause) {
        super(cause);
    }

    public OperationFailed(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
