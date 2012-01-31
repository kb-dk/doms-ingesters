package dk.statsbiblioteket.doms.ingesters.handleRegistrar;

/**
 * Exception signifying inconsistent data.
 */
public class InconsistentDataException extends RuntimeException {
    public InconsistentDataException(String message) {
        super(message);
    }

    public InconsistentDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
