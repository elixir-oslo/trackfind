package no.uio.ifi.trackfind.backend.services;

import com.google.gson.Gson;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import no.uio.ifi.trackfind.backend.dao.Dataset;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Service for querying remote GSuite service for performing JSON to GSuite conversion.
 */
// TODO: cover with tests
@Service
public class GSuiteService implements Function<Collection<Dataset>, String> {

    private TrackFindProperties properties;
    private Gson gson;
    private SchemaService schemaService;
    private RestTemplate restTemplate;

    /**
     * Convert datasets to GSuite.
     *
     * @param datasets Datasets to convert.
     * @return GSuite string.
     */
    @SuppressWarnings("unchecked")
    @Override
    public String apply(Collection<Dataset> datasets) {
        sortJSON(datasets);
        HttpEntity<Collection<Dataset>> request = new HttpEntity<>(datasets);
        return restTemplate.postForObject("http://gsuite/togsuite", request, String.class);
    }

    protected void sortJSON(Collection<Dataset> datasets) {
        for (Dataset dataset : datasets) {
            dataset.setFairContent(sortJSON(dataset.getFairContent()));
        }
    }

    protected String sortJSON(String fairContent) {
        Map mapContent = gson.fromJson(fairContent, Map.class);
        mapContent = sortMap("", mapContent);
        return gson.toJson(mapContent);
    }

    @SuppressWarnings("unchecked")
    protected Map sortMap(String path, Map mapContent) {
        TreeMap sortedMap = new TreeMap((o1, o2) -> {
            String key1 = path + o1.toString();
            String key2 = path + o2.toString();
            List<String> attributes = schemaService.getAttributes();
            String fullKey1 = attributes.stream().filter(a -> a.startsWith(key1)).findFirst().orElse("");
            String fullKey2 = attributes.stream().filter(a -> a.startsWith(key2)).findFirst().orElse("");
            if (StringUtils.isEmpty(fullKey1) || StringUtils.isEmpty(fullKey2)) {
                if (StringUtils.isEmpty(fullKey1) && StringUtils.isEmpty(fullKey2)) {
                    return fullKey1.compareTo(fullKey2);
                } else {
                    return Long.compare(fullKey1.length(), fullKey2.length());
                }
            } else {
                long index1 = attributes.indexOf(fullKey1);
                long index2 = attributes.indexOf(fullKey2);
                return Long.compare(index1, index2);
            }
        });
        for (Object key : mapContent.keySet()) {
            Object value = mapContent.get(key);
            if (value instanceof Map) {
                sortedMap.put(key, sortMap(path + key + properties.getLevelsSeparator(), (Map) value));
            } else {
                sortedMap.put(key, value);
            }
        }
        return sortedMap;
    }

    @Autowired
    public void setProperties(TrackFindProperties properties) {
        this.properties = properties;
    }

    @Autowired
    public void setGson(Gson gson) {
        this.gson = gson;
    }

    @Autowired
    public void setSchemaService(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

}
