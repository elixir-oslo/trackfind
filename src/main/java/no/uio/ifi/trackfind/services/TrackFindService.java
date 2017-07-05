package no.uio.ifi.trackfind.services;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Service
public class TrackFindService {

    private final Gson gson;

    private Directory index;
    private Multimap<String, String> metamodel;

    @Autowired
    public TrackFindService(Gson gson) {
        this.gson = gson;
    }

    @PostConstruct
    public void postConstruct() throws Exception {
        metamodel = HashMultimap.create();

        LinkedTreeMap grid = loadGrid();

        StandardAnalyzer analyzer = new StandardAnalyzer();
        index = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(index, config);

        LinkedTreeMap datasets = (LinkedTreeMap) grid.get("datasets");
        for (Object dataset : datasets.values()) {
            processDataset(indexWriter, (LinkedTreeMap) dataset);
        }

        indexWriter.close();
    }

    public Multimap<String, String> getMetamodel() {
        return metamodel;
    }

    public Collection<Document> search(String attribute, String value) throws IOException, ParseException {
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs topDocs = searcher.search(new TermQuery(new Term(attribute, value)), Integer.MAX_VALUE);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        Collection<Document> result = new HashSet<>();
        for (ScoreDoc scoreDoc : scoreDocs) {
            result.add(searcher.doc(scoreDoc.doc));
        }
        return result;
    }

    private void processDataset(IndexWriter indexWriter, LinkedTreeMap dataset) throws IOException {
        Document document = new Document();
        convertDatasetToDocument(document, dataset, "");
        indexWriter.addDocument(document);
    }

    private void convertDatasetToDocument(Document document, Object object, String path) {
        if (object instanceof LinkedTreeMap) {
            Set keySet = ((LinkedTreeMap) object).keySet();
            for (Object key : keySet) {
                Object value = ((LinkedTreeMap) object).get(key);
                convertDatasetToDocument(document, value, path + ">" + key);
            }
        } else if (object instanceof String) {
            String attribute = path.substring(1);
            String value = (String) object;
            if (!value.contains("http://") && !value.contains("https://")) {
                metamodel.put(attribute, value);
            }
            document.add(new StringField(attribute, value, Field.Store.YES));
        }
    }

    private LinkedTreeMap loadGrid() throws FileNotFoundException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("getDataHub.json").getFile());
        JsonReader reader = new JsonReader(new FileReader(file));
        return gson.fromJson(reader, Object.class);
    }


}
