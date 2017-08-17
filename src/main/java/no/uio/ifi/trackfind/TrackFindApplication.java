package no.uio.ifi.trackfind;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Spring Boot application's main class with some configuration and some beans defined.
 *
 * @author Dmytro Titov
 */
@EnableCaching
@SpringBootApplication
public class TrackFindApplication {

    private final ObjectMapper objectMapper;

    @Autowired
    public TrackFindApplication(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public static void main(String[] args) {
        SpringApplication.run(TrackFindApplication.class, args);
    }

    @PostConstruct
    public void setup() {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    @Bean
    public ExecutorService cachedThreadPool() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public ExecutorService fixedThreadPool() {
        return Executors.newFixedThreadPool(4);
    }

}
