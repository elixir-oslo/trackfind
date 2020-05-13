package no.uio.ifi.trackfind.backend.services.impl;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.pojo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for querying remote GSuite service for performing JSON to GSuite conversion.
 */
@Slf4j
@Service
public class GSuiteService {

    private Gson gson;
    private SchemaService schemaService;
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
    public String apply(Collection<SearchResult> searchResults, String[] attributes) {
        if (attributes.length != 0) {
            filterAttributes(searchResults, attributes);
        }
        HttpEntity<Collection<SearchResult>> request = new HttpEntity<>(searchResults);
        return loadBalancedRestTemplate.postForObject("http://gsuite/togsuite", request, String.class);
    }

    protected void filterAttributes(Collection<SearchResult> searchResults, String[] attributes) {
        try {
            Set<String> categoriesToKeep = Arrays.stream(attributes).map(a -> a.split(separator)[0].replace(".content", "")).collect(Collectors.toSet());
            for (SearchResult searchResult : searchResults) {
                Set<String> categories = new HashSet<>(searchResult.getContent().keySet());
                for (String category : categories) {
                    if (!categoriesToKeep.contains(category)) {
                        searchResult.getContent().remove(category);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
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
    public void setLoadBalancedRestTemplate(RestTemplate loadBalancedRestTemplate) {
        this.loadBalancedRestTemplate = loadBalancedRestTemplate;
    }

}
