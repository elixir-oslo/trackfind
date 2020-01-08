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
                    "    \"'doc_ontology_versions'->'http://www.ebi.ac.uk/efo/efo.owl'\",\n" +
                    "    \"'doc_ontology_versions'->'http://purl.obolibrary.org/obo/cl.owl'\",\n" +
                    "    \"'doc_version'\",\n" +
                    "    \"'doc_ontology_versions'->'http://purl.obolibrary.org/obo/obi.owl'\",\n" +
                    "    \"'doc_ontology_versions'->'http://edamontology.org/EDAM.owl'\",\n" +
                    "    \"'doc_ontology_versions'->'http://purl.obolibrary.org/obo/so.owl'\",\n" +
                    "    \"'doc_date'\",\n" +
                    "    \"'doc_ontology_versions'->'http://purl.obolibrary.org/obo/ncit.owl'\",\n" +
                    "    \"'doc_ontology_versions'->'http://purl.obolibrary.org/obo/uberon.owl'\"\n" +
                    "  ],\n" +
                    "  \"experiments\": [\n" +
                    "    \"'aggregated_from'\",\n" +
                    "    \"'target'->'phenotype'->'@schema'\",\n" +
                    "    \"'target'->'macromolecular_structure'->'term_label'\",\n" +
                    "    \"'target'->'gene_product_type'->'term_label'\",\n" +
                    "    \"'technique'->'term_id'\",\n" +
                    "    \"'technique'->'term_label'\",\n" +
                    "    \"'target'->'gene_id'\",\n" +
                    "    \"'study_ref'\",\n" +
                    "    \"'target'->'gene_product_type'->'term_id'\",\n" +
                    "    \"'local_id'\",\n" +
                    "    \"'@schema'\",\n" +
                    "    \"'sample_ref'\",\n" +
                    "    \"'target'->'sequence_feature'->'term_id'\",\n" +
                    "    \"'target'->'phenotype'->'term_label'\",\n" +
                    "    \"'target'->'sequence_feature'->'term_label'\",\n" +
                    "    \"'target'->'summary'\",\n" +
                    "    \"'global_id'\",\n" +
                    "    \"'compute_protocol_description'\",\n" +
                    "    \"'target'->'macromolecular_structure'->'term_id'\",\n" +
                    "    \"'target'->'target_details'\",\n" +
                    "    \"'lab_protocol_description'\",\n" +
                    "    \"'target'->'phenotype'->'term_id'\"\n" +
                    "  ],\n" +
                    "  \"studies\": [\n" +
                    "    \"'@schema'\",\n" +
                    "    \"'contact'->'e-mail'\",\n" +
                    "    \"'study_name'\",\n" +
                    "    \"'contact'->'orcid'\",\n" +
                    "    \"'contact'->'name'\",\n" +
                    "    \"'contact'->'@schema'\",\n" +
                    "    \"'global_id'\",\n" +
                    "    \"'publication'\",\n" +
                    "    \"'local_id'\"\n" +
                    "  ],\n" +
                    "  \"samples\": [\n" +
                    "    \"'sample_type'->'cell_type'->'term_label'\",\n" +
                    "    \"'biospecimen_class'->'term_id'\",\n" +
                    "    \"'sample_type'->'cell_type'->'term_id'\",\n" +
                    "    \"'phenotype'->'term_id'\",\n" +
                    "    \"'sample_type'->'abnormal_cell_type'->'term_id'\",\n" +
                    "    \"'sample_type'->'organism_part'->'term_label'\",\n" +
                    "    \"'sample_type'->'cell_line'->'term_id'\",\n" +
                    "    \"'local_id'\",\n" +
                    "    \"'sample_type'->'cell_line'->'term_label'\",\n" +
                    "    \"'@schema'\",\n" +
                    "    \"'biospecimen_class'->'term_label'\",\n" +
                    "    \"'global_id'\",\n" +
                    "    \"'sample_type'->'organism_part'->'term_id'\",\n" +
                    "    \"'phenotype'->'@schema'\",\n" +
                    "    \"'phenotype'->'term_label'\",\n" +
                    "    \"'sample_type'->'abnormal_cell_type'->'term_label'\",\n" +
                    "    \"'sample_type'->'summary'\"\n" +
                    "  ],\n" +
                    "  \"tracks\": [\n" +
                    "    \"'experiment_ref'\",\n" +
                    "    \"'genome_assembly'\",\n" +
                    "    \"'content_type'->'term_label'\",\n" +
                    "    \"'file_format'->'term_id'\",\n" +
                    "    \"'content_type'->'term_id'\",\n" +
                    "    \"'checksum'->'cs_hash'\",\n" +
                    "    \"'checksum'->'cs_method'\",\n" +
                    "    \"'local_id'\",\n" +
                    "    \"'file_url'\",\n" +
                    "    \"'@schema'\",\n" +
                    "    \"'file_format'->'term_label'\",\n" +
                    "    \"'label_long'\",\n" +
                    "    \"'file_name'\",\n" +
                    "    \"'global_id'\",\n" +
                    "    \"'label_short'\",\n" +
                    "    \"'raw_file_ids'\"\n" +
                    "  ],\n" +
                    "  \"collection_info\": [\n" +
                    "    \"'source_repo'->'local_id'\",\n" +
                    "    \"'orig_metadata_url'\",\n" +
                    "    \"'contact'->'e-mail'\",\n" +
                    "    \"'source_repo'->'repo_url'\",\n" +
                    "    \"'contact'->'orcid'\",\n" +
                    "    \"'contact'->'name'\",\n" +
                    "    \"'contact'->'@schema'\",\n" +
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
        schemaService = new SchemaService(
                "https://raw.githubusercontent.com/fairtracks/fairtracks_standard/master/json/schema/fairtracks.schema.json",
                "->"
        );
    }

    @Test
    public void getAttributesTest() {
        Map<String, Collection<String>> attributes = schemaService.getAttributes();
        String actual = gson.toJson(attributes);
        assertEquals(ATTRIBUTES, actual);
    }

}
