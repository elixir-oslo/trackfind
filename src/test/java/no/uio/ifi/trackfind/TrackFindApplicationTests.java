package no.uio.ifi.trackfind;

import com.google.common.collect.Multimap;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestTrackFindApplication.class)
public class TrackFindApplicationTests {

    private static boolean setUpIsDone = false;

    @Autowired
    private DataProvider dataProvider;

    @Before
    public void setUp() throws IOException {
        if (setUpIsDone) {
            return;
        }
        setUpIsDone = true;

        dataProvider.updateIndex();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void metamodelTreeTest() {
        Map<String, Object> metamodel = dataProvider.getMetamodelTree();
        Assertions.assertThat(metamodel).isNotNull().isNotEmpty();
        Assertions.assertThat(metamodel).containsKey("key");
        Object value = metamodel.get("key");
        Assertions.assertThat(value).isInstanceOf(Collection.class);
        Assertions.assertThat((Collection) value).containsOnly("value");
    }

    @Test
    public void metamodelFlatTest() {
        Multimap<String, String> metamodel = dataProvider.getMetamodelFlat();
        Assertions.assertThat(metamodel).isNotNull();
        Assertions.assertThat(metamodel.keySet()).contains("key");
        Assertions.assertThat(metamodel.get("key")).contains("value");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void searchTest() {
        Collection<Map> search = dataProvider.search("key: value");
        Assertions.assertThat(search).size().isEqualTo(1);
        Map map = search.iterator().next();
        Assertions.assertThat(map).containsEntry("key", "value");
    }

}
