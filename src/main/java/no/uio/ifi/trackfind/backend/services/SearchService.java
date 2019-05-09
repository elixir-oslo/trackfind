package no.uio.ifi.trackfind.backend.services;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import no.uio.ifi.trackfind.backend.pojo.Queries;
import no.uio.ifi.trackfind.backend.pojo.SearchResult;
import no.uio.ifi.trackfind.backend.pojo.TfObjectType;
import no.uio.ifi.trackfind.backend.pojo.TfReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.sql.*;
import java.util.*;

/**
 * Service to perform JSONB-oriented search for datasets in the database.
 */
@Slf4j
@Service
public class SearchService {

    private String jdbcUrl;

    private TrackFindProperties properties;
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
     * @param limit      Max number of entries to return. 0 for unlimited.
     * @return Found entries.
     */
    public Collection<SearchResult> search(String repository, String hub, String query, int limit) throws SQLException {
        Collection<TfReference> references = metamodelService.getReferences(repository, hub);

        Collection<TfObjectType> objectTypes = new HashSet<>();
        references.forEach(r -> objectTypes.addAll(Arrays.asList(r.getFromObjectType(), r.getToObjectType())));

        String fullQueryString = buildQuery(references, objectTypes, query, limit);

        PreparedStatement preparedStatement = connection.prepareStatement(fullQueryString);
        ResultSet resultSet = preparedStatement.executeQuery();
        Collection<SearchResult> results = new ArrayList<>();
        while (resultSet.next()) {
            SearchResult searchResult = new SearchResult();
            for (TfObjectType objectType : objectTypes) {
                String json = resultSet.getString(objectType.getName() + "_content");
                searchResult.getContent().put(objectType.getName(), gson.fromJson(json, Map.class));
            }
            results.add(searchResult);
        }
        return results;
    }

    protected String buildQuery(Collection<TfReference> references, Collection<TfObjectType> objectTypes, String query, int limit) {
        String separator = properties.getLevelsSeparator();

        StringBuilder fullQuery = new StringBuilder("SELECT ");

        for (TfObjectType objectType : objectTypes) {
            fullQuery.append(objectType.getName()).append(".content ").append(objectType.getName()).append("_content, ");
        }

        fullQuery.setLength(fullQuery.length() - 2);
        fullQuery.append("\nFROM ");

        for (TfObjectType objectType : objectTypes) {
            fullQuery.append("tf_latest_objects ").append(objectType.getName()).append(", ");
        }

        fullQuery.setLength(fullQuery.length() - 2);
        fullQuery.append("\nWHERE ");

        for (TfObjectType objectType : objectTypes) {
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

        fullQuery.append("\n\n");

        fullQuery.append(query);

        limit = limit == 0 ? Integer.MAX_VALUE : limit;
        fullQuery.append(" LIMIT ").append(limit);

        System.out.println("fullQuery = " + fullQuery);

        return fullQuery.toString().replaceAll("\\?", "\\?\\?");
    }

    @Value("${spring.datasource.url}")
    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    @Autowired
    public void setProperties(TrackFindProperties properties) {
        this.properties = properties;
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
