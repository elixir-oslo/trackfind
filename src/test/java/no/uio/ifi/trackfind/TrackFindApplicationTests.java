package no.uio.ifi.trackfind;

import com.google.common.collect.Multimap;
import no.uio.ifi.trackfind.data.providers.DataProvider;
import no.uio.ifi.trackfind.services.TrackFindService;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TrackFindApplicationTests {

    private static boolean setUpIsDone = false;

    @MockBean
    private DataProvider ihecDataProvider;

    @Autowired
    private TrackFindService trackFindService;

    @Before
    public void setUp() {
        if (setUpIsDone) {
            return;
        }
        setUpIsDone = true;

        Map<String, String> track = new HashMap<>();
        track.put("key", "value");
        given(ihecDataProvider.fetchData()).willReturn(Collections.singleton(track));
        trackFindService.updateIndex();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void metamodelTreeTest() {
        Map<String, Object> metamodel = trackFindService.getMetamodelTree();
        Assertions.assertThat(metamodel).isNotNull();
        Assertions.assertThat(metamodel).containsKey("key");
        Object value = metamodel.get("key");
        Assertions.assertThat(value).isInstanceOf(Collection.class);
        Assertions.assertThat((Collection) value).containsOnly("value");
    }

    @Test
    public void metamodelFlatTest() {
        Multimap<String, String> metamodel = trackFindService.getMetamodelFlat();
        Assertions.assertThat(metamodel).isNotNull();
        Assertions.assertThat(metamodel.keySet()).contains("key");
        Assertions.assertThat(metamodel.get("key")).contains("value");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void indexingTest() {
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
