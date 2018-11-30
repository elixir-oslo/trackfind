package no.uio.ifi.trackfind.backend.services;

import com.google.gson.Gson;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Service for loading JSON schema and for validating JSON objects.
 */
// TODO: cover with tests
@Service
public class SchemaService {

    private TrackFindProperties properties;

    private org.everit.json.schema.Schema schema;
    private Map<String, Object> schemaMap;
    private Set<String> attributes;

    @Autowired
    @SuppressWarnings("unchecked")
    public SchemaService(TrackFindProperties properties) throws IOException {
        this.properties = properties;
        try (InputStreamReader inputStreamReader = new InputStreamReader(getClass().getResourceAsStream("/schema.json"))) {
            this.schemaMap = new Gson().fromJson(inputStreamReader, Map.class);
            attributes = new HashSet<>();
            gatherAttributes((Map<String, Object>) ((Map) schemaMap.get("properties")).get(properties.getFairFieldName()));
        }
        try (InputStream inputStream = getClass().getResourceAsStream("/schema.json")) {
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            this.schema = SchemaLoader.load(rawSchema);
        }
        attributes = Collections.unmodifiableSet(attributes);
    }

    @SuppressWarnings("unchecked")
    private void gatherAttributes(Map object) {
        String type = String.valueOf(object.get("type"));
        if ("object".equals(type)) {
            for (Object value : ((Map) object.get("properties")).values()) {
                gatherAttributes((Map) value);
            }
        } else {
            String path = String.valueOf(object.get("$id"));
            attributes.add(path
                    .replace("#/", "")
                    .replace("/", properties.getLevelsSeparator())
                    .replace("properties" + properties.getLevelsSeparator(), "")
                    .replace(properties.getFairFieldName() + properties.getLevelsSeparator(), ""));
        }
    }

    /**
     * Returns attributes from JSON schema.
     *
     * @return Collection of attributes.
     */
    public Collection<String> getAttributes() {
        return attributes;
    }

    /**
     * Validates JSON object against the schema. Throws the exception if the object is invalid.
     *
     * @param object Object to validate.
     */
    public void validate(Object object) {
        this.schema.validate(object);
    }

}
