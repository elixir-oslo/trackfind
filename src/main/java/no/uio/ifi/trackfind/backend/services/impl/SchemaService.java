package no.uio.ifi.trackfind.backend.services.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.everit.json.schema.*;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Service for loading JSON schema.
 */
@Slf4j
@Service
public class SchemaService {

    protected String schemaLocation;
    protected String separator;

    private Schema schema;
    private Multimap<String, String> attributes = HashMultimap.create();

    @Autowired
    public SchemaService(@Value("${trackfind.schema-location}") String schemaLocation,
                         @Value("${trackfind.separator}") String separator) {
        this.schemaLocation = schemaLocation;
        this.separator = separator;
        try (InputStream inputStream = new URL(schemaLocation).openStream()) {
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            this.schema = SchemaLoader.load(rawSchema);
            gatherAttributes(null, "", schema);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void gatherAttributes(String objectType, String path, Schema schema) {
        if (schema instanceof ObjectSchema) {
            Map<String, Schema> propertySchemas = ((ObjectSchema) schema).getPropertySchemas();
            Set<Map.Entry<String, Schema>> entries = propertySchemas.entrySet();
            if (CollectionUtils.isNotEmpty(entries)) {
                for (Map.Entry<String, Schema> entry : entries) {
                    if (StringUtils.isNotEmpty(objectType)) {
                        gatherAttributes(objectType,
                                path + separator + "'" + entry.getKey() + "'",
                                entry.getValue());
                    } else {
                        gatherAttributes(entry.getKey(),
                                path,
                                entry.getValue());
                    }
                }
            }
            return;
        }
        if (schema instanceof ArraySchema) {
            Schema allItemSchema = ((ArraySchema) schema).getAllItemSchema();
            if (allItemSchema != null) {
                gatherAttributes(objectType, path, allItemSchema);
            }
            Schema containedItemSchema = ((ArraySchema) schema).getContainedItemSchema();
            if (containedItemSchema != null) {
                gatherAttributes(objectType, path, containedItemSchema);
            }
            return;
        }
        if (schema instanceof CombinedSchema) {
            Collection<Schema> subschemas = ((CombinedSchema) schema).getSubschemas();
            if (CollectionUtils.isNotEmpty(subschemas)) {
                for (Schema subschema : subschemas) {
                    gatherAttributes(objectType, path, subschema);
                }
            }
            return;
        }
        if (schema instanceof ReferenceSchema) {
            gatherAttributes(objectType, path, ((ReferenceSchema) schema).getReferredSchema());
            return;
        }
        if (StringUtils.isNotEmpty(objectType) && Character.isLetterOrDigit(objectType.charAt(0))) {
            int separatorLength = separator.length();
            String attribute = path.isEmpty() ? path : path.substring(separatorLength);
            if (StringUtils.isNoneEmpty(attribute)) {
                attributes.put(objectType, attribute);
            }
        }
    }

    public String getSchemaLocation() {
        return schemaLocation;
    }

    public Schema getSchema() {
        return schema;
    }

    /**
     * Returns attributes from JSON schema.
     *
     * @return Collection of attributes.
     */
    public Map<String, Collection<String>> getAttributes() {
        return Collections.unmodifiableMap(attributes.asMap());
    }

}
