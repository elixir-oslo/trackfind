package no.uio.ifi.trackfind.backend.data.providers.encode;

import com.google.common.collect.HashMultimap;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.AbstractDataProvider;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * Data Provider for ENCODE.
 *
 * @author Dmytro Titov
 */
@Slf4j
@Component
public class ENCODEDataProvider extends AbstractDataProvider {

    private static final String FETCH_URL = "https://www.encodeproject.org/search/?type=%s&limit=all&format=json";
    private static final Collection<String> AVAILABLE_TYPES = Arrays.asList("Annotation", "AntibodyLot", "Award",
            "Biosample", "BiosampleType", "Experiment", "File", "Gene", "Lab", "Library", "Organism", "Pipeline",
            "Platform", "Project", "Publication", "Reference", "ReferenceEpigenome", "Software", "Source", "Target",
            "Treatment", "User");

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void fetchData(String hubName) {
        HashMultimap<String, String> mapToSave = HashMultimap.create();
        for (String type : AVAILABLE_TYPES) {
            log.info("Processing type: {}", type);
            try (InputStream inputStream = new URL(String.format(FETCH_URL, type)).openStream();
                 InputStreamReader reader = new InputStreamReader(inputStream)) {
                Map all = gson.fromJson(reader, Map.class);
                Collection<Map> objects = (Collection<Map>) all.get("@graph");
                for (Map object : objects) {
                    mapToSave.put(hubName + "_" + type, gson.toJson(object));
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            log.info("{} processed", type);
        }
        save(hubName, mapToSave.asMap());
    }

}
