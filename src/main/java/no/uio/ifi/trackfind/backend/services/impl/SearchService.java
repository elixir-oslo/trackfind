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
import org.springframework.cache.annotation.Cacheable;
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

    @SuppressWarnings("ConstantConditions")
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
    @Cacheable(value = "search", sync = true)
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

        String fullQueryString = buildSearchQuery(repository, hub, references, objectTypesFromReferences, new HashSet<>(objectTypesToSelect), query, limit, false);
        return executeSearchQuery(fullQueryString);
    }

    /**
     * Counts entries returned by provided query.
     *
     * @param repository Repository name.
     * @param hub        Track TfHub name.
     * @param query      Search query.
     * @param categories Comma-separated categories.
     * @return Count of entries.
     */
    @Cacheable(value = "count", sync = true)
    public int count(String repository, String hub, String query, Collection<String> categories) throws SQLException {
        Collection<TfReference> references = metamodelService.getReferences(repository, hub);

        Collection<TfObjectType> objectTypesFromReferences = new HashSet<>();
        references.forEach(r -> objectTypesFromReferences.addAll(Arrays.asList(r.getFromObjectType(), r.getToObjectType())));

        Collection<String> objectTypesToSelect;
        if (CollectionUtils.isEmpty(categories)) {
            objectTypesToSelect = objectTypesFromReferences.stream().map(TfObjectType::getName).collect(Collectors.toSet());
        } else {
            objectTypesToSelect = categories;
        }

        String fullQueryString = buildSearchQuery(repository, hub, references, objectTypesFromReferences, new HashSet<>(objectTypesToSelect), query, 0, true);
        return executeCountQuery(fullQueryString);
    }

    protected String buildSearchQuery(String repository,
                                      String hub,
                                      Collection<TfReference> references,
                                      Collection<TfObjectType> objectTypesFromReferences,
                                      Collection<String> objectTypeNamesToSelect,
                                      String query,
                                      long limit,
                                      boolean count) {
        // temporary WA
        objectTypeNamesToSelect.add("doc_info");
        objectTypeNamesToSelect.add("collection_info");

        StringBuilder fullQuery = new StringBuilder("SELECT ");

        if (!count) {
            addDistinctClause(objectTypeNamesToSelect, fullQuery);
        } else {
            fullQuery.append("COUNT(*) ");
        }

        fullQuery.append("\nFROM ");

        if (CollectionUtils.isNotEmpty(objectTypeNamesToSelect)) {
            for (String objectTypeName : objectTypeNamesToSelect) {
                fullQuery.append("tf_current_objects ").append(objectTypeName).append(", ");
            }
        } else if (CollectionUtils.isNotEmpty(objectTypesFromReferences)) {
            for (TfObjectType objectType : objectTypesFromReferences) {
                fullQuery.append("tf_current_objects ").append(objectType.getName()).append(", ");
            }
        }

        if (fullQuery.toString().endsWith(", ")) {
            fullQuery.setLength(fullQuery.length() - 2);
        }
        fullQuery.append("\nWHERE ");

        if (CollectionUtils.isNotEmpty(objectTypeNamesToSelect)) {
            for (String objectTypeName : objectTypeNamesToSelect) {
                Optional<TfObjectType> objectType = metamodelService.findObjectTypeByName(repository, hub, objectTypeName);
                objectType.ifPresent(ot -> fullQuery.append(ot.getName()).append(".object_type_id = ").append(ot.getId()).append(" AND "));
            }
        } else if (CollectionUtils.isNotEmpty(objectTypesFromReferences)) {
            for (TfObjectType objectType : objectTypesFromReferences) {
                fullQuery.append(objectType.getName()).append(".object_type_id = ").append(objectType.getId()).append(" AND ");
            }
        }

        fullQuery.append("\n");

        for (TfReference reference : references) {
            String fromObjectType = reference.getFromObjectType().getName();
            String toObjectType = reference.getToObjectType().getName();
            if (CollectionUtils.isNotEmpty(objectTypeNamesToSelect)) {
                if (!objectTypeNamesToSelect.contains(fromObjectType) || !objectTypeNamesToSelect.contains(toObjectType)) {
                    continue;
                }
            }
            String fromAttribute = reference.getFromAttribute();
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

    private void addDistinctClause(Collection<String> objectTypeNamesToSelect, StringBuilder fullQuery) {
        fullQuery.append("DISTINCT ");

        for (String objectTypeName : objectTypeNamesToSelect) {
            fullQuery.append(objectTypeName).append(".content ").append("\"").append(objectTypeName).append("_content\", ");
            fullQuery.append(objectTypeName).append(".id ").append(objectTypeName).append("_id, ");
        }

        fullQuery.setLength(fullQuery.length() - 2);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
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
                searchResult.getContent().put(objectTypeName.replace("_content", ""), new HashMap(gson.fromJson(json, Map.class)));
            }
            results.add(searchResult);
        }
        return Pair.of(ids, results);
    }

    protected int executeCountQuery(String fullQueryString) throws SQLException {
        log.info("Executing count query: {}", fullQueryString);
        PreparedStatement preparedStatement = connection.prepareStatement(fullQueryString);
        ResultSet resultSet = preparedStatement.executeQuery();
        resultSet.next();
        return resultSet.getInt(1);
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
