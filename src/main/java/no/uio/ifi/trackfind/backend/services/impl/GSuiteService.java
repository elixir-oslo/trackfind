package no.uio.ifi.trackfind.backend.services.impl;

import alexh.weak.Dynamic;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.pojo.SearchResult;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for querying remote GSuite service for performing JSON to GSuite conversion.
 */
@Slf4j
@Service
public class GSuiteService {

    private RestTemplate loadBalancedRestTemplate;

    @Value("${trackfind.separator}")
    protected String separator;

    /**
     * Convert searchResults to GSuite.
     *
     * @param searchResults Datasets to convert.
     * @param attributes    Attributes to keep (the rest is discarded).
     * @return GSuite string.
     */
    @Cacheable(value = "gsuite", sync = true)
    public String apply(Collection<SearchResult> searchResults, String attributes) {
        HttpEntity<Collection<SearchResult>> request;
        if (!StringUtils.isEmpty(attributes)) {
            request = new HttpEntity<>(filterAttributes(searchResults, attributes.split(",")));
        } else {
            request = new HttpEntity<>(searchResults);
        }
        return loadBalancedRestTemplate.postForObject("http://gsuite/togsuite", request, String.class);
    }

    protected Collection<SearchResult> filterAttributes(Collection<SearchResult> searchResults, String[] attributes) {
        try {
            ArrayList<SearchResult> newSearchResults = new ArrayList<>();
            Set<String> categoriesToKeep = Arrays.stream(attributes).map(a -> a.split(separator)[0].replace(".content", "")).collect(Collectors.toSet());
            for (SearchResult searchResult : searchResults) {
                SearchResult newSearchResult = new SearchResult();
                newSearchResults.add(newSearchResult);
                for (String category : categoriesToKeep) {
                    newSearchResult.getContent().put(category, new HashMap(searchResult.getContent().get(category)));
                }
                for (String category : categoriesToKeep) {
                    Map oldMap = searchResult.getContent().get(category);
                    HashMap newMap = new HashMap();
                    newSearchResult.getContent().put(category, newMap);
                    for (String attribute : attributes) {
                        if (!attribute.startsWith(category)) {
                            continue;
                        }
                        attribute = attribute.replace(category + ".content" + separator, "").replace("'", "");
                        Collection<String> values;
                        Dynamic dynamicValues = Dynamic.from(oldMap).get(attribute, separator);
                        if (dynamicValues.isPresent()) {
                            if (dynamicValues.isList()) {
                                values = dynamicValues.asList();
                            } else {
                                values = Collections.singletonList(dynamicValues.asString());
                            }
                        } else {
                            values = Collections.emptyList();
                        }
                        String[] path = attribute.split(separator);
                        putValueByPath(newMap, path, values);
                    }
                }
            }
            return newSearchResults;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    protected void putValueByPath(Map<String, Object> standardMap, String[] path, Collection<String> values) {
        Map<String, Object> nestedMap = standardMap;
        for (int i = 0; i < path.length - 1; i++) {
            nestedMap = (Map<String, Object>) nestedMap.computeIfAbsent(path[i], k -> new HashMap<String, Object>());
        }
        if (CollectionUtils.size(values) == 1) {
            nestedMap.put(path[path.length - 1], values.iterator().next());
        } else {
            nestedMap.put(path[path.length - 1], values);
        }
    }

    @Autowired
    public void setLoadBalancedRestTemplate(RestTemplate loadBalancedRestTemplate) {
        this.loadBalancedRestTemplate = loadBalancedRestTemplate;
    }

}
