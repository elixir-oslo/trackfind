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
                    "  \"doc_info\": [\n" +
                    "    \"'doc_url'\",\n" +
                    "    \"'doc_date'\",\n" +
                    "    \"'doc_version'\"\n" +
                    "  ],\n" +
                    "  \"experiments\": [\n" +
                    "    \"'@schema'\",\n" +
                    "    \"'aggregated_from'\",\n" +
                    "    \"'sample_ref'\",\n" +
                    "    \"'target'->'term_url'\",\n" +
                    "    \"'study_ref'\",\n" +
                    "    \"'global_id'\",\n" +
                    "    \"'technique'->'term_url'\",\n" +
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
                    "    \"'sample_type'->'term_url'\",\n" +
                    "    \"'biomaterial_type'\",\n" +
                    "    \"'phenotype'->'term_value'\",\n" +
                    "    \"'tissue_type'->'term_value'\",\n" +
                    "    \"'phenotype'->'term_url'\",\n" +
                    "    \"'sample_type'->'term_value'\",\n" +
                    "    \"'global_id'\",\n" +
                    "    \"'tissue_type'->'term_url'\",\n" +
                    "    \"'local_id'\"\n" +
                    "  ],\n" +
                    "  \"tracks\": [\n" +
                    "    \"'content_type'->'term_url'\",\n" +
                    "    \"'experiment_ref'\",\n" +
                    "    \"'genome_assembly'\",\n" +
                    "    \"'file_format'->'term_url'\",\n" +
                    "    \"'checksum'->'cs_hash'\",\n" +
                    "    \"'checksum'->'cs_method'\",\n" +
                    "    \"'local_id'\",\n" +
                    "    \"'file_format'->'term_value'\",\n" +
                    "    \"'file_url'\",\n" +
                    "    \"'@schema'\",\n" +
                    "    \"'label_long'\",\n" +
                    "    \"'file_name'\",\n" +
                    "    \"'global_id'\",\n" +
                    "    \"'label_short'\",\n" +
                    "    \"'raw_file_ids'\",\n" +
                    "    \"'content_type'->'term_value'\"\n" +
                    "  ],\n" +
                    "  \"collection_info\": [\n" +
                    "    \"'source_repo'->'local_id'\",\n" +
                    "    \"'orig_metadata_url'\",\n" +
                    "    \"'contact'->'e-mail'\",\n" +
                    "    \"'source_repo'->'repo_url'\",\n" +
                    "    \"'contact'->'orcid'\",\n" +
                    "    \"'contact'->'name'\",\n" +
                    "    \"'long_name'\",\n" +
                    "    \"'short_name'\",\n" +
                    "    \"'description_url'\"\n" +
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
