package no.uio.ifi.trackfind.backend.services;

import com.google.common.collect.Multimap;
import no.uio.ifi.trackfind.backend.dao.Dataset;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@ImportAutoConfiguration
@SpringBootTest(classes = TrackFindServiceTests.TestConfiguration.class)
public class TrackFindServiceTests {

    private static final String TEST_DATA_PROVIDER = "TEST";

    @Autowired
    private TrackFindService trackFindService;

    @Test
    public void getDataProvidersTest() {
        Collection<DataProvider> dataProviders = trackFindService.getDataProviders();
        assertThat(dataProviders).isNotEmpty().hasSize(1);
        DataProvider dataProvider = dataProviders.iterator().next();
        assertThat(dataProvider.getName()).isEqualTo(TEST_DATA_PROVIDER);
    }

    @Test
    public void getDataProviderTest() {
        DataProvider dataProvider = trackFindService.getDataProvider(TEST_DATA_PROVIDER);
        assertThat(dataProvider.getName()).isEqualTo(TEST_DATA_PROVIDER);
    }

    static class TestDataProvider implements DataProvider, Comparable<DataProvider> {

        @Override
        public String getName() {
            return TEST_DATA_PROVIDER;
        }

        @Override
        public void crawlRemoteRepository() {

        }

        @Override
        public void applyMappings() {

        }

        @Override
        public Map<String, Object> getMetamodelTree() {
            return null;
        }

        @Override
        public Multimap<String, String> getMetamodelFlat() {
            return null;
        }

        @Override
        public Collection<Dataset> search(String query, int limit) {
            return null;
        }

        @Override
        public Map<String, Object> fetch(String datasetId, String version) {
            return null;
        }

        @Override
        public int compareTo(DataProvider o) {
            return 0;
        }

    }

    @ComponentScan(basePackages = "no.uio.ifi.trackfind.backend.services")
    static class TestConfiguration {

        @Bean
        public DataProvider testDataProvider() {
            return new TestDataProvider();
        }

    }

}
