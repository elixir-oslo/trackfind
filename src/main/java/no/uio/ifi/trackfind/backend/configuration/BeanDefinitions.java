package no.uio.ifi.trackfind.backend.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.netopyr.coffee4java.CoffeeScriptEngine;
import com.netopyr.coffee4java.CoffeeScriptEngineFactory;
import de.codecentric.boot.admin.client.config.ClientProperties;
import de.codecentric.boot.admin.client.registration.BlockingRegistrationClient;
import de.codecentric.boot.admin.server.web.client.HttpHeadersProvider;
import lombok.extern.slf4j.Slf4j;
import org.python.util.PythonInterpreter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Configuration
public class BeanDefinitions {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @Value("${trackfind.admin}")
    private String adminElixirId;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @LoadBalanced
    @Bean
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public BlockingRegistrationClient registrationClient(ClientProperties client) {
        RestTemplateBuilder builder = new RestTemplateBuilder()
                .setConnectTimeout(client.getConnectTimeout())
                .setReadTimeout(client.getReadTimeout())
                .additionalInterceptors((request, body, execution) -> {
                    request.getHeaders().set("oidc_claim_sub", adminElixirId);
                    return execution.execute(request, body);
                });
        if (client.getUsername() != null && client.getPassword() != null) {
            builder = builder.basicAuthentication(client.getUsername(), client.getPassword());
        }
        return new BlockingRegistrationClient(builder.build());
    }

    @Bean
    public HttpHeadersProvider oidcHttpHeadersProvider() {
        return instance -> {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set("oidc_claim_sub", adminElixirId);
            return httpHeaders;
        };
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
