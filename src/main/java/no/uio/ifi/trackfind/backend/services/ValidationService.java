package no.uio.ifi.trackfind.backend.services;

/**
 * Integration with JSON validation service (internal or external).
 *
 * @author Dmytro Titov
 */
public interface ValidationService {

    /**
     * Performs validation.
     *
     * @return Validation result.
     */
    String validate(String repository, String hub);

}
