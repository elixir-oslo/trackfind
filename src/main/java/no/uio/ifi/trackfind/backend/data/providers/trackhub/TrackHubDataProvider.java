package no.uio.ifi.trackfind.backend.data.providers.trackhub;

import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.annotations.VersionedComponent;
import no.uio.ifi.trackfind.backend.data.providers.AbstractDataProvider;
import org.apache.commons.collections4.MapUtils;
import org.apache.lucene.index.IndexWriter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Draft of Data Provider for TrackHub
 *
 * @author Dmytro Titov
 */
@Slf4j
@VersionedComponent
public class TrackHubDataProvider extends AbstractDataProvider {

    private static final String FETCH_URL = "https://hyperbrowser.uio.no/hb/static/hyperbrowser/files/trackfind/blueprint_hub.json";

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void fetchData(IndexWriter indexWriter) {
        try (InputStream inputStream = new URL(FETCH_URL).openStream();
             InputStreamReader reader = new InputStreamReader(inputStream)) {
            Map document = gson.fromJson(reader, Map.class);
            Map hitsMap = MapUtils.getMap(document, "hits");
            Collection<Map> hits = (Collection<Map>) hitsMap.get("hits");
            Collection<Map> datasets = new HashSet<>();
            for (Map hit : hits) {
                Map source = MapUtils.getMap(hit, "_source");
                Collection<Map> data = (Collection<Map>) source.get("data");
                datasets.addAll(data);
            }
            indexWriter.addDocuments(datasets.parallelStream().map(this::postprocessDataset).map(mapToDocumentConverter).collect(Collectors.toSet()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
