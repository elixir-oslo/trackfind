package no.uio.ifi.trackfind.backend.data.providers;

import com.google.common.collect.Multimap;
import no.uio.ifi.trackfind.TestTrackFindApplication;
import org.apache.lucene.document.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestTrackFindApplication.class)
public class DataProviderTests {

    @Autowired
    private DataProvider dataProvider;

    @SuppressWarnings("unchecked")
    @Test
    public void metamodelTreeTest() {
        Map<String, Object> metamodel = dataProvider.getMetamodelTree();
        assertThat(metamodel).isNotNull().isNotEmpty();
        assertThat(metamodel).containsOnlyKeys("Advanced");
        metamodel = (Map<String, Object>) metamodel.get("Advanced");
        assertThat(metamodel).containsKeys("key1", "key2", "data_type");
        Object value = metamodel.get("key1");
        assertThat(value).isInstanceOf(Collection.class);
        assertThat((Collection) value).containsOnly("value1", "value2");
        value = metamodel.get("key2");
        assertThat(value).isInstanceOf(Collection.class);
        assertThat((Collection) value).containsOnly("value3");
    }

    @Test
    public void metamodelFlatTest() {
        Multimap<String, String> metamodel = dataProvider.getMetamodelFlat();
        assertThat(metamodel).isNotNull();
        Set<String> keyset = metamodel.keySet();
        assertThat(keyset).isNotNull().isNotEmpty();
        assertThat(keyset).containsAll(Arrays.asList("Advanced>key1", "Advanced>key2"));
        assertThat(metamodel.get("Advanced>key1")).containsOnly("value1", "value2");
        assertThat(metamodel.get("Advanced>key2")).containsOnly("value3");
    }

    @Test
    public void searchNoneTest() {
        Collection<Document> result = dataProvider.search("key1: value3", 0);
        assertThat(result).isEmpty();
    }

    @Test
    public void searchOneTest() {
        Collection<Document> result = dataProvider.search("Advanced>key2: value3", 0);
        assertThat(result).size().isEqualTo(1);
        Document document = result.iterator().next();
        assertThat(document.get("Advanced>key2")).isEqualToIgnoringCase("value3");
    }

}
