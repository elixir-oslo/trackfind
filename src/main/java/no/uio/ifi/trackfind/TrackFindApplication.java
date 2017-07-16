package no.uio.ifi.trackfind;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

@EnableCaching
@SpringBootApplication
public class TrackFindApplication {

    private static final String INDICES_FOLDER = "indices";

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
    public Directory directory() throws IOException {
        return FSDirectory.open(new File(INDICES_FOLDER).toPath());
    }


}
