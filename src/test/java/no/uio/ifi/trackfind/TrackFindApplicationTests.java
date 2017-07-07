package no.uio.ifi.trackfind;

import no.uio.ifi.trackfind.data.providers.DataProvider;
import no.uio.ifi.trackfind.services.TrackFindService;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.*;

import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TrackFindApplicationTests {

    @MockBean
    private DataProvider ihecDataProvider;

    @Autowired
    private TrackFindService trackFindService;

    @SuppressWarnings("unchecked")
    @Test
    public void indexingTest() {
        Map<String, String> track = new HashMap<>();
        track.put("key", "value");
        Set<Map> data = Collections.singleton(track);
        given(ihecDataProvider.fetchData()).willReturn(data);
        trackFindService.updateIndex();
        Collection<Map> search = trackFindService.search("key: value");
        Assertions.assertThat(search).size().isEqualTo(1);
        Map map = search.iterator().next();
        Assertions.assertThat(map).containsEntry("key", "value");
    }

    @TestConfiguration
    static class TrackFindTestApplication {

        @Bean
        public Directory directory() throws IOException {
            return new RAMDirectory();
        }

    }


}
