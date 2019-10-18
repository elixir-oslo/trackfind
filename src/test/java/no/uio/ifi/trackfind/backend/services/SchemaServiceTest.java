package no.uio.ifi.trackfind.backend.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import no.uio.ifi.trackfind.backend.services.impl.SchemaService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
public class SchemaServiceTest {

    public static final String ATTRIBUTES =
            "{\n" +
                    "  \"experiments\": [\n" +
                    "    \"'@schema'\",\n" +
                    "    \"'technique'->'term_iri'\",\n" +
                    "    \"'aggregated_from'\",\n" +
                    "    \"'sample_ref'\",\n" +
                    "    \"'target'->'term_iri'\",\n" +
                    "    \"'study_ref'\",\n" +
                    "    \"'global_id'\",\n" +
                    "    \"'target'->'term_value'\",\n" +
                    "    \"'compute_protocol_description'\",\n" +
                    "    \"'technique'->'term_value'\",\n" +
                    "    \"'local_id'\",\n" +
                    "    \"'lab_protocol_description'\"\n" +
                    "  ],\n" +
                    "  \"studies\": [\n" +
                    "    \"'@schema'\",\n" +
                    "    \"'contact'->'e-mail'\",\n" +
                    "    \"'study_name'\",\n" +
                    "    \"'contact'->'orcid'\",\n" +
                    "    \"'contact'->'name'\",\n" +
                    "    \"'global_id'\",\n" +
                    "    \"'publication'\",\n" +
                    "    \"'local_id'\"\n" +
                    "  ],\n" +
                    "  \"samples\": [\n" +
                    "    \"'@schema'\",\n" +
                    "    \"'tissue_type'->'term_iri'\",\n" +
                    "    \"'biomaterial_type'\",\n" +
                    "    \"'sample_type'->'term_iri'\",\n" +
                    "    \"'phenotype'->'term_value'\",\n" +
                    "    \"'tissue_type'->'term_value'\",\n" +
                    "    \"'phenotype'->'term_iri'\",\n" +
                    "    \"'sample_type'->'term_value'\",\n" +
                    "    \"'global_id'\",\n" +
                    "    \"'local_id'\"\n" +
                    "  ],\n" +
                    "  \"tracks\": [\n" +
                    "    \"'file_format'->'term_iri'\",\n" +
                    "    \"'genome_assembly'\",\n" +
                    "    \"'experiment_ref'\",\n" +
                    "    \"'checksum'->'cs_hash'\",\n" +
                    "    \"'checksum'->'cs_method'\",\n" +
                    "    \"'local_id'\",\n" +
                    "    \"'file_format'->'term_value'\",\n" +
                    "    \"'@schema'\",\n" +
                    "    \"'file_iri'\",\n" +
                    "    \"'content_type'->'term_iri'\",\n" +
                    "    \"'label_long'\",\n" +
                    "    \"'file_name'\",\n" +
                    "    \"'global_id'\",\n" +
                    "    \"'label_short'\",\n" +
                    "    \"'content_type'->'term_value'\"\n" +
                    "  ]\n" +
                    "}";

    private Gson gson;

    private SchemaService schemaService;

    @Before
    public void setUp() {
        gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        schemaService = new SchemaService("->");
    }

    @Test
    public void getAttributesTest() {
        Map<String, Collection<String>> attributes = schemaService.getAttributes();
        String actual = gson.toJson(attributes);
        assertEquals(ATTRIBUTES, actual);
    }

}
