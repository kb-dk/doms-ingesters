package dk.statsbiblioteket.doms.ingesters.handleRegistrar;

/**
 * In case trying to resolve a handle failed abnormally
 */
public class ResolveHandleFailedException extends RuntimeException {
    public ResolveHandleFailedException(String message) {
        super(message);
    }

    public ResolveHandleFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
