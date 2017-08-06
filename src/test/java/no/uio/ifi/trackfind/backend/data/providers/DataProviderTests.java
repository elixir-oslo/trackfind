package no.uio.ifi.trackfind.backend.data.providers;

import com.google.common.collect.Multimap;
import no.uio.ifi.trackfind.TestTrackFindApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

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
        assertThat(metamodel).containsOnlyKeys("key1", "key2");
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
        assertThat(keyset).containsAll(Arrays.asList("key1", "key2"));
        assertThat(metamodel.get("key1")).containsOnly("value1", "value2");
        assertThat(metamodel.get("key2")).containsOnly("value3");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void searchNoneTest() {
        Collection<Map> search = dataProvider.search("key1: value3", 0);
        assertThat(search).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void searchOneTest() {
        Collection<Map> search = dataProvider.search("key1: value1", 0);
        assertThat(search).size().isEqualTo(1);
        Map map = search.iterator().next();
        assertThat(map).containsEntry("key1", "value1");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void searchTwoTest() {
        Collection<Map> search = dataProvider.search("key1: value1 OR key2: value3", 0);
        assertThat(search).size().isEqualTo(2);
        Iterator<Map> iterator = search.iterator();
        Map map = iterator.next();
        assertThat(map).containsEntry("key1", "value1");
        map = iterator.next();
        assertThat(map).containsEntry("key1", "value2");
        assertThat(map).containsEntry("key2", "value3");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void searchTwoWithLimitTest() {
        Collection<Map> search = dataProvider.search("key1: value1 OR key2: value3", 1);
        assertThat(search).size().isEqualTo(1);
        Iterator<Map> iterator = search.iterator();
        Map map = iterator.next();
        assertThat(map).containsEntry("key1", "value1");
    }

}
