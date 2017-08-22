package no.uio.ifi.trackfind.backend.data.providers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.lucene.DirectoryFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.analyzing.AnalyzingQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.SerializationUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Abstract class for all data providers.
 * Implements some common logic like Lucene Directory initialization, getting metamodel, searching, etc.
 *
 * @author Dmytro Titov
 */
@Slf4j
public abstract class AbstractDataProvider implements DataProvider, Comparable<DataProvider> {

    private static final String INDICES_FOLDER = "indices/";
    private static final String DATASET = "dataset";
    private static final String PATH_SEPARATOR = ">";

    protected static final String BROWSER = "browser";
    protected static final String DATA_TYPE = "data_type";

    private Analyzer analyzer = new KeywordAnalyzer();

    private IndexReader indexReader;
    private IndexSearcher searcher;
    private Directory directory;

    private DirectoryFactory directoryFactory;
    protected ExecutorService executorService;

    /**
     * Initialize Lucene Directory (Index) and the Searcher over this Directory.
     *
     * @throws Exception If initialization fails.
     */
    @SuppressWarnings("unused")
    @PostConstruct
    private void postConstruct() throws Exception {
        directory = directoryFactory.getDirectory(INDICES_FOLDER + getName() + getClass().getPackage().getImplementationVersion());
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
     * Gets attributes to skip during indexing. Basically hides this attributes from the tree.
     *
     * @return Collection of unneeded attributes.
     */
    protected Collection<String> getAttributesToSkip() {
        return Collections.singletonList(BROWSER);
    }

    /**
     * Gets values to skip during indexing. Basically hides this values from the tree.
     *
     * @return Collection of unneeded values.
     */
    protected Collection<String> getValuesToSkip() {
        return Arrays.asList("://", "CHECK");
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<String> getUrlsFromDataset(String query, Map dataset) {
        Set<String> dataTypes = extractDataTypesFromQuery(query);
        Collection<String> urls = new HashSet<>();
        Map<String, Collection<String>> browser = (Map<String, Collection<String>>) dataset.get(BROWSER);
        if (CollectionUtils.isNotEmpty(dataTypes)) {
            dataTypes.forEach(dt -> urls.addAll(browser.getOrDefault(dt, Collections.emptySet())));
        } else {
            urls.addAll(browser.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()));
        }
        return urls;
    }

    /**
     * Parses Lucene Query to get 'data type' terms from it.
     *
     * @param query Query used for finding this dataset.
     * @return Data types.
     */
    private Set<String> extractDataTypesFromQuery(String query) {
        try {
            Query parsedQuery = new AnalyzingQueryParser("", analyzer).parse(query.replace("*", "").replace("?", "")); // remove wildcard characters as they are not supported in createWeight()
            Weight weight = parsedQuery.createWeight(searcher, false);
            Set<Term> terms = new HashSet<>();
            weight.extractTerms(terms);
            return terms.stream().filter(t -> t.field().equals(DATA_TYPE)).map(Term::text).collect(Collectors.toSet());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Collections.emptySet();
        }
    }

    /**
     * Fetches data from the repository.
     *
     * @param indexWriter Handler to write to te Lucene Index.
     * @throws Exception in case of some problems.
     */
    protected abstract void fetchData(IndexWriter indexWriter) throws Exception;

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void updateIndex() {
        log.info("Fetching data using " + getName());
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        try (IndexWriter indexWriter = new IndexWriter(directory, config)) {
            fetchData(indexWriter);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
        reinitIndexSearcher();
        log.info("Success");
    }

    /**
     * Datasets post-processing.
     *
     * @param datasets Fetched datasets to process.
     */
    protected void postProcessDatasets(Collection<Map> datasets) {
        datasets.forEach(this::postProcessDataset);
    }

    /**
     * Dataset post-processing.
     *
     * @param dataset Fetched dataset to process.
     */
    @SuppressWarnings("unchecked")
    protected void postProcessDataset(Map dataset) {
        Map<String, Collection<String>> browser = (Map<String, Collection<String>>) dataset.get(BROWSER);
        if (browser == null) {
            log.error("'browser' field is 'null' for dataset!");
        } else {
            dataset.put(DATA_TYPE, new HashSet<>(browser.keySet()));
        }
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
        } catch (Exception e) {
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
                Collection<String> values = (Collection<String>) metamodel.computeIfAbsent(valuesKey, k -> new HashSet<>());
                Terms terms = fields.terms(fieldName);
                TermsEnum iterator = terms.iterator();
                BytesRef next = iterator.next();
                while (next != null) {
                    String value = next.utf8ToString();
                    values.add(value);
                    next = iterator.next();
                }
                if (CollectionUtils.isEmpty(values)) {
                    metamodel.remove(valuesKey);
                }
            }
        } catch (Exception e) {
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
                    metamodel.put(fieldName, value);
                    next = iterator.next();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return metamodel;
    }

    /**
     * {@inheritDoc}
     */
    // Sample query: "sample_id: SRS306625_*_471 OR other_attributes>lab: U??D AND ihec_data_portal>assay: (WGB-Seq OR something)"
    @Override
    public Collection<Map> search(String query, int limit) {
        try {
            Query parsedQuery = new AnalyzingQueryParser("", analyzer).parse(query);
            TopDocs topDocs = searcher.search(parsedQuery, limit == 0 ? Integer.MAX_VALUE : limit);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            Collection<Map> result = new HashSet<>();
            for (ScoreDoc scoreDoc : scoreDocs) {
                result.add((Map) SerializationUtils.deserialize(searcher.doc(scoreDoc.doc).getBinaryValue(DATASET).bytes));
            }
            return result;
        } catch (Exception e) {
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
    protected Document convertDatasetToDocument(Map dataset) {
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
        } else if (object instanceof Collection) {
            Collection values = (Collection) object;
            for (Object value : values) {
                convertDatasetToDocument(document, value, path);
            }
        } else if (object != null) {
            String attribute = path.substring(1);
            if (getAttributesToSkip().stream().anyMatch(attribute::contains)) {
                return;
            }
            String value = String.valueOf(object);
            if (getValuesToSkip().stream().anyMatch(value::contains)) {
                return;
            }
            document.add(new StringField(attribute, value, Field.Store.YES));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(DataProvider that) {
        return this.getName().compareTo(that.getName());
    }

    @Autowired
    public void setDirectoryFactory(DirectoryFactory directoryFactory) {
        this.directoryFactory = directoryFactory;
    }

    @Autowired
    public void setExecutorService(ExecutorService workStealingPool) {
        this.executorService = workStealingPool;
    }

}
