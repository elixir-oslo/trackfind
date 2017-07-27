package no.uio.ifi.trackfind.backend.data.providers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.lucene.DirectoryFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.analyzing.AnalyzingQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.SerializationUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Abstract class for all data providers.
 * Implements some common logic like Lucene Directory initialization, getting metamodel, searching, etc.
 */
@Slf4j
public abstract class AbstractDataProvider implements DataProvider, Comparable<DataProvider> {

    private static final String DATASET = "dataset";
    private static final String PATH_SEPARATOR = ">";
    private static final List<String> SKIP_TERMS = Arrays.asList("://", "CHECK");

    private Analyzer analyzer = new KeywordAnalyzer();

    private IndexReader indexReader;
    private IndexSearcher searcher;
    private Directory directory;

    private DirectoryFactory directoryFactory;

    /**
     * Initialize Lucene Directory (Index) and the Searcher over this Directory.
     *
     * @throws Exception If initialization fails.
     */
    @PostConstruct
    private void postConstruct() throws Exception {
        directory = directoryFactory.getDirectory(this.getClass().getSimpleName());
        if (DirectoryReader.indexExists(directory)) {
            reinitIndexSearcher();
        } else {
            updateIndex();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return getClass().getSimpleName().replace("DataProvider", "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void updateIndex() {
        log.info("Fetching data using " + getClass().getSimpleName());
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        try (IndexWriter indexWriter = new IndexWriter(directory, config)) {
            Collection<Map> data = fetchData();
            indexWriter.addDocuments(data.stream().map(this::processDataset).collect(Collectors.toSet()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
        reinitIndexSearcher();
        log.info("Success");
    }

    /**
     * Reinitialize Directory Reader and Searcher (in case of Directory update).
     */
    private void reinitIndexSearcher() {
        if (indexReader != null) {
            try {
                indexReader.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return;
            }
        }
        try {
            indexReader = DirectoryReader.open(directory);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return;
        }
        searcher = new IndexSearcher(indexReader);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMetamodelTree() {
        Map<String, Object> result = new HashMap<>();
        try {
            Collection<String> fieldNames = MultiFields.getIndexedFields(indexReader);
            Fields fields = MultiFields.getFields(indexReader);
            for (String fieldName : fieldNames) {
                Map<String, Object> metamodel = result;
                String[] path = fieldName.split(PATH_SEPARATOR);
                for (int i = 0; i < path.length - 1; i++) {
                    String attribute = path[i];
                    metamodel = (Map<String, Object>) metamodel.computeIfAbsent(attribute, k -> new HashMap<String, Object>());
                }
                String valuesKey = path[path.length - 1];
                Collection<String> values = (Collection<String>) metamodel.computeIfAbsent(valuesKey, k -> new HashSet<String>());
                Terms terms = fields.terms(fieldName);
                TermsEnum iterator = terms.iterator();
                BytesRef next = iterator.next();
                while (next != null) {
                    String value = next.utf8ToString();
                    if (SKIP_TERMS.stream().noneMatch(value::contains)) {
                        values.add(value);
                    }
                    next = iterator.next();
                }
                if (CollectionUtils.isEmpty(values)) {
                    metamodel.remove(valuesKey);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Multimap<String, String> getMetamodelFlat() {
        Multimap<String, String> metamodel = HashMultimap.create();
        try {
            Collection<String> fieldNames = MultiFields.getIndexedFields(indexReader);
            Fields fields = MultiFields.getFields(indexReader);
            for (String fieldName : fieldNames) {
                Terms terms = fields.terms(fieldName);
                TermsEnum iterator = terms.iterator();
                BytesRef next = iterator.next();
                while (next != null) {
                    String value = next.utf8ToString();
                    if (SKIP_TERMS.stream().noneMatch(value::contains)) {
                        metamodel.put(fieldName, value);
                    }
                    next = iterator.next();
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return metamodel;
    }

    /**
     * {@inheritDoc}
     */
    // Sample query: "sample_id: SRS306625_*_471 OR other_attributes>lab: U??D AND ihec_data_portal>assay: (WGB-Seq OR something)"
    @Override
    public Collection<Map> search(String query) {
        try {
            Query parsedQuery = new AnalyzingQueryParser("", analyzer).parse(query);
            TopDocs topDocs = searcher.search(parsedQuery, Integer.MAX_VALUE);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            Collection<Map> result = new HashSet<>();
            for (ScoreDoc scoreDoc : scoreDocs) {
                result.add((Map) SerializationUtils.deserialize(searcher.doc(scoreDoc.doc).getBinaryValue(DATASET).bytes));
            }
            return result;
        } catch (IOException | ParseException e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Convert dataset from Map to Document.
     *
     * @param dataset Dataset as a Map.
     * @return Dataset as a Document.
     */
    @SuppressWarnings("ConstantConditions")
    private Document processDataset(Map dataset) {
        Document document = new Document();
        convertDatasetToDocument(document, dataset, "");
        document.add(new StoredField(DATASET, new BytesRef(SerializationUtils.serialize(dataset))));
        return document;
    }

    /**
     * Recursive implementation of Map to Document conversion: field by field, taking care of nesting.
     *
     * @param document Result Document.
     * @param object   Either inner Map or value.
     * @param path     Path to the current entry (sequence of attributes).
     */
    private void convertDatasetToDocument(Document document, Object object, String path) {
        if (object instanceof Map) {
            Set keySet = ((Map) object).keySet();
            for (Object key : keySet) {
                Object value = ((Map) object).get(key);
                convertDatasetToDocument(document, value, path + PATH_SEPARATOR + key);
            }
        } else if (object instanceof String) {
            String attribute = path.substring(1);
            String value = (String) object;
            document.add(new StringField(attribute, value, Field.Store.YES));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(DataProvider that) {
        return this.getClass().getSimpleName().compareTo(that.getClass().getSimpleName());
    }

    @Autowired
    public void setDirectoryFactory(DirectoryFactory directoryFactory) {
        this.directoryFactory = directoryFactory;
    }

}
