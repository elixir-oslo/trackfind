package no.uio.ifi.trackfind.backend.services;

import no.uio.ifi.trackfind.backend.dao.Hub;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.backend.data.providers.ihec.IHECDataProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class TrackFindServiceTest {

    private static final String TEST_DATA_PROVIDER = "TEST";

    @InjectMocks
    private TrackFindService trackFindService;

    @Spy
    private Collection<DataProvider> dataProviders = new HashSet<>();

    @Mock
    private IHECDataProvider dataProvider;

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

    @Test
    public void getActiveTrackHubsTest() {
        Collection<Hub> allTrackHubs = trackFindService.getActiveTrackHubs();
        assertThat(allTrackHubs).isNotEmpty().hasSize(1);
        Hub hub = allTrackHubs.iterator().next();
        assertThat(hub.getRepository()).isEqualTo(TEST_DATA_PROVIDER);
        assertThat(hub.getHub()).isEqualTo("active");
    }

    @Test
    public void getAllTrackHubsTest() {
        Collection<Hub> allTrackHubs = trackFindService.getAllTrackHubs();
        assertThat(allTrackHubs).isNotEmpty().hasSize(1);
        Hub hub = allTrackHubs.iterator().next();
        assertThat(hub.getRepository()).isEqualTo(TEST_DATA_PROVIDER);
        assertThat(hub.getHub()).isEqualTo("inactive");
    }

    @Before
    public void setUp() {
        when(dataProvider.getName()).thenReturn(TEST_DATA_PROVIDER);
        when(dataProvider.getActiveTrackHubs()).thenReturn(Collections.singleton(new Hub(TEST_DATA_PROVIDER, "active")));
        when(dataProvider.getAllTrackHubs()).thenReturn(Collections.singleton(new Hub(TEST_DATA_PROVIDER, "inactive")));
        dataProviders.add(dataProvider);
    }

}
