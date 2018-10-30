package no.uio.ifi.trackfind.backend.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.netopyr.coffee4java.CoffeeScriptEngine;
import com.netopyr.coffee4java.CoffeeScriptEngineFactory;
import lombok.extern.slf4j.Slf4j;
import org.python.util.PythonInterpreter;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Configuration
public class BeanDefinitions {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @LoadBalanced
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public Gson gson() {
        return new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().setDateFormat(DATE_FORMAT).create();
    }

    @Bean
    public CoffeeScriptEngine coffeeScriptEngine() {
        return (CoffeeScriptEngine) new CoffeeScriptEngineFactory().getScriptEngine();
    }

    @Bean
    public PythonInterpreter pythonInterpreter() {
        Properties props = new Properties();
        props.put("python.import.site", "false");
        Properties systemProperties = System.getProperties();
        PythonInterpreter.initialize(systemProperties, props, new String[0]);
        return new PythonInterpreter();
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
                new Contact("Elixir Norway", "https://github.com/elixir-no-nels/", null),
                null,
                null,
                Collections.emptyList());
    }

    @Bean
    public ExecutorService workStealingPool() {
        return Executors.newWorkStealingPool(10);
    }

    @Bean
    public ExecutorService singleThreadExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    @Bean
    public ExecutorService fixedThreadPool() {
        return Executors.newFixedThreadPool(4);
    }

}
