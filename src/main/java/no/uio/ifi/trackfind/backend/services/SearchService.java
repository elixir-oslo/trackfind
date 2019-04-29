package no.uio.ifi.trackfind.backend.services;

import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import no.uio.ifi.trackfind.backend.pojo.Queries;
import no.uio.ifi.trackfind.backend.pojo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to perform JSONB-oriented search for datasets in the database.
 */
@Slf4j
@Service
public class SearchService {

    private String jdbcUrl;

    private TrackFindProperties properties;
    private JdbcTemplate jdbcTemplate;

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
    public Collection<SearchResult> search(String repository, String hub, String query, int limit) {
//        try {
//            String repositoryName = hub.getRepository();
//            String hubName = hub.getName();
//            limit = limit == 0 ? Integer.MAX_VALUE : limit;
//            Map<String, String> joinTerms = new HashMap<>();
//            int size = joinTerms.size();
//            while (true) {
//                query = processQuery(query, joinTerms);
//                if (size == joinTerms.size()) {
//                    break;
//                }
//                size = joinTerms.size();
//            }
//            String joinTermsConcatenated = joinTerms
//                    .entrySet()
//                    .stream()
//                    .map(e -> String.format("jsonb_array_elements(%s) %s",
//                            e.getKey().substring(0, e.getKey().length() - properties.getLevelsSeparator().length() - 1),
//                            e.getValue()
//                    ))
//                    .collect(Collectors.joining(", "));
//            if (StringUtils.isNotEmpty(joinTermsConcatenated)) {
//                joinTermsConcatenated = ", " + joinTermsConcatenated;
//            }
//            String rawQuery = String.format("SELECT *\n" +
//                    "FROM latest_datasets%s\n" +
//                    "WHERE repository = '%s' AND hub = '%s'\n" +
//                    "  AND (%s)\n" +
//                    "ORDER BY id ASC\n" +
//                    "LIMIT %s", joinTermsConcatenated, repositoryName, hubName, query, limit);
//            rawQuery = rawQuery.replaceAll("\\?", "\\?\\?");
//            PreparedStatement preparedStatement = connection.prepareStatement(rawQuery);
//            ResultSet resultSet = preparedStatement.executeQuery();
//            Collection<Dataset> result = new ArrayList<>();
//            while (resultSet.next()) {
//                Dataset dataset = new Dataset();
//                dataset.setId(resultSet.getLong("id"));
//                dataset.setRepository(resultSet.getString("repository"));
//                dataset.setCuratedContent(resultSet.getString("curated_content"));
//                dataset.setStandardContent(resultSet.getString("standard_content"));
//                dataset.setFairContent(resultSet.getString("fair_content"));
//                dataset.setVersion(resultSet.getString("version"));
//                result.add(dataset);
//            }
//            return result;
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//            return Collections.emptySet();
//        }
        return Collections.emptySet();
    }

    protected String processQuery(String query, Map<String, String> allJoinTerms) {
        Map<String, String> joinTerms = getJoinTerms(query, allJoinTerms);
        for (Map.Entry<String, String> joinTerm : joinTerms.entrySet()) {
            query = query.replaceAll(Pattern.quote(joinTerm.getKey()), joinTerm.getValue() + ".value");
        }
        return query;
    }

    protected Map<String, String> getJoinTerms(String query, Map<String, String> allJoinTerms) {
        Collection<String> joinTerms = new HashSet<>();
        String separator = properties.getLevelsSeparator();
        String end = separator + "*";
        for (String start : Arrays.asList(
                "fair_content" + separator,
                "joinTerm\\d+.value" + separator
        )) {
            String regexString = start + "(.*?)" + Pattern.quote(end);
            Pattern pattern = Pattern.compile(regexString);
            Matcher matcher = pattern.matcher(query);
            while (matcher.find()) {
                joinTerms.add(matcher.group());
            }
        }
        Map<String, String> substitution = new HashMap<>();
        int i = allJoinTerms.size();
        for (String joinTerm : joinTerms) {
            substitution.put(joinTerm, "joinTerm" + i++);
        }
        allJoinTerms.putAll(substitution);
        return substitution;
    }

//    @SuppressWarnings("unchecked")
//    public Dataset fetch(Long datasetId, String version) {
//        return version == null ? objectRepository.findByIdLatest(datasetId) : objectRepository.findByIdAndVersion(datasetId, version);
//    }

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

}
