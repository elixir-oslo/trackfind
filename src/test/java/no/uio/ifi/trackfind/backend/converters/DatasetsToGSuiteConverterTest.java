package no.uio.ifi.trackfind.backend.converters;

import com.google.gson.Gson;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import no.uio.ifi.trackfind.backend.dao.Dataset;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DatasetsToGSuiteConverterTest.TestConfiguration.class)
@JsonTest
@TestPropertySource("/trackfind.properties")
public class DatasetsToGSuiteConverterTest {

    @Autowired
    private TrackFindProperties properties;

    @Autowired
    private Gson gson;

    @Autowired
    private DatasetsToGSuiteConverter datasetsToGSuiteConverter;

    private Dataset dataset;

    @Before
    public void setUp() {
        dataset = new Dataset();
        dataset.setId(9L);
        dataset.setRepository("test");
        dataset.setVersion(10L);

        Map<String, String> basicDataset = new HashMap<>();
        List<String> basicAttributes = properties.getBasicAttributes();
        int i = 0;
        for (String basicAttribute : basicAttributes) {
            basicDataset.put(basicAttribute, String.valueOf(i++));
        }
        dataset.setBasicDataset(gson.toJson(basicDataset));
    }

    @Test
    public void apply() {
        String gSuite = datasetsToGSuiteConverter.apply(Collections.singleton(dataset));
        assertEquals("###uri\tdata_type\tgenome_build\ttissue_type\tcell_type\ttarget\texperiment_type\tfile_suffix\tdata_source\tidversion\n" +
                "0\t1\t2\t3\t4\t5\t6\t7\t8\t9\t10\n", gSuite);
    }

    @EnableConfigurationProperties(TrackFindProperties.class)
    @ComponentScan(basePackages = "no.uio.ifi.trackfind.backend.converters")
    static class TestConfiguration {
        // nothing
    }

}