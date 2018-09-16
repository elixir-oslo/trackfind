package no.uio.ifi.trackfind.backend.converters;

import com.google.gson.Gson;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import no.uio.ifi.trackfind.backend.dao.Dataset;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
public class DatasetsToGSuiteConverterTest {

    @InjectMocks
    private DatasetsToGSuiteConverter datasetsToGSuiteConverter;

    private Gson gson = new Gson();

    @Spy
    private TrackFindProperties properties = new TrackFindProperties();

    private Dataset dataset;

    @Before
    public void setUp() {
        datasetsToGSuiteConverter.setGson(new Gson());

        properties.setIdAttribute("id");
        properties.setVersionAttribute("version");
        properties.setUriAttribute("uri");
        properties.setDataTypeAttribute("data_type");
        properties.setBasicAttributes(Arrays.asList("uri", "data_type", "attribute"));

        dataset = new Dataset();
        dataset.setId(1L);
        dataset.setRepository("test");
        dataset.setVersion(2L);

        Map<String, String> basicDataset = new HashMap<>();
        basicDataset.put("data_type", "data_type");
        basicDataset.put("uri", "uri");
        basicDataset.put("attribute", "value");
        dataset.setBasicDataset(gson.toJson(basicDataset));
    }

    @Test
    public void apply() {
        String gSuite = datasetsToGSuiteConverter.apply(Collections.singleton(dataset));
        assertEquals("###uri\tdata_type\tattribute\tid\tversion\n" +
                "uri\tdata_type\tvalue\t1\t2\n", gSuite);
    }

}