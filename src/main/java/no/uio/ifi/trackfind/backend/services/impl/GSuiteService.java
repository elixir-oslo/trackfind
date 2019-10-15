package no.uio.ifi.trackfind.backend.services.impl;

import com.google.gson.Gson;
import no.uio.ifi.trackfind.backend.pojo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.function.Function;

/**
 * Service for querying remote GSuite service for performing JSON to GSuite conversion.
 */
// TODO: cover with tests
@Service
public class GSuiteService implements Function<Collection<SearchResult>, String> {

    private Gson gson;
    private SchemaService schemaService;
    private RestTemplate loadBalancedRestTemplate;

    /**
     * Convert searchResults to GSuite.
     *
     * @param searchResults Datasets to convert.
     * @return GSuite string.
     */
    @Cacheable(value = "gsuite", sync = true)
    @Override
    public String apply(Collection<SearchResult> searchResults) {
//        sortJSON(searchResults);
        HttpEntity<Collection<SearchResult>> request = new HttpEntity<>(searchResults);
        return loadBalancedRestTemplate.postForObject("http://gsuite/togsuite", request, String.class);
    }

//    protected void sortJSON(Collection<SearchResult> searchResults) {
//        for (SearchResult searchResult : searchResults) {
//            searchResult.setContent(sortJSON(searchResult.getContent()));
//        }
//    }
//
//    protected String sortJSON(String fairContent) {
//        Map mapContent = gson.fromJson(fairContent, Map.class);
//        mapContent = sortMap("", mapContent);
//        return gson.toJson(mapContent);
//    }
//
//    @SuppressWarnings("unchecked")
//    protected Map sortMap(String path, Map mapContent) {
//        TreeMap sortedMap = new TreeMap((o1, o2) -> {
//            String key1 = path + o1.toString();
//            String key2 = path + o2.toString();
//            List<String> attributes = schemaService.getAttributes();
//            String fullKey1 = attributes.stream().filter(a -> a.startsWith(key1)).findFirst().orElse("");
//            String fullKey2 = attributes.stream().filter(a -> a.startsWith(key2)).findFirst().orElse("");
//            if (StringUtils.isEmpty(fullKey1) || StringUtils.isEmpty(fullKey2)) {
//                if (StringUtils.isEmpty(fullKey1) && StringUtils.isEmpty(fullKey2)) {
//                    return fullKey1.compareTo(fullKey2);
//                } else {
//                    return Long.compare(fullKey1.length(), fullKey2.length());
//                }
//            } else {
//                long index1 = attributes.indexOf(fullKey1);
//                long index2 = attributes.indexOf(fullKey2);
//                return Long.compare(index1, index2);
//            }
//        });
//        for (Object key : mapContent.keySet()) {
//            Object value = mapContent.get(key);
//            if (value instanceof Map) {
//                sortedMap.put(key, sortMap(path + key + properties.getLevelsSeparator(), (Map) value));
//            } else {
//                sortedMap.put(key, value);
//            }
//        }
//        return sortedMap;
//    }

    @Autowired
    public void setGson(Gson gson) {
        this.gson = gson;
    }

    @Autowired
    public void setSchemaService(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @Autowired
    public void setLoadBalancedRestTemplate(RestTemplate loadBalancedRestTemplate) {
        this.loadBalancedRestTemplate = loadBalancedRestTemplate;
    }

}
