package no.uio.ifi.trackfind.backend.services;

import no.uio.ifi.trackfind.backend.dao.Dataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service for querying remote GSuite service for performing JSON to GSuite conversion.
 */
// TODO: cover with tests
@Service
public class GSuiteService implements Function<Collection<Dataset>, String> {

    private RestTemplate restTemplate;
    private SchemaService schemaService;

    /**
     * Convert datasets to GSuite.
     *
     * @param datasets Datasets to convert.
     * @return GSuite string.
     */
    @SuppressWarnings("unchecked")
    @Override
    public String apply(Collection<Dataset> datasets) {
        Map<String, Object> body = new HashMap<>();
        body.put("attributes", schemaService.getAttributes());
        body.put("datasets", datasets);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body);
        return restTemplate.postForObject("http://gsuite/togsuite", request, String.class);
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Autowired
    public void setSchemaService(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

}
