package no.uio.ifi.trackfind;

import com.google.common.collect.Multimap;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.backend.data.providers.ebi.EBIDataProvider;
import no.uio.ifi.trackfind.backend.data.providers.ihec.IHECDataProvider;
import no.uio.ifi.trackfind.backend.lucene.DirectoryFactory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TrackFindApplicationTests {

    private static boolean setUpIsDone = false;

    @Qualifier("IHECDataProvider")
    @Autowired
    private DataProvider ihecDataProvider;

    @Before
    public void setUp() throws IOException {
        if (setUpIsDone) {
            return;
        }
        setUpIsDone = true;

        ihecDataProvider.updateIndex();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void metamodelTreeTest() {
        Map<String, Object> metamodel = ihecDataProvider.getMetamodelTree();
        Assertions.assertThat(metamodel).isNotNull().isNotEmpty();
        Assertions.assertThat(metamodel).containsKey("key");
        Object value = metamodel.get("key");
        Assertions.assertThat(value).isInstanceOf(Collection.class);
        Assertions.assertThat((Collection) value).containsOnly("value");
    }

    @Test
    public void metamodelFlatTest() {
        Multimap<String, String> metamodel = ihecDataProvider.getMetamodelFlat();
        Assertions.assertThat(metamodel).isNotNull();
        Assertions.assertThat(metamodel.keySet()).contains("key");
        Assertions.assertThat(metamodel.get("key")).contains("value");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void searchTest() {
        Collection<Map> search = ihecDataProvider.search("key: value");
        Assertions.assertThat(search).size().isEqualTo(1);
        Map map = search.iterator().next();
        Assertions.assertThat(map).containsEntry("key", "value");
    }

    @TestConfiguration
    static class TrackFindTestApplication {

        @Bean
        public DirectoryFactory directoryFactory() {
            return new DirectoryFactory() {
                @Override
                public Directory getDirectory(String dataProviderName) throws IOException {
                    return new RAMDirectory();
                }
            };
        }

        @Bean("IHECDataProvider")
        public DataProvider ihecDataProvider() {
            return new IHECDataProvider() {
                @Override
                public Collection<Map> fetchData() {
                    HashMap<String, String> track = new HashMap<>();
                    track.put("key", "value");
                    return Collections.singleton(track);
                }
            };
        }

        @Bean("EBIDataProvider")
        public DataProvider ebiDataProvider() {
            return new EBIDataProvider() {
                @Override
                public Collection<Map> fetchData() {
                    return Collections.emptySet();
                }
            };
        }

    }


}
