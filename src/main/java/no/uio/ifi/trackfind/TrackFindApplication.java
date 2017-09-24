package no.uio.ifi.trackfind;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.annotation.PostConstruct;

/**
 * Spring Boot application's main class with some configuration and some beans defined.
 *
 * @author Dmytro Titov
 */
@EnableSwagger2
@Slf4j
@SpringBootApplication
public class TrackFindApplication {

    private ObjectMapper objectMapper;

    public static void main(String[] args) {
        SpringApplication.run(TrackFindApplication.class, args);
    }

    @PostConstruct
    public void setup() {
        configureObjectMapper();
    }

    private void configureObjectMapper() {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

}
