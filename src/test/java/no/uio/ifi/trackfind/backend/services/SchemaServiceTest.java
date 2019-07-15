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
                    "    \"@schema\"\n" +
                    "  ],\n" +
                    "  \"experiments\": [\n" +
                    "    \"experiments->global_id\",\n" +
                    "    \"experiments->tech_type->term_value\",\n" +
                    "    \"experiments->aggregated_from\",\n" +
                    "    \"experiments->local_id\",\n" +
                    "    \"experiments->target->term_value\",\n" +
                    "    \"experiments->compute_protocol_description\",\n" +
                    "    \"experiments->tech_type->term_iri\",\n" +
                    "    \"experiments->lab_protocol_description\",\n" +
                    "    \"experiments->@schema\",\n" +
                    "    \"experiments->sample_ref\",\n" +
                    "    \"experiments->study_ref\",\n" +
                    "    \"experiments->target->term_iri\"\n" +
                    "  ],\n" +
                    "  \"studies\": [\n" +
                    "    \"studies->study_name\",\n" +
                    "    \"studies->contact->e-mail\",\n" +
                    "    \"studies->contact->orcid_id\",\n" +
                    "    \"studies->global_id\",\n" +
                    "    \"studies->phenotype->term_iri\",\n" +
                    "    \"studies->phenotype->term_value\",\n" +
                    "    \"studies->@schema\",\n" +
                    "    \"studies->local_id\",\n" +
                    "    \"studies->contact->name\",\n" +
                    "    \"studies->publications\"\n" +
                    "  ],\n" +
                    "  \"samples\": [\n" +
                    "    \"samples->local_id\",\n" +
                    "    \"samples->@schema\",\n" +
                    "    \"samples->global_id\",\n" +
                    "    \"samples->sample_type->term_iri\",\n" +
                    "    \"samples->biomaterial_type\",\n" +
                    "    \"samples->sample_type->term_value\"\n" +
                    "  ],\n" +
                    "  \"tracks\": [\n" +
                    "    \"tracks->content_type->term_iri\",\n" +
                    "    \"tracks->label_short\",\n" +
                    "    \"tracks->genome_assembly\",\n" +
                    "    \"tracks->checksum->cs_method\",\n" +
                    "    \"tracks->local_id\",\n" +
                    "    \"tracks->file_format->term_value\",\n" +
                    "    \"tracks->experiment_ref\",\n" +
                    "    \"tracks->@schema\",\n" +
                    "    \"tracks->content_type->term_value\",\n" +
                    "    \"tracks->label_long\",\n" +
                    "    \"tracks->checksum->cs_hash\",\n" +
                    "    \"tracks->file_format->term_iri\",\n" +
                    "    \"tracks->global_id\",\n" +
                    "    \"tracks->file_iri\",\n" +
                    "    \"tracks->file_name\"\n" +
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
        assertEquals(ATTRIBUTES, gson.toJson(attributes));
    }

}
