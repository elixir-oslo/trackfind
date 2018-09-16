package no.uio.ifi.trackfind.backend.services;

import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.backend.data.providers.TestDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;

import static no.uio.ifi.trackfind.backend.data.providers.TestDataProvider.TEST_DATA_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@ImportAutoConfiguration
@SpringBootTest(classes = TrackFindServiceTests.TestConfiguration.class)
public class TrackFindServiceTests {

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

    @ComponentScan(basePackageClasses = no.uio.ifi.trackfind.backend.services.TrackFindService.class)
    static class TestConfiguration {

        @Bean
        public DataProvider testDataProvider() {
            return new TestDataProvider();
        }

    }

}
