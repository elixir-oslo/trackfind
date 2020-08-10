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
                    "    {\n" +
                    "      \"path\": \"'technique'->'term_label'\",\n" +
                    "      \"description\": \"Exact value according to the ontology used\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'target'->'target_details'\",\n" +
                    "      \"description\": \"Important details about the target of the experiment (to be included in the 'target' property)\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'target'->'gene_product_type'->'term_label'\",\n" +
                    "      \"description\": \"Exact value according to the ontology used\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'lab_protocol_description'\",\n" +
                    "      \"description\": \"Free-text description of lab protocol, or URL to such description\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'aggregated_from'\",\n" +
                    "      \"description\": \"References to external experiments used as basis for aggregated data generation (using global experiment identifiers, resolvable by identifiers.org)\",\n" +
                    "      \"icon\": \"\uD83C\uDF10\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'study_ref'\",\n" +
                    "      \"description\": \"Reference to the study that generated the sample (using the submitter-local identifier of the study)\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'target'->'gene_product_type'\",\n" +
                    "      \"description\": \"Gene product type targeted by the experiment (e.g., miRNA)\",\n" +
                    "      \"icon\": \"\uD83D\uDCD6\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'target'->'phenotype'->'term_label'\",\n" +
                    "      \"description\": \"Exact value according to the ontology used\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'target'->'macromolecular_structure'->'term_label'\",\n" +
                    "      \"description\": \"Exact value according to the ontology used\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'local_id'\",\n" +
                    "      \"description\": \"Submitter-local identifier (within investigation/hub) for experiment (in CURIE-format, if applicable)\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'global_id'\",\n" +
                    "      \"description\": \"Global experiment identifier, resolvable by identifiers.org\",\n" +
                    "      \"icon\": \"\uD83C\uDF10\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'target'->'sequence_feature'->'term_label'\",\n" +
                    "      \"description\": \"Exact value according to the ontology used\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'target'->'sequence_feature'->'term_id'\",\n" +
                    "      \"description\": \"URL linking to an ontology term\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'technique'\",\n" +
                    "      \"description\": \"Main technique used in experiment (e.g., laboratory, computational or statistical technique)\",\n" +
                    "      \"icon\": \"\uD83D\uDCD6\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'target'->'macromolecular_structure'->'term_id'\",\n" +
                    "      \"description\": \"URL linking to an ontology term\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'target'\",\n" +
                    "      \"description\": \"Main target of the experiment\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'target'->'gene_product_type'->'term_id'\",\n" +
                    "      \"description\": \"URL linking to an ontology term\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'technique'->'term_id'\",\n" +
                    "      \"description\": \"URL linking to an ontology term\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'target'->'macromolecular_structure'\",\n" +
                    "      \"description\": \"Macromolecular structure targeted by the experiment (e.g., chromatin strucure)\",\n" +
                    "      \"icon\": \"\uD83D\uDCD6\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'compute_protocol_description'\",\n" +
                    "      \"description\": \"Free-text description of computational protocol, or URL to such description\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'target'->'phenotype'->'@schema'\",\n" +
                    "      \"description\": \"The absolute URL of the 'current' version of the relevant FAIRtracks JSON schema within the same major version as the JSON document follows (which should ensure compatibility). Must match the value of '$id' in the linked schema\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'target'->'gene_id'\",\n" +
                    "      \"description\": \"HGNC identifier for gene targeted by the experiment (e.g., specific transcription factor used as ChIP-seq antibody).\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'target'->'sequence_feature'\",\n" +
                    "      \"description\": \"Sequence feature targeted by the experiment\",\n" +
                    "      \"icon\": \"\uD83D\uDCD6\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'sample_ref'\",\n" +
                    "      \"description\": \"Reference to the sample of the experiment (using the submitter-local identifier of the sample)\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'target'->'summary'\",\n" +
                    "      \"description\": \"Summary of 'target', copied from 'sequence_feature', 'gene_id', 'gene_product', or 'macromolecular_structure' (and adding 'target_detail'), according to 'technique'\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'target'->'phenotype'\",\n" +
                    "      \"description\": \"Main phenotype (e.g. disease) connected to the sample\",\n" +
                    "      \"icon\": \"\uD83D\uDCD6\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'target'->'phenotype'->'term_id'\",\n" +
                    "      \"description\": \"URL linking to an ontology term\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"samples\": [\n" +
                    "    {\n" +
                    "      \"path\": \"'sample_type'->'cell_type'\",\n" +
                    "      \"description\": \"Cell type of isolated normal cells in the sample. This property should only be used if biospecimen_class is set to \\\"Cell\\\".\",\n" +
                    "      \"icon\": \"\uD83D\uDCD6\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'species_id'\",\n" +
                    "      \"description\": \"Species identifier, resolvable by identifiers.org\",\n" +
                    "      \"icon\": \"\uD83C\uDF10\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'sample_type'->'abnormal_cell_type'->'term_label'\",\n" +
                    "      \"description\": \"Exact value according to the ontology used\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'sample_type'->'cell_line'->'term_label'\",\n" +
                    "      \"description\": \"Exact value according to the ontology used\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'phenotype'->'term_label'\",\n" +
                    "      \"description\": \"Exact value according to the ontology used\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'species_name'\",\n" +
                    "      \"description\": \"Species name according to the NCBI Taxonomy database (https://www.ncbi.nlm.nih.gov/taxonomy)\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'global_id'\",\n" +
                    "      \"description\": \"Global sample identifier, resolvable by identifiers.org\",\n" +
                    "      \"icon\": \"\uD83C\uDF10\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'sample_type'->'cell_type'->'term_label'\",\n" +
                    "      \"description\": \"Exact value according to the ontology used\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'biospecimen_class'\",\n" +
                    "      \"description\": \"Main type of structural unit to be used for classification of the sample\",\n" +
                    "      \"icon\": \"\uD83D\uDCD6\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'biospecimen_class'->'term_id'\",\n" +
                    "      \"description\": \"URL linking to an ontology term\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'local_id'\",\n" +
                    "      \"description\": \"Submitter-local identifier (within investigation/hub) for sample (in CURIE-format, if applicable)\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'sample_type'->'cell_type'->'term_id'\",\n" +
                    "      \"description\": \"URL linking to an ontology term\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'phenotype'->'@schema'\",\n" +
                    "      \"description\": \"The absolute URL of the 'current' version of the relevant FAIRtracks JSON schema within the same major version as the JSON document follows (which should ensure compatibility). Must match the value of '$id' in the linked schema\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'sample_type'->'organism_part'->'term_label'\",\n" +
                    "      \"description\": \"Exact value according to the ontology used\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'sample_type'->'cell_line'->'term_id'\",\n" +
                    "      \"description\": \"URL linking to an ontology term\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'sample_type'->'organism_part'->'term_id'\",\n" +
                    "      \"description\": \"URL linking to an ontology term\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'sample_type'\",\n" +
                    "      \"description\": \"The type of the sample\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'phenotype'\",\n" +
                    "      \"description\": \"Main phenotype (e.g. disease) connected to the sample\",\n" +
                    "      \"icon\": \"\uD83D\uDCD6\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'phenotype'->'term_id'\",\n" +
                    "      \"description\": \"URL linking to an ontology term\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'sample_type'->'organism_part'\",\n" +
                    "      \"description\": \"Part of organism (typically tissue or organ) from which the sample was taken, or cell line was derived from. This property  must be used is biospecimen_class is set to \\\"Organism Part\\\", but can also be used for the other values of biospecimen_class.\",\n" +
                    "      \"icon\": \"\uD83D\uDCD6\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'sample_type'->'abnormal_cell_type'->'term_id'\",\n" +
                    "      \"description\": \"URL linking to an ontology term\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'sample_type'->'cell_line'\",\n" +
                    "      \"description\": \"Cultured cell line used in the sample. This property should only be used if biospecimen_class is set to \\\"Cell Line\\\".\",\n" +
                    "      \"icon\": \"\uD83D\uDCD6\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'biospecimen_class'->'term_label'\",\n" +
                    "      \"description\": \"Exact value according to the ontology used\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'sample_type'->'summary'\",\n" +
                    "      \"description\": \"Summary of 'sample_type', copied from 'cell_type', 'abnormal_cell_type', 'cell_line', or 'organism_part', according to 'biospecimen_class'\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'sample_type'->'abnormal_cell_type'\",\n" +
                    "      \"description\": \"Cell type of isolated abnormal cells in the sample. This property should only be used if biospecimen_class is set to \\\"Abnormal Cell\\\".\",\n" +
                    "      \"icon\": \"\uD83D\uDCD6\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"tracks\": [\n" +
                    "    {\n" +
                    "      \"path\": \"'assembly_id'\",\n" +
                    "      \"description\": \"Genome assembly identifier, resolvable by identifiers.org. Tracks should be annotated with the lowest version of the reference genome that contains all the sequences referenced by the track. Also, GCF (Refseq) ids should be preferred to GCA (Genbank) ids\",\n" +
                    "      \"icon\": \"\uD83C\uDF10\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'file_name'\",\n" +
                    "      \"description\": \"Name of the track file\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'checksum'->'cs_hash'\",\n" +
                    "      \"description\": \"Checksum of track file, using the method described in cs_method\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'local_id'\",\n" +
                    "      \"description\": \"Submitter-local identifier (within investigation/hub) for study (in CURIE-format, if applicable)\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'checksum'->'cs_method'\",\n" +
                    "      \"description\": \"Method of checksum generation\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'global_id'\",\n" +
                    "      \"description\": \"Global track identifier, resolvable by identifiers.org [to be created by us]\",\n" +
                    "      \"icon\": \"\uD83C\uDF10\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'type_of_condensed_data'\",\n" +
                    "      \"description\": \"Type of condensed track data: Track data, by definition, is formed downstream of some data condensation process. However, the condensed data vary in form and content, technically speaking, and thus in their interpretation. Still, there is a limited set of common types of condensed track data which are able to describe the vast majority of track files\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'raw_file_ids'\",\n" +
                    "      \"description\": \"List of identifiers to raw data files used to create track (typically BAM), resolvable by identifiers.org\",\n" +
                    "      \"icon\": \"\uD83C\uDF10\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'label_long'\",\n" +
                    "      \"description\": \"A long label of the track file. Suggested maximum length is 80 characters\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'file_format'->'term_label'\",\n" +
                    "      \"description\": \"Exact value according to the ontology used\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'file_format'\",\n" +
                    "      \"description\": \"File format of the track data file\",\n" +
                    "      \"icon\": \"\uD83D\uDCD6\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'label_short'\",\n" +
                    "      \"description\": \"A short label of the track file. Suggested maximum length is 17 characters\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'geometric_track_type'\",\n" +
                    "      \"description\": \"Geometric type of track, according to the delineation of tracks into one of fifteen logical track types based upon their core informational properties (see doi:10.1186/1471-2105-12-494) \"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'file_url'\",\n" +
                    "      \"description\": \"A URL to the track data file\",\n" +
                    "      \"icon\": \"\uD83D\uDD17\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'assembly_name'\",\n" +
                    "      \"description\": \"Genome assembly name or synonym, according to the NCBI Assembly database. For tracks following UCSC-style chromosome names (e.g., \\\"chr1\\\"), the UCSC synonym should be used instead of the official name\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'experiment_ref'\",\n" +
                    "      \"description\": \"Reference to the experiment of the track (using the submitter-local identifier of the sample)\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'file_format'->'term_id'\",\n" +
                    "      \"description\": \"URL linking to an ontology term\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"doc_info\": [\n" +
                    "    {\n" +
                    "      \"path\": \"'doc_ontology_versions'->'http://purl.obolibrary.org/obo/ncit.owl'\",\n" +
                    "      \"description\": \"URL to the version of \\\"NCI Thesaurus OBO Edition\\\" used in the JSON document\",\n" +
                    "      \"icon\": \"\uD83D\uDD17\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'local_id'\",\n" +
                    "      \"description\": \"Submitter-local identifier (within  track repository) for current FAIRtracks document (in CURIE-format, if applicable)\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'doc_version'\",\n" +
                    "      \"description\": \"Version of this FAIRtracks JSON document\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'doc_ontology_versions'->'http://edamontology.org/EDAM.owl'\",\n" +
                    "      \"description\": \"URL to the version of \\\"Bioinformatics operations, data types, formats, identifiers and topics\\\" (EDAM Ontology) used in the JSON document\",\n" +
                    "      \"icon\": \"\uD83D\uDD17\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'has_augmented_metadata'\",\n" +
                    "      \"description\": \"Set to true if the metadata properties with augmented=true is set in the JSON document, as returned by the fairtracks_augment service\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'doc_ontology_versions'->'http://purl.obolibrary.org/obo/cl.owl'\",\n" +
                    "      \"description\": \"URL to the version of \\\"Cell Ontology\\\" used in the JSON document\",\n" +
                    "      \"icon\": \"\uD83D\uDD17\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'doc_ontology_versions'->'http://purl.obolibrary.org/obo/so.owl'\",\n" +
                    "      \"description\": \"URL to the version of \\\"Sequence types and features ontology\\\" used in the JSON document\",\n" +
                    "      \"icon\": \"\uD83D\uDD17\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'doc_url'\",\n" +
                    "      \"description\": \"URL to this FAIRtracks JSON document\",\n" +
                    "      \"icon\": \"\uD83D\uDD17\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'doc_ontology_versions'->'http://www.ebi.ac.uk/efo/efo.owl'\",\n" +
                    "      \"description\": \"URL to the version of \\\"Experimental Factor Ontology\\\" used in the JSON document\",\n" +
                    "      \"icon\": \"\uD83D\uDD17\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'doc_ontology_versions'->'http://purl.obolibrary.org/obo/uberon.owl'\",\n" +
                    "      \"description\": \"URL to the version of  \\\"Uber-anatomy ontology\\\" used in the JSON document\",\n" +
                    "      \"icon\": \"\uD83D\uDD17\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'doc_date'\",\n" +
                    "      \"description\": \"Creation date of this version of this FAIRtracks document\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'doc_ontology_versions'->'http://purl.obolibrary.org/obo/obi.owl'\",\n" +
                    "      \"description\": \"URL to the version of \\\"Ontology for Biomedical Investigations\\\" used in the JSON document\",\n" +
                    "      \"icon\": \"\uD83D\uDD17\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'doc_ontology_versions'\",\n" +
                    "      \"description\": \"URLs to the version of the ontologies used in the JSON document\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"studies\": [\n" +
                    "    {\n" +
                    "      \"path\": \"'study_name'\",\n" +
                    "      \"description\": \"Name of the study\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'contact'->'name'\",\n" +
                    "      \"description\": \"Name of contact person/organization\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'contact'->'@schema'\",\n" +
                    "      \"description\": \"The absolute URL of the 'current' version of the relevant FAIRtracks JSON schema within the same major version as the JSON document follows (which should ensure compatibility). Must match the value of '$id' in the linked schema\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'local_id'\",\n" +
                    "      \"description\": \"Submitter-local identifier (within the track collection) for the study\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'contact'\",\n" +
                    "      \"description\": \"Contact information for study\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'global_id'\",\n" +
                    "      \"description\": \"Global study identifier, resolvable by identifiers.org\",\n" +
                    "      \"icon\": \"\uD83C\uDF10\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'collection_ref'\",\n" +
                    "      \"description\": \"Reference to the track collection containing the study (using the submitter-local identifier of the collection)\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'contact'->'orcid'\",\n" +
                    "      \"description\": \"ORCID to contact person\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'publication'\",\n" +
                    "      \"description\": \"Pubmed identifier (dataset or publication)\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'contact'->'e-mail'\",\n" +
                    "      \"description\": \"E-mail to contact person/organization\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"collection_info\": [\n" +
                    "    {\n" +
                    "      \"path\": \"'local_id'\",\n" +
                    "      \"description\": \"Submitter-local identifier (within track repository) for the collection\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'description_url'\",\n" +
                    "      \"description\": \"URL to a web page or file describing the track collection\",\n" +
                    "      \"icon\": \"\uD83D\uDD17\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'contact'->'name'\",\n" +
                    "      \"description\": \"Name of contact person/organization\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'contact'\",\n" +
                    "      \"description\": \"Contact information for the track collection\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'short_name'\",\n" +
                    "      \"description\": \"Short name of the track collection. Suggested maximum length is 17 characters\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'long_name'\",\n" +
                    "      \"description\": \"Long name of the track collection. Suggested maximum length is 80 characters\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'contact'->'@schema'\",\n" +
                    "      \"description\": \"The absolute URL of the 'current' version of the relevant FAIRtracks JSON schema within the same major version as the JSON document follows (which should ensure compatibility). Must match the value of '$id' in the linked schema\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'orig_metadata_url'\",\n" +
                    "      \"description\": \"URL to track collection metadata in its original form (might contain more than the relevant metadata)\",\n" +
                    "      \"icon\": \"\uD83D\uDD17\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'contact'->'orcid'\",\n" +
                    "      \"description\": \"ORCID to contact person\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'source_repo_url'\",\n" +
                    "      \"description\": \"URL to the track repository containing the collection (e.g., the Track Hub Registry)\",\n" +
                    "      \"icon\": \"\uD83D\uDD17\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'doc_ref'\",\n" +
                    "      \"description\": \"Reference to the JSON document containing the study (using the  identifier of the document)\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"path\": \"'contact'->'e-mail'\",\n" +
                    "      \"description\": \"E-mail to contact person/organization\"\n" +
                    "    }\n" +
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
        Map<String, Collection<SchemaService.Attribute>> attributes = schemaService.getAttributes();
        String actual = gson.toJson(attributes);
        assertEquals(ATTRIBUTES, actual);
    }

}
