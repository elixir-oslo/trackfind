package no.uio.ifi.trackfind.backend.data.providers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import no.uio.ifi.trackfind.backend.converters.DocumentToMapConverter;
import no.uio.ifi.trackfind.backend.converters.MapToDocumentConverter;
import no.uio.ifi.trackfind.backend.lucene.DirectoryFactory;
import no.uio.ifi.trackfind.backend.services.VersioningService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.analyzing.AnalyzingQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;

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

    private Analyzer analyzer = new KeywordAnalyzer();

    private IndexReader indexReader;
    private IndexSearcher searcher;
    private Directory directory;

    private DirectoryFactory directoryFactory;
    private VersioningService versioningService;

    protected TrackFindProperties properties;
    protected MapToDocumentConverter mapToDocumentConverter;
    protected DocumentToMapConverter documentToMapConverter;
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
     * Gets attributes to hide in the tree.
     *
     * @return Collection of hidden attributes.
     */
    protected Collection<String> getAttributesToHide() {
        return Collections.emptySet();
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
            commit(VersioningService.Operation.CRAWLING);
            tag();
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
        Collection<String> indexedFields = MultiFields.getIndexedFields(indexReader);
        Collection<String> basicAttributes = indexedFields.stream().filter(f -> f.startsWith(properties.getMetamodel().getBasicSectionName() + properties.getMetamodel().getLevelsSeparator())).collect(Collectors.toSet());
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
                basicAttributes.forEach(document::removeFields);
                for (Map.Entry<String, String> mapping : attributesMapping.entrySet()) {
                    Set<String> values = new HashSet<>();
                    String sourceAttribute = mapping.getKey();
                    values.addAll(Arrays.asList(document.getValues(sourceAttribute)));
                    values.add(document.get(sourceAttribute));
                    values.remove(null);
                    if (CollectionUtils.isEmpty(values)) { // no values found - let's use nested attributes as values
                        values = indexedFields.stream().
                                filter(f -> f.startsWith(sourceAttribute)).
                                map(f -> f.replace(sourceAttribute, "")).
                                filter(StringUtils::isNotEmpty).
                                map(f -> f.split(properties.getMetamodel().getLevelsSeparator())[1]).
                                collect(Collectors.toSet());
                    }
                    for (String value : values) {
                        document.add(new StringField(properties.getMetamodel().getBasicSectionName() + properties.getMetamodel().getLevelsSeparator() + mapping.getValue(), value, Field.Store.YES));
                    }
                }
                indexWriter.updateDocument(new Term(properties.getMetamodel().getAdvancedIdAttribute(), document.get(properties.getMetamodel().getAdvancedIdAttribute())), document);
            }
            indexWriter.flush();
            indexWriter.commit();
            commit(VersioningService.Operation.REMAPPING);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
        reinitIndexSearcher();
        log.info("Success!");
    }

    /**
     * Commit changes.
     *
     * @param operation Operation to commit.
     * @throws GitAPIException In case of Git error.
     */
    protected void commit(VersioningService.Operation operation) throws GitAPIException {
        versioningService.commitAllChanges(operation, getName());
    }


    /**
     * Tag current revision (HEAD, hopefully).
     *
     * @throws GitAPIException In case of Git error.
     */
    protected void tag() throws GitAPIException {
        versioningService.tag(getName());
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
        // do some post-processing
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
                String[] path = fieldName.split(properties.getMetamodel().getLevelsSeparator());
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
    @Override
    public Collection<Document> search(String query, int limit) {
        try {
            Query parsedQuery = new AnalyzingQueryParser("", analyzer).parse(query);
            TopDocs topDocs = searcher.search(parsedQuery, limit == 0 ? Integer.MAX_VALUE : limit);
            return Arrays.stream(topDocs.scoreDocs).map(scoreDoc -> {
                try {
                    return searcher.doc(scoreDoc.doc);
                } catch (IOException e) {
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toSet());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> fetch(String documentId) {
        Collection<Document> documents = search(properties.getMetamodel().getAdvancedIdAttribute() + ": " + documentId, 1);
        if (CollectionUtils.isEmpty(documents)) {
            return null;
        }
        Map map = documentToMapConverter.apply(documents.iterator().next());
        ((Map) map.get(properties.getMetamodel().getAdvancedSectionName())).remove(properties.getMetamodel().getIdAttribute());
        return (Map) map.get(properties.getMetamodel().getAdvancedSectionName());
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
    public void setProperties(TrackFindProperties properties) {
        this.properties = properties;
    }

    @Autowired
    public void setMapToDocumentConverter(MapToDocumentConverter mapToDocumentConverter) {
        this.mapToDocumentConverter = mapToDocumentConverter;
    }

    @Autowired
    public void setDocumentToMapConverter(DocumentToMapConverter documentToMapConverter) {
        this.documentToMapConverter = documentToMapConverter;
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
