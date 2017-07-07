package no.uio.ifi.trackfind.services;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.data.providers.DataProvider;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.analyzing.AnalyzingQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TrackFindService {

    private static final String INDICES_FOLDER = "indices";

    private static final String DATASET = "dataset";
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final String PATH_SEPARATOR = ">";

    private Analyzer analyzer;
    private Directory index;
    private IndexReader indexReader;
    private IndexSearcher searcher;
    private Multimap<String, String> metamodel;

    private final Collection<DataProvider> dataProviders;

    @Autowired
    public TrackFindService(Collection<DataProvider> dataProviders) throws IOException {
        this.dataProviders = dataProviders;
        this.analyzer = new KeywordAnalyzer();
        this.index = FSDirectory.open(new File(INDICES_FOLDER).toPath());
        this.metamodel = HashMultimap.create();
    }

    @PostConstruct
    public void postConstruct() throws Exception {
        if (DirectoryReader.indexExists(index)) {
            reinitIndexSearcher();
        } else {
            updateIndex();
        }
    }

    public synchronized void updateIndex() {
        log.info("Fetching and indexing data...");
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        try (IndexWriter indexWriter = new IndexWriter(index, config)) {
            for (DataProvider dataProvider : dataProviders) {
                indexWriter.addDocuments(dataProvider.fetchData().stream().map(this::processDataset).collect(Collectors.toSet()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
        reinitIndexSearcher();
        log.info("Success");
    }

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
            indexReader = DirectoryReader.open(index);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return;
        }
        searcher = new IndexSearcher(indexReader);
    }

    public Multimap<String, String> getMetamodel() {
        return metamodel;
    }

    // Sample query: "sample_id: SRS306625_*_471 AND other_attributes>lab: U??D AND ihec_data_portal>assay: (WGB-Seq OR something)"
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

    @SuppressWarnings("ConstantConditions")
    private Document processDataset(Map dataset) {
        Document document = new Document();
        convertDatasetToDocument(document, dataset, "");
        document.add(new StoredField(DATASET, new BytesRef(SerializationUtils.serialize(dataset))));
        return document;
    }

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
            if (!value.contains(HTTP) && !value.contains(HTTPS)) {
                metamodel.put(attribute, value);
            }
            document.add(new StringField(attribute, value, Field.Store.YES));
        }
    }


}
