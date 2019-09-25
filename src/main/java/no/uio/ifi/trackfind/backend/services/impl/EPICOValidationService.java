package no.uio.ifi.trackfind.backend.services.impl;

import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.services.ValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration with EPICO JSON validation service.
 *
 * @author Dmytro Titov
 */
@Slf4j
@Service
@Transactional
public class EPICOValidationService implements ValidationService {

    /**
     * {@inheritDoc}
     */
    @Override
    public String validate() {
        return "Success";
    }

}
