package no.uio.ifi.trackfind.backend.services;

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
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class TrackFindServiceTests {

    private static final String TEST_DATA_PROVIDER = "TEST";

    @InjectMocks
    private TrackFindService trackFindService;

    @Spy
    private Collection<DataProvider> dataProviders = new HashSet<>();

    @Mock
    private IHECDataProvider dataProvider;

    @Before
    public void setUp() {
        when(dataProvider.getName()).thenReturn(TEST_DATA_PROVIDER);
        dataProviders.add(dataProvider);
    }

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
