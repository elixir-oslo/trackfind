package no.uio.ifi.trackfind.backend.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class SchemaServiceTest {

    public static final String ATTRIBUTES =
            "{\n" +
                    "  \"@schema\": [\n" +
                    "    \"\"\n" +
                    "  ],\n" +
                    "  \"experiments\": [\n" +
                    "    \"aggregated_from\",\n" +
                    "    \"@schema\",\n" +
                    "    \"tech_type->term_iri\",\n" +
                    "    \"target->term_iri\",\n" +
                    "    \"sample_ref\",\n" +
                    "    \"compute_protocol_description\",\n" +
                    "    \"local_id\",\n" +
                    "    \"study_ref\",\n" +
                    "    \"global_id\",\n" +
                    "    \"tech_type->term_value\",\n" +
                    "    \"lab_protocol_description\",\n" +
                    "    \"target->term_value\"\n" +
                    "  ],\n" +
                    "  \"studies\": [\n" +
                    "    \"study_name\",\n" +
                    "    \"@schema\",\n" +
                    "    \"phenotype->term_iri\",\n" +
                    "    \"local_id\",\n" +
                    "    \"contact->name\",\n" +
                    "    \"global_id\",\n" +
                    "    \"contact->orcid_id\",\n" +
                    "    \"phenotype->term_value\",\n" +
                    "    \"contact->e-mail\",\n" +
                    "    \"publications\"\n" +
                    "  ],\n" +
                    "  \"samples\": [\n" +
                    "    \"global_id\",\n" +
                    "    \"@schema\",\n" +
                    "    \"sample_type->term_value\",\n" +
                    "    \"biomaterial_type\",\n" +
                    "    \"local_id\",\n" +
                    "    \"sample_type->term_iri\"\n" +
                    "  ],\n" +
                    "  \"tracks\": [\n" +
                    "    \"file_format->term_iri\",\n" +
                    "    \"local_id\",\n" +
                    "    \"file_name\",\n" +
                    "    \"global_id\",\n" +
                    "    \"label_short\",\n" +
                    "    \"file_format->term_value\",\n" +
                    "    \"label_long\",\n" +
                    "    \"checksum->cs_hash\",\n" +
                    "    \"experiment_ref\",\n" +
                    "    \"file_iri\",\n" +
                    "    \"@schema\",\n" +
                    "    \"genome_assembly\",\n" +
                    "    \"content_type->term_value\",\n" +
                    "    \"content_type->term_iri\",\n" +
                    "    \"checksum->cs_method\"\n" +
                    "  ]\n" +
                    "}";

    private Gson gson;

    @Mock
    private TrackFindProperties properties;

    private SchemaService schemaService;

    @Before
    public void setUp() {
        gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        when(properties.getLevelsSeparator()).thenReturn("->");
        schemaService = new SchemaService(properties);
    }

    @Test
    public void getDataProvidersTest() {
        Map<String, Collection<String>> attributes = schemaService.getAttributes();
        String actual = gson.toJson(attributes);
        assertEquals(ATTRIBUTES, actual);
    }

}
