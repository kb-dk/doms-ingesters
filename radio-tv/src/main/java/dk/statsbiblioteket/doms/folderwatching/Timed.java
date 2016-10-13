package dk.statsbiblioteket.doms.folderwatching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

/**
 * Timed implements AutoClosable but without throwing exceptions. Is used for timing the execution of code inside try-with-resources
 * blocks
 *
 * @see #timeExecution(String) (String)
 */
public interface Timed extends AutoCloseable {

    Logger log = LoggerFactory.getLogger(Timed.class);

    /**
     * A Trick to time the execution of a try statement and get it automatically to log the execution time when closed
     * <p>
     * use it like this
     * <pre>
     * try (Timed ignored = timeExecution(name)) {
     * //Do stuff
     * }
     * </pre>
     * <p>
     * Works by returning a Timed, which extends AutoClosable. The close method is implemented to log the duration at INFO level
     *
     * @param name the name to log the duration time for
     * @return an autoclosable instance for logging the execution time
     */
    static Timed timeExecution(String name) {
        Instant start = Instant.now();
        log.info("Starting work on {}",name);
        return () -> {
            log.info("Finished work on {} which took {} milliseconds",name, Duration.between(start,Instant.now()).toMillis());
        };
    }

    /**
     * Time the executio
     *
     * @param path the path, name is derived as the filename of the rightmost part
     * @return a Timed
     * @see #timeExecution(String)
     */
    static Timed timeExecution(Path path) {
        return Timed.timeExecution(path.toFile().getName());
    }


    @Override
    void close();
}
