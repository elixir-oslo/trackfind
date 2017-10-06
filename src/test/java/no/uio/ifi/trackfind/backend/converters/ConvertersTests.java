package no.uio.ifi.trackfind.backend.converters;

import no.uio.ifi.trackfind.TestTrackFindApplication;
import org.apache.commons.collections4.MapUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestTrackFindApplication.class)
public class ConvertersTests {

    @Autowired
    private MapToDocumentConverter mapToDocumentConverter;
    @Autowired
    private DocumentToMapConverter documentToMapConverter;
    @Autowired
    private DocumentToJSONConverter documentToJSONConverter;
    @Autowired
    private DocumentToTSVConverter documentToTSVConverter;


    private Document document;
    private Map map;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        document = new Document();
        document.add(new StringField("key1", "value1", Field.Store.NO));
        document.add(new StringField("Basic>id", "value2", Field.Store.NO));
        document.add(new StringField("Basic>id", "value3", Field.Store.NO));

        map = new HashMap();
        map.put("key1", "value1");
        Map innerMap = new HashMap();
        map.put("Basic", innerMap);
        Collection collection = new HashSet();
        collection.add("value2");
        collection.add("value3");
        innerMap.put("id", collection);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mapToDocumentAndBackTest() {
        Document document = mapToDocumentConverter.apply(map);
        Map map = MapUtils.getMap(documentToMapConverter.apply(document), "Advanced");
        map.remove("id");
        assertThat(map).isEqualTo(this.map);
    }

    @Test
    public void documentToJSONConverterTest() {
        String apply = documentToJSONConverter.apply(document);
        assertThat(apply).isEqualToIgnoringCase("{\n" +
                "  \"key1\": \"value1\",\n" +
                "  \"Basic\": {\n" +
                "    \"id\": [\n" +
                "      \"value2\",\n" +
                "      \"value3\"\n" +
                "    ]\n" +
                "  }\n" +
                "}");
    }

    @Test
    public void documentToTSVConverterTest() {
        String apply = documentToTSVConverter.apply(document);
        assertThat(apply).isEqualToIgnoringCase(".\t.\t.\t.\t.\t.\t.\t.\t.\t[value2, value3]\t");
    }

}
