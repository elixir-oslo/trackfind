package no.uio.ifi.trackfind.backend.services.impl;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.pojo.Queries;
import no.uio.ifi.trackfind.backend.pojo.SearchResult;
import no.uio.ifi.trackfind.backend.pojo.TfObjectType;
import no.uio.ifi.trackfind.backend.pojo.TfReference;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to perform JSONB-oriented search for datasets in the database.
 */
@Slf4j
@Service
public class SearchService {

    @Value("${trackfind.separator}")
    protected String separator;

    private String jdbcUrl;

    private JdbcTemplate jdbcTemplate;
    private MetamodelService metamodelService;
    private Gson gson;

    private Connection connection;

    @PostConstruct
    private void init() throws SQLException {
        if (jdbcTemplate.queryForObject(Queries.CHECK_SEARCH_USER_EXISTS, Integer.TYPE) == 0) {
            jdbcTemplate.execute(Queries.CREATE_SEARCH_USER);
        }
        connection = DriverManager.getConnection(jdbcUrl, "search", "search");
    }

    /**
     * Searches for entries using provided query.
     *
     * @param repository Repository name.
     * @param hub        Track TfHub name.
     * @param query      Search query.
     * @param categories Comma-separated categories.
     * @param limit      Max number of entries to return. 0 for unlimited.
     * @return Found entries with set of IDs.
     */
    public Pair<Set<Long>, Collection<SearchResult>> search(String repository, String hub, String query, Collection<String> categories, long limit) throws SQLException {
        Collection<TfReference> references = metamodelService.getReferences(repository, hub);

        Collection<TfObjectType> objectTypesFromReferences = new HashSet<>();
        references.forEach(r -> objectTypesFromReferences.addAll(Arrays.asList(r.getFromObjectType(), r.getToObjectType())));

        Collection<String> objectTypesToSelect;
        if (CollectionUtils.isEmpty(categories)) {
            objectTypesToSelect = objectTypesFromReferences.stream().map(TfObjectType::getName).collect(Collectors.toSet());
        } else {
            objectTypesToSelect = categories;
        }

        String fullQueryString = buildSearchQuery(references, objectTypesFromReferences, objectTypesToSelect, query, limit);
        return executeSearchQuery(fullQueryString);
    }

    protected String buildSearchQuery(Collection<TfReference> references, Collection<TfObjectType> objectTypesFromReferences, Collection<String> objectTypesToSelect, String query, long limit) {
        StringBuilder fullQuery = new StringBuilder("SELECT DISTINCT ");

        for (String objectTypeName : objectTypesToSelect) {
            fullQuery.append(objectTypeName).append(".content ").append(objectTypeName).append("_content, ");
            fullQuery.append(objectTypeName).append(".id ").append(objectTypeName).append("_id, ");
        }

        fullQuery.setLength(fullQuery.length() - 2);
        fullQuery.append("\nFROM ");

        if (CollectionUtils.isEmpty(objectTypesFromReferences) && CollectionUtils.isNotEmpty(objectTypesToSelect)) {
            for (String objectTypeName : objectTypesToSelect) {
                fullQuery.append("tf_current_objects ").append(objectTypeName).append(", ");
            }
        } else {
            for (TfObjectType objectType : objectTypesFromReferences) {
                fullQuery.append("tf_current_objects ").append(objectType.getName()).append(", ");
            }
        }

        if (fullQuery.toString().endsWith(", ")) {
            fullQuery.setLength(fullQuery.length() - 2);
        }
        fullQuery.append("\nWHERE ");

        for (TfObjectType objectType : objectTypesFromReferences) {
            fullQuery.append(objectType.getName()).append(".object_type_id = ").append(objectType.getId()).append(" AND ");
        }

        fullQuery.append("\n");

        for (TfReference reference : references) {
            String fromObjectType = reference.getFromObjectType().getName();
            String fromAttribute = reference.getFromAttribute();
            String toObjectType = reference.getToObjectType().getName();
            String toAttribute = reference.getToAttribute();
            fullQuery
                    .append(fromObjectType)
                    .append(".content ")
                    .append(separator)
                    .append(" ")
                    .append(fromAttribute)
                    .append(" = ")
                    .append(toObjectType)
                    .append(".content ")
                    .append(separator)
                    .append(" ")
                    .append(toAttribute)
                    .append(" AND ");
        }

        fullQuery.append("\n");

        fullQuery.append(query);

        if (limit != 0) {
            fullQuery.append(" LIMIT ").append(limit);
        }

        return fullQuery.toString().replaceAll("\\?", "\\?\\?");
    }

    protected Pair<Set<Long>, Collection<SearchResult>> executeSearchQuery(String fullQueryString) throws SQLException {
        log.info("Executing search query: {}", fullQueryString);
        PreparedStatement preparedStatement = connection.prepareStatement(fullQueryString);
        ResultSet resultSet = preparedStatement.executeQuery();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        Collection<String> objectTypesToSelect = new HashSet<>();
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            if (columnName.endsWith("_content")) {
                objectTypesToSelect.add(columnName);
            }
        }
        Set<Long> ids = new HashSet<>();
        Collection<SearchResult> results = new ArrayList<>();
        while (resultSet.next()) {
            SearchResult searchResult = new SearchResult();
            for (String objectTypeName : objectTypesToSelect) {
                String json = resultSet.getString(objectTypeName);
                ids.add(resultSet.getLong(objectTypeName.replace("_content", "_id")));
                searchResult.getContent().put(objectTypeName.replace("_content", ""), gson.fromJson(json, Map.class));
            }
            results.add(searchResult);
        }
        return Pair.of(ids, results);
    }

    @Value("${spring.datasource.url}")
    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
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
    public void setGson(Gson gson) {
        this.gson = gson;
    }

}
