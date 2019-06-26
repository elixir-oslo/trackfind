package no.uio.ifi.trackfind.backend.services;

import com.google.gson.Gson;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import org.apache.commons.collections.MapUtils;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service for loading JSON schema and for validating JSON objects.
 */
// TODO: cover with tests
@Slf4j
@Service
public class SchemaService {

    private TrackFindProperties properties;

    private org.everit.json.schema.Schema schema;
    private List<String> attributes;

    @Autowired
    @SuppressWarnings("unchecked")
    public SchemaService(TrackFindProperties properties) {
        this.properties = properties;
        try {
            try (InputStreamReader inputStreamReader = new InputStreamReader(getClass().getResourceAsStream("/schema.json"))) {
                Map<String, Object> schemaMap = new Gson().fromJson(inputStreamReader, Map.class);
                attributes = new ArrayList<>();
                gatherAttributes("", schemaMap);
            }
            try (InputStream inputStream = getClass().getResourceAsStream("/schema.json")) {
                JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
                this.schema = SchemaLoader.load(rawSchema);
            }
            attributes = Collections.unmodifiableList(attributes);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    @HystrixCommand
    private void gatherAttributes(String path, Map object) {
        String type = String.valueOf(object.get("type"));
        if ("object".equals(type)) {
            for (Object properties : MapUtils.getMap(object, "properties").entrySet()) {
                Map.Entry<String, Map> entry = (Map.Entry<String, Map>) properties;
                gatherAttributes(path + this.properties.getLevelsSeparator() + entry.getKey(), entry.getValue());
            }
        } else if ("array".equals(type)) {
            for (Object properties : MapUtils.getMap(MapUtils.getMap(object, "items"), "properties").entrySet()) {
                Map.Entry<String, Map> entry = (Map.Entry<String, Map>) properties;
                gatherAttributes(path + this.properties.getLevelsSeparator() + entry.getKey(), entry.getValue());
            }
        } else {
            attributes.add(path.substring(this.properties.getLevelsSeparator().length()));
        }
    }

    /**
     * Returns attributes from JSON schema.
     *
     * @return Collection of attributes.
     */
    @HystrixCommand
    public List<String> getAttributes() {
        return attributes;
    }

    /**
     * Validates JSON object against the schema. Throws the exception if the object is invalid.
     *
     * @param object Object to validate.
     */
    @HystrixCommand
    public void validate(Object object) {
        this.schema.validate(object);
    }

}
