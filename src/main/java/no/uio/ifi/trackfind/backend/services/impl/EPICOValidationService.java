package no.uio.ifi.trackfind.backend.services.impl;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.pojo.TfObjectType;
import no.uio.ifi.trackfind.backend.pojo.TfVersion;
import no.uio.ifi.trackfind.backend.repositories.VersionRepository;
import no.uio.ifi.trackfind.backend.services.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Integration with EPICO JSON validation service.
 *
 * @author Dmytro Titov
 */
@Slf4j
@Service
@Transactional
public class EPICOValidationService implements ValidationService {

    public static final String EPICO_VALIDATION_URL = "http://fairtracks.bsc.es/api/validate";

    private JdbcTemplate jdbcTemplate;
    private MetamodelService metamodelService;
    private VersionRepository versionRepository;
    private RestTemplate restTemplate;
    private Gson gson;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public String validate(String repository, String hubName) {
        Map<String, Object> hubContent = new HashMap<>();
        Collection<TfObjectType> objectTypes = metamodelService.getObjectTypes(repository, hubName);
        for (TfObjectType objectType : objectTypes) {
            List<Map<String, Object>> result = jdbcTemplate.queryForList("SELECT content from tf_objects WHERE object_type_id = " + objectType.getId());
            List<Map> results = result.stream().map(e -> e.values().iterator().next()).map(e -> gson.fromJson(String.valueOf(e), Map.class)).collect(Collectors.toList());
            hubContent.put(objectType.getName(), results);
        }
        hubContent.put("@schema", SchemaService.SCHEMA_URL);
        TfVersion version = objectTypes.iterator().next().getVersion();
        try {
            String resultJSON = restTemplate.postForObject(EPICO_VALIDATION_URL, hubContent, String.class);
            Map<String, Object> result = gson.fromJson(resultJSON, Map.class);
            version.setValidation(Boolean.TRUE.toString().equalsIgnoreCase(String.valueOf(result.get("validated"))));
            versionRepository.save(version);
            return gson.toJson(result);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            version.setValidation(false);
            versionRepository.save(version);
            return "Validation error: " + e.getMessage();
        }
    }

    @Autowired
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Autowired
    public void setMetamodelService(MetamodelService metamodelService) {
        this.metamodelService = metamodelService;
    }

    @Autowired
    public void setVersionRepository(VersionRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Autowired
    public void setGson(Gson gson) {
        this.gson = gson;
    }

}
