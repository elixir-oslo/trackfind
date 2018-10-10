package no.uio.ifi.trackfind.backend.services;

import no.uio.ifi.trackfind.backend.dao.Dataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.function.Function;

@Service
public class GSuiteService implements Function<Collection<Dataset>, String> {

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
        HttpEntity<Collection<Dataset>> request = new HttpEntity<>(datasets);
        return restTemplate.postForObject("http://gsuite/togsuite", request, String.class);
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

}
