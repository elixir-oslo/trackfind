package no.uio.ifi.trackfind;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring Boot application's main class with some configuration and some beans defined.
 *
 * @author Dmytro Titov
 */
@Slf4j
@SpringBootApplication
@EnableCaching
@EnableTransactionManagement
@EnableHystrix
@EnableHystrixDashboard
@EnableAdminServer
public class TrackFindApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrackFindApplication.class, args);
    }

}
