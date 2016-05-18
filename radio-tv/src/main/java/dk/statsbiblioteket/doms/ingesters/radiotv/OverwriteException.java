package dk.statsbiblioteket.doms.ingesters.radiotv;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: abr
 * Date: 8/19/13
 * Time: 3:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class OverwriteException extends IOException {
    public OverwriteException() {
        super();
    }

    public OverwriteException(String message) {
        super(message);
    }

    public OverwriteException(String message, Throwable cause) {
        super(message, cause);
    }

    public OverwriteException(Throwable cause) {
        super(cause);
    }
}
