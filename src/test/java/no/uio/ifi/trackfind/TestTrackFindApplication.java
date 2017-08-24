package no.uio.ifi.trackfind;

import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.backend.data.providers.ihec.IHECDataProvider;
import no.uio.ifi.trackfind.backend.lucene.DirectoryFactory;
import org.apache.lucene.index.IndexWriter;
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

    public static final String TEST_DATA_PROVIDER = "Test";

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
        return new IHECDataProvider() {
            @Override
            public String getName() {
                return TEST_DATA_PROVIDER;
            }

            @Override
            protected void fetchData(IndexWriter indexWriter) throws Exception {
                Map<String, Object> dataset1 = new HashMap<>();
                dataset1.put("key1", "value1");
                indexWriter.addDocument(convertDatasetToDocument(dataset1));
                Map<String, Object> dataset2 = new HashMap<>();
                dataset2.put("key1", "value2");
                dataset2.put("key2", "value3");
                indexWriter.addDocument(convertDatasetToDocument(dataset2));
                Map<String, Object> dataset3 = new HashMap<>();
                Map<String, Collection<String>> dataUrls = new HashMap<>();
                dataUrls.put("someDataType", Collections.singleton("someURL"));
                dataUrls.put("anotherDataType", Collections.singleton("anotherURL"));
                dataset3.put(DataProvider.DATA_URL_ATTRIBUTE, dataUrls);
                postProcessDataset(dataset3);
                indexWriter.addDocument(convertDatasetToDocument(dataset3));
            }
        };
    }

}
