package no.uio.ifi.trackfind.backend.services.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
import java.util.*;

/**
 * Service for loading JSON schema.
 */
@Slf4j
@Service
public class SchemaService {

    protected String schemaLocation;
    protected String separator;

    private final Schema schema;
    private final Map<String, String> categories = new HashMap<>();
    private final Multimap<String, Attribute> attributes = HashMultimap.create();

    @Autowired
    public SchemaService(@Value("${trackfind.schema-location}") String schemaLocation,
                         @Value("${trackfind.separator}") String separator) {
        this.schemaLocation = schemaLocation;
        this.separator = separator;
        try (InputStream inputStream = new URL(schemaLocation).openStream()) {
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            this.schema = SchemaLoader.load(rawSchema);
            gatherAttributes(null, "", this.schema);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void gatherAttributes(String objectType, String path, Schema schema) {
        if (StringUtils.isNotEmpty(objectType) && Character.isLetterOrDigit(objectType.charAt(0))) {
            int separatorLength = separator.length();
            String attribute = path.isEmpty() ? path : path.substring(separatorLength);
            String description = schema.getDescription();
            if (StringUtils.isNoneEmpty(attribute) && Character.isLetterOrDigit(attribute.charAt(1)) && StringUtils.isNoneEmpty(description)) {
                String icon = null;
                Map<String, Object> unprocessedProperties = schema.getUnprocessedProperties();
                if (unprocessedProperties.containsKey("ontologyTermPair")) {
                    icon = "📖";
                }
                attributes.put(objectType, new Attribute(attribute, description, icon));
            }
        }
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
                        categories.put(entry.getKey(), entry.getValue().getDescription());
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
    }

    public String getSchemaLocation() {
        return schemaLocation;
    }

    public Schema getSchema() {
        return schema;
    }

    /**
     * Returns categories from JSON schema.
     *
     * @return Map of category names to their descriptions.
     */
    public Map<String, String> getCategories() {
        return categories;
    }

    /**
     * Returns attributes from JSON schema.
     *
     * @return Collection of attributes with their description.
     */
    public Map<String, Collection<Attribute>> getAttributes() {
        return Collections.unmodifiableMap(attributes.asMap());
    }

    @EqualsAndHashCode
    @AllArgsConstructor
    @Data
    public static class Attribute {
        private String path;
        private String description;
        private String icon;
    }

}
