package no.uio.ifi.trackfind.backend.annotations;

import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Annotation for combining both @Component and @DependsOn("git") annotations.
 */
@DependsOn("git")
@Component
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface VersionedComponent {
}
