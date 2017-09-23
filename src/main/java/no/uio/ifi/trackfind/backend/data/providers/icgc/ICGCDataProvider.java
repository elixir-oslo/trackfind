package no.uio.ifi.trackfind.backend.data.providers.icgc;

import alexh.weak.Dynamic;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.PaginationAwareDataProvider;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fetches data from <a href="http://docs.icgc.org/">ICGC</a>.
 *
 * @author Dmytro Titov
 */
@Slf4j
@Component
public class ICGCDataProvider extends PaginationAwareDataProvider { // TODO: Fetch more data.

    private static final String DONORS = "https://dcc.icgc.org/api/v1/donors?";
    private static final String SUBMIT = "https://dcc.icgc.org/api/v1/download/submit?filters={%22donor%22:{%22id%22:{%22is%22:[%22__DONOR_ID__%22]}}}&info=[{%22key%22:%22__DATA_TYPE__%22,%22value%22:%22TSV%22}]";
    private static final String DOWNLOAD = "https://dcc.icgc.org/api/v1/download/";
    private static final String AVAILABLE_DATA_TYPES = "availableDataTypes";

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void fetchData(IndexWriter indexWriter) throws Exception {
        log.info("Fetching donors...");
        fetchPages(indexWriter, DONORS, DONORS, ICGCPage.class);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void postProcessPage(Collection<Map> page) {
        for (Map dataset : page) {
            Map<String, Collection<String>> browser = new HashMap<>();
            String donorId = (String) dataset.get("id");
            Collection<String> availableDataTypes = (Collection<String>) dataset.get(AVAILABLE_DATA_TYPES);
            for (String dataType : availableDataTypes) {
                browser.computeIfAbsent(dataType, k -> new HashSet<>()).add(SUBMIT.replace("__DONOR_ID__", donorId).replace("__DATA_TYPE__", dataType));
            }
            dataset.put(properties.getMetamodel().getBrowserAttribute(), browser);
            dataset.remove(AVAILABLE_DATA_TYPES);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override // hack for ICGC's "two-steps download"
    public Collection<Document> search(String query, int limit) {
        String separator = properties.getMetamodel().getLevelsSeparator();
        String advancedURL = properties.getMetamodel().getAdvancedSectionName() + separator + properties.getMetamodel().getDataURLAttribute();
        return super.search(query, limit).parallelStream().map(documentToMapConverter).map(Dynamic::from).map(dynamic -> {
            String submitURL = dynamic.get(advancedURL, separator).asString();
            try (InputStream inputStream = new URL(submitURL).openStream();
                 InputStreamReader reader = new InputStreamReader(inputStream)) {
                Download download = gson.fromJson(reader, Download.class);
                dynamic.get(properties.getMetamodel().getAdvancedSectionName(), separator).asMap().put(properties.getMetamodel().getDataURLAttribute(), DOWNLOAD + download.getDownloadId());
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return null;
            }
            return dynamic.asMap();
        }).filter(Objects::nonNull).map(mapToDocumentConverter).collect(Collectors.toSet());
    }

}
