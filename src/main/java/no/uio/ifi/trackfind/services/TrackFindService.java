package no.uio.ifi.trackfind.services;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.internal.LinkedTreeMap;
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
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Service
public class TrackFindService {

    public static final String DATASET = "dataset";

    private static final String DATASETS = "datasets";
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final String PATH_SEPARATOR = ">";

    private Analyzer analyzer = new KeywordAnalyzer();
    private Directory index = new RAMDirectory();
    private Multimap<String, String> metamodel = HashMultimap.create();

    private final Collection<DataProvider> dataProviders;

    @Autowired
    public TrackFindService(Collection<DataProvider> dataProviders) {
        this.dataProviders = dataProviders;
    }

    @PostConstruct
    public void postConstruct() throws Exception {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(index, config);

        for (DataProvider dataProvider : dataProviders) {
            LinkedTreeMap grid = dataProvider.fetchData();
            LinkedTreeMap datasets = (LinkedTreeMap) grid.get(DATASETS);
            for (Object dataset : datasets.values()) {
                processDataset(indexWriter, (LinkedTreeMap) dataset);
            }
        }

        indexWriter.close();
    }

    public Multimap<String, String> getMetamodel() {
        return metamodel;
    }

    // Sample query: "sample_id: SRS306625_*_471 AND other_attributes>lab: U??D AND ihec_data_portal>assay: (WGB-Seq OR something)"
    public Collection<Document> search(String query) throws IOException, ParseException {
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        Query parsedQuery = new AnalyzingQueryParser("", analyzer).parse(query);
        TopDocs topDocs = searcher.search(parsedQuery, Integer.MAX_VALUE);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        Collection<Document> result = new HashSet<>();
        for (ScoreDoc scoreDoc : scoreDocs) {
            result.add(searcher.doc(scoreDoc.doc));
        }
        return result;
    }

    @SuppressWarnings("ConstantConditions")
    private void processDataset(IndexWriter indexWriter, LinkedTreeMap dataset) throws IOException {
        Document document = new Document();
        convertDatasetToDocument(document, dataset, "");
        document.add(new StoredField(DATASET, new BytesRef(SerializationUtils.serialize(dataset))));
        indexWriter.addDocument(document);
    }

    private void convertDatasetToDocument(Document document, Object object, String path) {
        if (object instanceof LinkedTreeMap) {
            Set keySet = ((LinkedTreeMap) object).keySet();
            for (Object key : keySet) {
                Object value = ((LinkedTreeMap) object).get(key);
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
