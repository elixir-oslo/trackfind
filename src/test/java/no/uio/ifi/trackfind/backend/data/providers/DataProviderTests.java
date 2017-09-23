package no.uio.ifi.trackfind.backend.data.providers;

import com.google.common.collect.Multimap;
import no.uio.ifi.trackfind.TestTrackFindApplication;
import no.uio.ifi.trackfind.backend.converters.DocumentToMapConverter;
import org.apache.commons.collections4.MapUtils;
import org.apache.lucene.document.Document;
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

    @Test
    public void getNameTest() {
        assertThat(dataProvider.getName()).isEqualTo("Test");
    }

    @Test
    public void getPathTest() {
        assertThat(dataProvider.getPath()).isEqualTo("indices/Test/");
    }

    @Test
    public void saveLoadConfigurationTest() {
        DataProvider.Configuration oldConfiguration = new DataProvider.Configuration();
        oldConfiguration.setAttributesMapping(new HashMap<String, String>() {{
            put("1", "2");
        }});
        dataProvider.saveConfiguration(oldConfiguration);
        DataProvider.Configuration newConfiguration = dataProvider.loadConfiguration();
        assertThat(newConfiguration).isNotNull();
        Map<String, String> attributesMapping = newConfiguration.getAttributesMapping();
        assertThat(attributesMapping).isNotNull().isNotEmpty();
        assertThat(attributesMapping).containsOnlyKeys("1");
        assertThat(attributesMapping).containsEntry("1", "2");
    }

    @Test
    public void applyMappingsTest() {
        DataProvider.Configuration oldConfiguration = new DataProvider.Configuration();
        oldConfiguration.setAttributesMapping(new HashMap<String, String>() {{
            put("Advanced>key1", "BasicKey");
        }});
        dataProvider.saveConfiguration(oldConfiguration);
        dataProvider.applyMappings();
        Map<String, Collection<String>> metamodel = dataProvider.getMetamodelFlat().asMap();
        assertThat(metamodel).containsKeys("Basic>BasicKey");
        assertThat(metamodel.get("Basic>BasicKey")).hasSize(2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void metamodelTreeTest() {
        Map<String, Object> metamodel = dataProvider.getMetamodelTree();
        assertThat(metamodel).isNotNull().isNotEmpty();
        assertThat(metamodel).containsOnlyKeys("Advanced");
        metamodel = (Map<String, Object>) metamodel.get("Advanced");
        assertThat(metamodel).containsKeys("key1", "key2");
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
    public void searchNotFoundTest() {
        Collection<Map> result = dataProvider.search("key1: value3", 0);
        assertThat(result).isEmpty();
    }

    @Test
    public void searchFoundTest() {
        Collection<Map> result = dataProvider.search("Advanced>key2: value3", 0);
        assertThat(result).size().isEqualTo(2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fetch() {
        Collection<Map> result = dataProvider.search("Advanced>key2: value3", 1);
        Map map = MapUtils.getMap(result.iterator().next(), "Advanced");
        String id = String.valueOf(map.remove("id"));
        Map<String, Object> rawData = dataProvider.fetch(id);
        assertThat(rawData).isEqualTo(map);
    }

}
