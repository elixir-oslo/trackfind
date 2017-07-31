package no.uio.ifi.trackfind;

import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.backend.data.providers.ihec.IHECDataProvider;
import no.uio.ifi.trackfind.backend.lucene.DirectoryFactory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@EnableCaching
@SpringBootApplication
@ComponentScan(basePackages = "no.uio.ifi.trackfind.backend",
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "no.uio.ifi.trackfind.backend.data.providers.*.*"))
public class TestTrackFindApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestTrackFindApplication.class, args);
    }

    @Bean
    public DirectoryFactory directoryFactory() {
        return new DirectoryFactory() {
            @Override
            public Directory getDirectory(String dataProviderName) throws IOException {
                return new RAMDirectory();
            }
        };
    }

    @Bean
    public DataProvider ihecDataProvider() {
        IHECDataProvider testDataProvider = new IHECDataProvider() {
            @Override
            public Collection<Map> fetchData() {
                HashMap<String, String> track = new HashMap<>();
                track.put("key", "value");
                return Collections.singleton(track);
            }
        };
        testDataProvider.updateIndex();
        return testDataProvider;
    }

}
