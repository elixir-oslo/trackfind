package no.uio.ifi.trackfind.backend.data.providers.encode;

import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.AbstractDataProvider;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
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

    private static final String FETCH_URL = "https://www.encodeproject.org/search/?type=experiment&limit=all&format=json";

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void fetchData(String hubName) {
//        try (InputStream inputStream = new URL(FETCH_URL).openStream();
//             InputStreamReader reader = new InputStreamReader(inputStream)) {
//            Map all = gson.fromJson(reader, Map.class);
//            Collection<Map> datasets = (Collection<Map>) all.get("@graph");
//            save(hubName, datasets);
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//        }
    }

}
