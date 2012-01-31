package dk.statsbiblioteket.doms.ingesters.handleRegistrar;

/**
 * Exception signifying initialization failure.
 */
public class InitializationFailedException extends RuntimeException {
    public InitializationFailedException(String message) {
        super(message);
    }

    public InitializationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
