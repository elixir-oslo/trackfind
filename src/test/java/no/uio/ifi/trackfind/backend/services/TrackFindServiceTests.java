package no.uio.ifi.trackfind.backend.services;

import no.uio.ifi.trackfind.TestTrackFindApplication;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;

import static no.uio.ifi.trackfind.TestTrackFindApplication.TEST_DATA_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestTrackFindApplication.class)
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

}
