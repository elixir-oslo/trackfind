package no.uio.ifi.trackfind;

import com.google.common.collect.Multimap;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestTrackFindApplication.class)
public class TrackFindApplicationTests {

    @Autowired
    private DataProvider dataProvider;

    @SuppressWarnings("unchecked")
    @Test
    public void metamodelTreeTest() {
        Map<String, Object> metamodel = dataProvider.getMetamodelTree();
        assertThat(metamodel).isNotNull().isNotEmpty();
        assertThat(metamodel).containsKey("key");
        Object value = metamodel.get("key");
        assertThat(value).isInstanceOf(Collection.class);
        assertThat((Collection) value).containsOnly("value");
    }

    @Test
    public void metamodelFlatTest() {
        Multimap<String, String> metamodel = dataProvider.getMetamodelFlat();
        assertThat(metamodel).isNotNull();
        assertThat(metamodel.keySet()).contains("key");
        assertThat(metamodel.get("key")).contains("value");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void searchTest() {
        Collection<Map> search = dataProvider.search("key: value");
        assertThat(search).size().isEqualTo(1);
        Map map = search.iterator().next();
        assertThat(map).containsEntry("key", "value");
    }

}
