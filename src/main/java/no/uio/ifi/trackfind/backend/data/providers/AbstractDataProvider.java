package no.uio.ifi.trackfind.backend.data.providers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.lucene.DirectoryFactory;
import no.uio.ifi.trackfind.backend.services.VersioningService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
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
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.SerializationUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static no.uio.ifi.trackfind.TrackFindApplication.INDICES_FOLDER;

/**
 * Abstract class for all data providers.
 * Implements some common logic like Lucene Directory initialization, getting metamodel, searching, etc.
 *
 * @author Dmytro Titov
 */
@Slf4j
public abstract class AbstractDataProvider implements DataProvider, Comparable<DataProvider> {

    private static final String SEPARATOR = ">";
    private static final String ID = ADVANCED + SEPARATOR + "id";
    private static final String DATASET = "dataset";

    private Analyzer analyzer = new KeywordAnalyzer();

    private IndexReader indexReader;
    private IndexSearcher searcher;
    private Directory directory;

    private DirectoryFactory directoryFactory;
    private VersioningService versioningService;
    protected ExecutorService executorService;
    protected Gson gson;

    /**
     * Initialize Lucene Directory (Index) and the Searcher over this Directory.
     *
     * @throws Exception If initialization fails.
     */
    @SuppressWarnings("unused")
    @PostConstruct
    private void postConstruct() throws Exception {
        directory = directoryFactory.getDirectory(getPath());
        if (DirectoryReader.indexExists(directory)) {
            reinitIndexSearcher();
        } else {
            crawlRemoteRepository();
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
    public String getPath() {
        return INDICES_FOLDER + getName() + "/";
    }

    /**
     * Gets attributes to skip during indexing.
     *
     * @return Collection of unneeded attributes.
     */
    protected Collection<String> getAttributesToSkip() {
        return Collections.emptySet();
    }

    /**
     * Gets attributes to hide in the tree.
     *
     * @return Collection of hidden attributes.
     */
    protected Collection<String> getAttributesToHide() {
        return Collections.emptySet();
    }

    /**
     * Gets values to skip during indexing.
     *
     * @return Collection of unneeded values.
     */
    protected Collection<String> getValuesToSkip() {
        return Collections.emptySet();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<String> getUrlsFromDataset(String query, Map dataset) {
        Set<String> dataTypes = extractDataTypesFromQuery(query);
        Collection<String> urls = new HashSet<>();
        Map<String, Collection<String>> browser = (Map<String, Collection<String>>) dataset.get(DATA_URL_ATTRIBUTE);
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
            Query parsedQuery = new AnalyzingQueryParser("", analyzer).parse(query);
            parsedQuery = parsedQuery.rewrite(indexReader);
            Weight weight = parsedQuery.createWeight(searcher, false);
            Set<Term> terms = new HashSet<>();
            weight.extractTerms(terms);
            return terms.stream().filter(t -> t.field().endsWith(DATA_TYPE_ATTRIBUTE)).map(Term::text).collect(Collectors.toSet());
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
    public synchronized void crawlRemoteRepository() {
        log.info("Fetching data using " + getName());
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        try (IndexWriter indexWriter = new IndexWriter(directory, config)) {
            fetchData(indexWriter);
            indexWriter.flush();
            indexWriter.commit();
            versioningService.commitAllChanges(VersioningService.Operation.CRAWLING, getName());
            versioningService.tag(getName());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
        reinitIndexSearcher();
        log.info("Success!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void applyMappings() {
        log.info("Applying mappings for " + getName());
        Collection<String> basicAttributes = MultiFields.getIndexedFields(indexReader).stream().filter(f -> f.startsWith(BASIC + SEPARATOR)).collect(Collectors.toSet());
        Bits liveDocs = MultiFields.getLiveDocs(indexReader);
        Map<String, String> attributesMapping = loadConfiguration().getAttributesMapping();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
        try (IndexWriter indexWriter = new IndexWriter(directory, config)) {
            for (int i = 0; i < indexReader.maxDoc(); i++) {
                if (liveDocs != null && !liveDocs.get(i)) {
                    continue;
                }
                Document document = indexReader.document(i);
                basicAttributes.forEach(document::removeField);
                for (Map.Entry<String, String> mapping : attributesMapping.entrySet()) {
                    Set<String> values = new HashSet<>();
                    values.addAll(Arrays.asList(document.getValues(mapping.getKey())));
                    values.add(document.get(mapping.getKey()));
                    values.remove(null);
                    for (String value : values) {
                        document.add(new StringField(BASIC + SEPARATOR + mapping.getValue(), value, Field.Store.YES));
                    }
                }
                indexWriter.updateDocument(new Term(ID, document.get(ID)), document);
            }
            indexWriter.flush();
            indexWriter.commit();
            versioningService.commitAllChanges(VersioningService.Operation.REMAPPING, getName());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
        reinitIndexSearcher();
        log.info("Success!");
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
        Map<String, Collection<String>> browser = (Map<String, Collection<String>>) dataset.get(DATA_URL_ATTRIBUTE);
        if (browser == null) {
            log.error("'browser' field is 'null' for dataset!");
        } else {
            dataset.put(DATA_TYPE_ATTRIBUTE, new HashSet<>(browser.keySet()));
        }
    }

    /**
     * Reinitialize Directory Reader and Searcher (in case of Directory update).
     */
    private synchronized void reinitIndexSearcher() {
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
            Collection<String> attributesToHide = getAttributesToHide();
            for (String fieldName : fieldNames) {
                if (attributesToHide.contains(fieldName)) {
                    continue;
                }
                Map<String, Object> metamodel = result;
                String[] path = fieldName.split(SEPARATOR);
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
            Collection<String> attributesToHide = getAttributesToHide();
            for (String fieldName : fieldNames) {
                if (attributesToHide.contains(fieldName)) {
                    continue;
                }
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
    @SuppressWarnings("unchecked")
    @Override
    public Collection<Map<String, Object>> search(String query, int limit) {
        try {
            Query parsedQuery = new AnalyzingQueryParser("", analyzer).parse(query);
            TopDocs topDocs = searcher.search(parsedQuery, limit == 0 ? Integer.MAX_VALUE : limit);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            Collection<Map<String, Object>> result = new HashSet<>();
            for (ScoreDoc scoreDoc : scoreDocs) {
                result.add((Map<String, Object>) SerializationUtils.deserialize(searcher.doc(scoreDoc.doc).getBinaryValue(DATASET).bytes));
            }
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Configuration loadConfiguration() {
        try {
            String json = FileUtils.readFileToString(new File(getPath() + "." + getName()), Charset.defaultCharset());
            return gson.fromJson(json, Configuration.class);
        } catch (IOException e) {
            log.info(e.getMessage());
            return new Configuration();
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void saveConfiguration(Configuration configuration) {
        try {
            FileUtils.write(new File(getPath() + "." + getName()), gson.toJson(configuration), Charset.defaultCharset());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
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
        convertDatasetToDocument(document, dataset, SEPARATOR + ADVANCED);
        document.add(new StringField(ID, UUID.randomUUID().toString(), Field.Store.YES));
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
                convertDatasetToDocument(document, value, path + SEPARATOR + key);
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
            if (StringUtils.isEmpty(value) || getValuesToSkip().stream().anyMatch(value::contains)) {
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
    public void setVersioningService(VersioningService versioningService) {
        this.versioningService = versioningService;
    }

    @Autowired
    public void setDirectoryFactory(DirectoryFactory directoryFactory) {
        this.directoryFactory = directoryFactory;
    }

    @Autowired
    public void setExecutorService(ExecutorService workStealingPool) {
        this.executorService = workStealingPool;
    }

    @Autowired
    public void setGson(Gson gson) {
        this.gson = gson;
    }

}
