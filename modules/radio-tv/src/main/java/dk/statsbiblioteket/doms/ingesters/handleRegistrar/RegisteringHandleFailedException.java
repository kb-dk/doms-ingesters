package dk.statsbiblioteket.doms.ingesters.handleRegistrar;

/**
 * In case trying to resolve a handle failed abnormally
 */
public class RegisteringHandleFailedException extends RuntimeException {
    public RegisteringHandleFailedException(String message) {
        super(message);
    }

    public RegisteringHandleFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
