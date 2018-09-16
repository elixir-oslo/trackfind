package no.uio.ifi.trackfind;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Spring Boot application's main class with some configuration and some beans defined.
 *
 * @author Dmytro Titov
 */
@Slf4j
@SpringBootApplication
public class TrackFindApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrackFindApplication.class, args);
    }

}
