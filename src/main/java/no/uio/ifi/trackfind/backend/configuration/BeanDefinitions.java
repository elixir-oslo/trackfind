package no.uio.ifi.trackfind.backend.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class BeanDefinitions {

    @Bean
    public ExecutorService cachedThreadPool() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public ExecutorService singleThreadExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    @Bean
    public ExecutorService fixedThreadPoolQuadro() {
        return Executors.newFixedThreadPool(4);
    }

    @Bean
    public ExecutorService fixedThreadPoolOcto() {
        return Executors.newFixedThreadPool(8);
    }

}
