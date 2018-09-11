package no.uio.ifi.trackfind;

import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.backend.data.providers.ihec.IHECDataProvider;
import no.uio.ifi.trackfind.backend.lucene.DirectoryFactory;
import no.uio.ifi.trackfind.backend.services.VersioningService;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = "no.uio.ifi.trackfind.backend",
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "no.uio.ifi.trackfind.backend.data.providers.*.*"))
public class TestTrackFindApplication {

    public static final String TEST_DATA_PROVIDER = "Test";

    public static void main(String[] args) {
        SpringApplication.run(TestTrackFindApplication.class, args);
    }

    @Bean
    public Git git() throws GitAPIException {
        File tempDirectory = FileUtils.getTempDirectory();
        Git git = Git.init().setDirectory(tempDirectory).call();
        git.add().addFilepattern("console.log").call();
        git.commit().setMessage("").call();
        return git;
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
                Map<String, Collection<String>> dataUrls = new HashMap<>();
                dataUrls.put("someDataType", Collections.singleton("someURL"));
                dataUrls.put("anotherDataType", Collections.singleton("anotherURL"));
                Map<String, Object> dataset1 = new HashMap<>();
                dataset1.put("key1", "value1");
                dataset1.put("browser", dataUrls);
                indexWriter.addDocument(mapToDocumentConverter.apply(postprocessDataset(dataset1)));
                Map<String, Object> dataset2 = new HashMap<>();
                dataset2.put("key1", "value2");
                dataset2.put("key2", "value3");
                dataset2.put("browser", dataUrls);
                indexWriter.addDocument(mapToDocumentConverter.apply(postprocessDataset(dataset2)));
            }

            @Override
            protected void commit(VersioningService.Operation operation) {
                // do nothing
            }

            @Override
            protected void tag() {
                // do nothing
            }

        };
    }

}
