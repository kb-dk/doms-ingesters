package dk.statsbiblioteket.doms.ingesters.radiotv;

import java.io.IOException;

/**
 * This exception is thrown when we need to overwrite an object and the overwrite flag is false
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
