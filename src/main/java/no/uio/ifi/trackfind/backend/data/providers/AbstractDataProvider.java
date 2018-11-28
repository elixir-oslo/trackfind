package no.uio.ifi.trackfind.backend.data.providers;

import alexh.weak.Dynamic;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.vaadin.data.provider.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.server.SerializablePredicate;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import no.uio.ifi.trackfind.backend.dao.*;
import no.uio.ifi.trackfind.backend.data.TreeNode;
import no.uio.ifi.trackfind.backend.events.DataReloadEvent;
import no.uio.ifi.trackfind.backend.operations.Operation;
import no.uio.ifi.trackfind.backend.repositories.DatasetRepository;
import no.uio.ifi.trackfind.backend.repositories.MappingRepository;
import no.uio.ifi.trackfind.backend.repositories.SourceRepository;
import no.uio.ifi.trackfind.backend.repositories.StandardRepository;
import no.uio.ifi.trackfind.backend.scripting.ScriptingEngine;
import no.uio.ifi.trackfind.backend.services.CacheService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract class for all data providers.
 * Implements some common logic like getting metamodel, searching, etc.
 *
 * @author Dmytro Titov
 */
@Slf4j
public abstract class AbstractDataProvider
        extends AbstractBackEndHierarchicalDataProvider<TreeNode, SerializablePredicate<TreeNode>>
        implements DataProvider, Comparable<DataProvider> {

    protected String jdbcUrl;

    protected TrackFindProperties properties;
    protected ApplicationEventPublisher applicationEventPublisher;
    protected CacheService cacheService;
    protected JdbcTemplate jdbcTemplate;
    protected SourceRepository sourceRepository;
    protected StandardRepository standardRepository;
    protected DatasetRepository datasetRepository;
    protected MappingRepository mappingRepository;
    protected ExecutorService executorService;
    protected Gson gson;
    protected Collection<ScriptingEngine> scriptingEngines;

    protected Connection connection;

    private LoadingCache<Boolean, Multimap<String, String>> flatMetamodelCache = Caffeine.newBuilder()
            .build(key -> {
                Multimap<String, String> metamodel = HashMultimap.create();
                List<Map<String, Object>> attributeValuePairs = jdbcTemplate.queryForList(
                        "SELECT attribute, value FROM " + (key ? "source" : "standard") + "_metamodel WHERE repository = ?",
                        getName());
                for (Map attributeValuePair : attributeValuePairs) {
                    String attribute = String.valueOf(attributeValuePair.get("attribute"));
                    String value = String.valueOf(attributeValuePair.get("value"));
                    metamodel.put(attribute, value);
                }
                return metamodel;
            });

    private LoadingCache<Boolean, Map<String, Object>> treeMetamodelCache = Caffeine.newBuilder()
            .build(key -> {
                Map<String, Object> result = new HashMap<>();
                Multimap<String, String> metamodelFlat = getMetamodelFlat(key);
                for (Map.Entry<String, Collection<String>> entry : metamodelFlat.asMap().entrySet()) {
                    String attribute = entry.getKey();
                    Map<String, Object> metamodel = result;
                    String[] path = attribute.split(properties.getLevelsSeparator());
                    for (int i = 0; i < path.length - 1; i++) {
                        String part = path[i];
                        metamodel = (Map<String, Object>) metamodel.computeIfAbsent(part, k -> new HashMap<String, Object>());
                    }
                    String valuesKey = path[path.length - 1];
                    metamodel.put(valuesKey, entry.getValue());
                }
                return result;
            });

    @PostConstruct
    protected void init() throws SQLException {
        try {
            if (datasetRepository.countByRepository(getName()) == 0) {
                crawlRemoteRepository();
            }
            jdbcTemplate.execute(String.format(Queries.METAMODEL_VIEW, "source", "curated", properties.getLevelsSeparator(), properties.getLevelsSeparator()));
            jdbcTemplate.execute(String.format(Queries.METAMODEL_VIEW, "standard", "standard", properties.getLevelsSeparator(), properties.getLevelsSeparator()));

            Integer count = jdbcTemplate.queryForObject(Queries.CHECK_SEARCH_USER_EXISTS, Integer.TYPE);
            if (count == 0) {
                jdbcTemplate.execute(Queries.CREATE_SEARCH_USER);
            }

            connection = DriverManager.getConnection(jdbcUrl, "search", "search");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return getClass().getSimpleName().replace("DataProvider", "");
    }

    /**
     * Fetches data from the repository.
     *
     * @throws Exception in case of some problems.
     */
    protected abstract void fetchData() throws Exception;

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public synchronized void crawlRemoteRepository() {
        log.info("Fetching data using " + getName());
        try {
            fetchData();
            applicationEventPublisher.publishEvent(new DataReloadEvent(getName(), Operation.CRAWLING));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
        log.info("Success!");
    }

    /**
     * Saves datasets to the database.
     *
     * @param datasets Datasets to save.
     */
    protected void save(Collection<Map> datasets) {
        sourceRepository.saveAll(datasets.parallelStream().map(map -> {
            Source source = new Source();
            source.setRepository(getName());
            source.setContent(gson.toJson(map));
            source.setRawVersion(0L);           // TODO: set proper version
            source.setCuratedVersion(0L);       // TODO: set proper version
            return source;
        }).collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Transactional
    @Override
    public synchronized void applyMappings() {
        log.info("Applying mappings for " + getName());
        Collection<Mapping> mappings = mappingRepository.findByRepository(getName());
        Collection<Mapping> staticMappings = mappings.stream().filter(Mapping::isStaticMapping).collect(Collectors.toSet());
        Optional<Mapping> dynamicMappingOptional = mappings.stream().filter(m -> !m.isStaticMapping()).findAny();
        Collection<Source> sources = sourceRepository.findByRepositoryLatest(getName());
        Collection<Standard> standards = new HashSet<>();
        ScriptingEngine scriptingEngine = scriptingEngines.stream().filter(se -> properties.getScriptingLanguage().equals(se.getLanguage())).findAny().orElseThrow(RuntimeException::new);
        try {
            for (Source source : sources) {
                Map<String, Object> rawMap = gson.fromJson(source.getContent(), Map.class);
                Map<String, Object> standardMap = new HashMap<>();
                if (dynamicMappingOptional.isPresent()) {
                    Mapping mapping = dynamicMappingOptional.get();
                    standardMap = gson.fromJson(scriptingEngine.execute(mapping.getFrom(), source.getContent()), Map.class);
                }
                for (Mapping mapping : staticMappings) {
                    Collection<String> values;
                    Dynamic dynamicValues = Dynamic.from(rawMap).get(mapping.getFrom(), properties.getLevelsSeparator());
                    if (dynamicValues.isPresent()) {
                        if (dynamicValues.isList()) {
                            values = dynamicValues.asList();
                        } else {
                            values = Collections.singletonList(dynamicValues.asString());
                        }
                    } else {
                        values = Collections.emptyList();
                    }
                    String[] path = mapping.getTo().split(properties.getLevelsSeparator());
                    Map<String, Object> nestedMap = standardMap;
                    for (int i = 0; i < path.length - 1; i++) {
                        nestedMap = (Map<String, Object>) nestedMap.computeIfAbsent(path[i], k -> new HashMap<String, Object>());
                    }
                    if (CollectionUtils.size(values) == 1) {
                        nestedMap.put(path[path.length - 1], values.iterator().next());
                    } else {
                        nestedMap.put(path[path.length - 1], values);
                    }
                }
                Standard standard = new Standard();
                standard.setId(source.getId());
                standard.setContent(gson.toJson(standardMap));
                standard.setRawVersion(source.getRawVersion());
                standard.setCuratedVersion(source.getCuratedVersion());
                standard.setStandardVersion(0L);
                standardRepository.findByIdLatest(standard.getId()).ifPresent(s -> standard.setStandardVersion(s.getStandardVersion() + 1));
                standards.add(standard);
            }
            standardRepository.saveAll(standards);
            applicationEventPublisher.publishEvent(new DataReloadEvent(getName(), Operation.MAPPING));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
        log.info("Success!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void resetCaches() {
        treeMetamodelCache.invalidateAll();
        flatMetamodelCache.invalidateAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMetamodelTree(boolean raw) {
        return treeMetamodelCache.get(raw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Multimap<String, String> getMetamodelFlat(boolean raw) {
        return flatMetamodelCache.get(raw);
    }

    @Override
    public Collection<String> getAttributes(String filter, boolean raw, boolean top) {
        Set<String> attributes = top ? getMetamodelTree(raw).keySet() : getMetamodelFlat(raw).asMap().keySet();
        return attributes.parallelStream().filter(a -> a.contains(filter)).collect(Collectors.toSet());
    }

    @Override
    public Collection<String> getSubAttributes(String attribute, String filter, boolean raw) {
        Set<String> attributes = getMetamodelFlat(raw).asMap().keySet();
        Set<String> filteredAttributes = attributes.stream().filter(a -> a.contains(filter)).collect(Collectors.toSet());
        String separator = properties.getLevelsSeparator();
        if (filteredAttributes.contains(attribute)) {
            return Collections.emptySet();
        }
        return filteredAttributes
                .parallelStream()
                .filter(a -> a.startsWith(attribute))
                .map(a -> a.replace(attribute, ""))
                .map(a -> (a.contains(separator) ? a.substring(separator.length()) : a))
                .map(a -> (a.contains(separator) ? a.substring(0, a.indexOf(separator)) : a))
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<String> getValues(String attribute, String filter, boolean raw) {
        return getMetamodelFlat(raw).get(attribute).parallelStream().filter(a -> a.contains(filter)).collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Dataset> search(String query, int limit) {
        try {
            limit = limit == 0 ? Integer.MAX_VALUE : limit;
            String rawQuery = String.format("SELECT *\n" +
                    "FROM latest_datasets\n" +
                    "WHERE repository = '%s'\n" +
                    "  AND (%s)\n" +
                    "ORDER BY id ASC\n" +
                    "LIMIT %s", getName(), query, limit);
            rawQuery = rawQuery.replaceAll("\\?", "\\?\\?");
            PreparedStatement preparedStatement = connection.prepareStatement(rawQuery);
            ResultSet resultSet = preparedStatement.executeQuery();
            Collection<Dataset> result = new ArrayList<>();
            while (resultSet.next()) {
                Dataset dataset = new Dataset();
                dataset.setId(resultSet.getLong("id"));
                dataset.setRepository(resultSet.getString("repository"));
                dataset.setCuratedContent(resultSet.getString("curated_content"));
                dataset.setStandardContent(resultSet.getString("standard_content"));
                dataset.setFairContent(resultSet.getString("fair_content"));
                dataset.setVersion(resultSet.getString("version"));
                result.add(dataset);
            }
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Collections.emptySet();
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Dataset fetch(Long datasetId, String version) {
        return version == null ? datasetRepository.findByIdLatest(datasetId) : datasetRepository.findByIdAndVersion(datasetId, version);
    }

    @Override
    protected Stream<TreeNode> fetchChildrenFromBackEnd(HierarchicalQuery<TreeNode, SerializablePredicate<TreeNode>> query) {
        Map<String, Object> metamodelTree = getMetamodelTree(true);
        Multimap<String, String> metamodelFlat = getMetamodelFlat(true);
        Optional<TreeNode> parentOptional = query.getParentOptional();
        Optional<SerializablePredicate<TreeNode>> filter = query.getFilter();
        if (!parentOptional.isPresent()) {
            return metamodelTree.keySet().stream().map(c -> {
                TreeNode treeNode = new TreeNode();
                treeNode.setValue(c);
                treeNode.setParent(null);
                treeNode.setFin(metamodelFlat.containsKey(treeNode.getPath()));
                treeNode.setSeparator(properties.getLevelsSeparator());
                treeNode.setLevel(0);
                treeNode.setChildren(treeNode.isFin()
                        ? getValues(treeNode.getPath(), "", true)
                        : getSubAttributes(treeNode.getPath(), "", true));
                treeNode.setAttribute(true);
                return treeNode;
            });
        } else {
            TreeNode parent = parentOptional.get();
            if (!parent.isAttribute()) {
                return Stream.empty();
            }
            Collection<String> children = parent.isFin()
                    ? getValues(parent.getPath(), "", true)
                    : getSubAttributes(parent.getPath(), "", true);
            return children.stream().map(c -> {
                TreeNode treeNode = new TreeNode();
                treeNode.setValue(c);
                treeNode.setParent(parent);
                treeNode.setFin(metamodelFlat.containsKey(treeNode.getPath()));
                treeNode.setSeparator(properties.getLevelsSeparator());
                treeNode.setLevel(parent.getLevel() + 1);
                treeNode.setChildren(treeNode.isFin()
                        ? getValues(treeNode.getPath(), "", true)
                        : getSubAttributes(treeNode.getPath(), "", true));
                treeNode.setAttribute(CollectionUtils.isNotEmpty(treeNode.getChildren()));
                return treeNode;
            });
        }
    }

    @Override
    public int getChildCount(HierarchicalQuery<TreeNode, SerializablePredicate<TreeNode>> query) {
        return (int) fetchChildrenFromBackEnd(query).count();
    }

    @Override
    public boolean hasChildren(TreeNode item) {
        return getChildCount(new HierarchicalQuery<>(null, item)) != 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(DataProvider that) {
        return this.getName().compareTo(that.getName());
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
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Autowired
    public void setCacheService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Autowired
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Autowired
    public void setSourceRepository(SourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    @Autowired
    public void setStandardRepository(StandardRepository standardRepository) {
        this.standardRepository = standardRepository;
    }

    @Autowired
    public void setDatasetRepository(DatasetRepository datasetRepository) {
        this.datasetRepository = datasetRepository;
    }

    @Autowired
    public void setMappingRepository(MappingRepository mappingRepository) {
        this.mappingRepository = mappingRepository;
    }

    @Autowired
    public void setExecutorService(ExecutorService workStealingPool) {
        this.executorService = workStealingPool;
    }

    @Autowired
    public void setGson(Gson gson) {
        this.gson = gson;
    }

    @Autowired
    public void setScriptingEngines(Collection<ScriptingEngine> scriptingEngines) {
        this.scriptingEngines = scriptingEngines;
    }

}
