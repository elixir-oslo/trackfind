package no.uio.ifi.trackfind;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

/**
 * Spring Boot application's main class with some configuration and some beans defined.
 *
 * @author Dmytro Titov
 */
@EnableSwagger2
@Slf4j
@SpringBootApplication
public class TrackFindApplication {

    public static final String INDICES_FOLDER = "indices/";

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private ObjectMapper objectMapper;

    public static void main(String[] args) {
        SpringApplication.run(TrackFindApplication.class, args);
    }

    @PostConstruct
    private void setup() {
        configureObjectMapper();
    }

    private void configureObjectMapper() {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    @Bean
    public Git git() throws IOException, GitAPIException {
        Git git = Git.init().setDirectory(new File(INDICES_FOLDER)).call();
        try {
            git.log().call();
        } catch (NoHeadException e) {
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Initial commit.").call();
        }
        return git;
    }

    @Bean
    public Gson gson() {
        return new GsonBuilder().setPrettyPrinting().setDateFormat(DATE_FORMAT).create();
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2).
                useDefaultResponseMessages(false).
                select().
                apis(RequestHandlerSelectors.basePackage("no.uio.ifi.trackfind.backend.rest.controllers")).
                paths(PathSelectors.any()).
                build().
                apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        return new ApiInfo("TrackFind",
                "Search engine for finding genomic tracks",
                String.valueOf(getClass().getPackage().getImplementationVersion()),
                null,
                new Contact(null, "https://github.com/elixir-no-nels/trackfind", null),
                null,
                null,
                Collections.emptyList());
    }

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

}
