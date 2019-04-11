CREATE OR REPLACE FUNCTION jsonb_recursive_merge(a jsonb, b jsonb)
    RETURNS jsonb
    LANGUAGE SQL AS
$$
SELECT jsonb_object_agg(
               COALESCE(ka, kb),
               CASE
                   WHEN va ISNULL THEN vb
                   WHEN vb ISNULL THEN va
                   WHEN jsonb_typeof(va) <> 'object' THEN vb
                   ELSE jsonb_recursive_merge(va, vb)
                   END
           )
FROM jsonb_each(a) e1(ka, va)
         FULL JOIN jsonb_each(b) e2(kb, vb) ON ka = kb
$$;

CREATE TABLE IF NOT EXISTS tf_hubs
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR NOT NULL,
    repository VARCHAR NOT NULL,
    UNIQUE (name, repository)
);

CREATE TABLE IF NOT EXISTS tf_versions
(
    id        BIGSERIAL PRIMARY KEY,
    version   VARCHAR NOT NULL,
    operation VARCHAR NOT NULL,
    username  VARCHAR NOT NULL,
    time      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tf_object_types
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR NOT NULL,
    hub_id     BIGINT,
    version_id BIGINT  NOT NULL,
    UNIQUE (name, hub_id),
    FOREIGN KEY (hub_id) REFERENCES tf_hubs (id),
    FOREIGN KEY (version_id) REFERENCES tf_versions (id)
);

CREATE SEQUENCE IF NOT EXISTS tf_objects_ids_sequence
    START 1 INCREMENT 1;

CREATE TABLE IF NOT EXISTS tf_objects
(
    id             BIGSERIAL PRIMARY KEY,
    hub_id         BIGINT NOT NULL,
    object_type_id BIGINT NOT NULL,
    version_id     BIGINT NOT NULL,
    content        JSONB  NOT NULL,
    FOREIGN KEY (hub_id) REFERENCES tf_hubs (id),
    FOREIGN KEY (object_type_id) REFERENCES tf_object_types (id),
    FOREIGN KEY (version_id) REFERENCES tf_versions (id)
);

CREATE INDEX IF NOT EXISTS tf_objects_id_index
    ON tf_objects (id);

CREATE INDEX IF NOT EXISTS tf_objects_hub_id_index
    ON tf_objects (hub_id);

CREATE INDEX IF NOT EXISTS tf_objects_object_type_id_index
    ON tf_objects (object_type_id);

CREATE INDEX IF NOT EXISTS tf_objects_content_index
    ON tf_objects
        USING gin (content);

CREATE INDEX IF NOT EXISTS tf_objects_version_id_index
    ON tf_objects (version_id);

CREATE TABLE IF NOT EXISTS tf_references
(
    id                  BIGSERIAL PRIMARY KEY,
    from_object_type_id BIGINT  NOT NULL,
    from_attribute      VARCHAR NOT NULL,
    to_object_type_id   BIGINT  NOT NULL,
    to_attribute        VARCHAR NOT NULL,
    version_id          BIGINT  NOT NULL,
    UNIQUE (from_object_type_id, from_attribute, to_object_type_id, to_attribute),
    FOREIGN KEY (from_object_type_id) REFERENCES tf_object_types (id),
    FOREIGN KEY (to_object_type_id) REFERENCES tf_object_types (id),
    FOREIGN KEY (version_id) REFERENCES tf_versions (id)
);

CREATE TABLE IF NOT EXISTS tf_mappings
(
    id         BIGSERIAL PRIMARY KEY,
    hub_id     BIGINT  NOT NULL,
    map_from   VARCHAR NOT NULL,
    map_to     VARCHAR NOT NULL,
    static     BOOLEAN NOT NULL,
    version_id BIGINT  NOT NULL,
    UNIQUE (hub_id, map_from, map_to, static),
    FOREIGN KEY (hub_id) REFERENCES tf_hubs (id),
    FOREIGN KEY (version_id) REFERENCES tf_versions (id)
);

-- CREATE TABLE IF NOT EXISTS standard
-- (
--     id               BIGINT NOT NULL,
--     content          JSONB  NOT NULL,
--     raw_version      BIGINT NOT NULL,
--     curated_version  BIGINT NOT NULL,
--     standard_version BIGINT NOT NULL,
--     PRIMARY KEY (id, standard_version),
--     FOREIGN KEY (id, raw_version, curated_version) REFERENCES source (id, raw_version, curated_version)
-- );
--
-- CREATE INDEX IF NOT EXISTS standard_index
--     ON standard (id, raw_version, curated_version, standard_version);
--
-- CREATE INDEX IF NOT EXISTS standard_content_index
--     ON standard
--         USING gin (content);
--
-- CREATE MATERIALIZED VIEW IF NOT EXISTS datasets AS
-- SELECT source.id                                                                      AS id,
--        source.repository                                                              AS repository,
--        source.hub                                                                     AS hub,
--        source.content                                                                 AS curated_content,
--        standard.content                                                               AS standard_content,
--        jsonb_recursive_merge(source.content, COALESCE(standard.content, '{}'::jsonb)) AS fair_content,
--        source.raw_version                                                             AS raw_version,
--        source.curated_version                                                         AS curated_version,
--        COALESCE(standard.standard_version, 0)                                         AS standard_version,
--        CONCAT(source.raw_version, ':', source.curated_version, ':',
--               COALESCE(standard.standard_version, 0))                                 AS version
-- FROM source
--          LEFT JOIN standard ON source.id = standard.id AND source.raw_version = standard.raw_version AND
--                                source.curated_version = standard.curated_version
-- GROUP BY source.id, source.repository, source.hub, source.content, standard.content, source.raw_version,
--          source.curated_version, standard.standard_version
--     WITH DATA;
--
-- CREATE INDEX IF NOT EXISTS datasets_index
--     ON datasets (id, repository, hub, raw_version, curated_version, standard_version, version);
--
-- CREATE MATERIALIZED VIEW IF NOT EXISTS latest_datasets AS
--     WITH max_raw_versions AS (SELECT id, MAX(raw_version) as max_version
--                               FROM datasets
--                               GROUP BY id),
--          filtered_by_raw AS (SELECT datasets.*
--                              FROM datasets
--                                       INNER JOIN max_raw_versions
--                                                  ON datasets.id = max_raw_versions.id AND
--                                                     datasets.raw_version = max_raw_versions.max_version),
--          max_curated_versions AS (SELECT id, MAX(curated_version) as max_version
--                                   FROM filtered_by_raw
--                                   GROUP BY id),
--          filtered_by_curated AS (SELECT filtered_by_raw.*
--                                  FROM filtered_by_raw
--                                           INNER JOIN max_curated_versions
--                                                      ON filtered_by_raw.id = max_curated_versions.id AND
--                                                         filtered_by_raw.curated_version = max_curated_versions.max_version),
--          max_standard_versions AS (SELECT id, MAX(standard_version) as max_version
--                                    FROM filtered_by_curated
--                                    GROUP BY id),
--          filtered_by_standard AS (SELECT filtered_by_curated.*
--                                   FROM filtered_by_curated
--                                            INNER JOIN max_standard_versions
--                                                       ON filtered_by_curated.id = max_standard_versions.id AND
--                                                          filtered_by_curated.standard_version =
--                                                          max_standard_versions.max_version)
--     SELECT *
--     FROM filtered_by_standard
--     WITH DATA;
--
-- CREATE INDEX IF NOT EXISTS latest_datasets_index
--     ON latest_datasets (id, repository, hub, raw_version, curated_version, standard_version, version);
--
-- CREATE MATERIALIZED VIEW IF NOT EXISTS source_metamodel AS
--     WITH RECURSIVE collect_metadata AS (SELECT latest_datasets.repository,
--                                                latest_datasets.hub,
--                                                first_level.key,
--                                                first_level.value,
--                                                jsonb_typeof(first_level.value) AS type
--                                         FROM latest_datasets,
--                                              jsonb_each(latest_datasets.curated_content) first_level
--
--                                         UNION ALL
--
--                                         (WITH prev_level AS (
--                                             SELECT *
--                                             FROM collect_metadata
--                                         )
--                                          SELECT prev_level.repository,
--                                                 prev_level.hub,
--                                                 concat(prev_level.key, '->', current_level.key),
--                                                 current_level.value,
--                                                 jsonb_typeof(current_level.value) AS type
--                                          FROM prev_level,
--                                               jsonb_each(prev_level.value) AS current_level
--                                          WHERE prev_level.type = 'object'
--
--                                          UNION ALL
--
--                                          SELECT prev_level.repository,
--                                                 prev_level.hub,
--                                                 concat(prev_level.key, '->', current_level.key),
--                                                 current_level.value,
--                                                 jsonb_typeof(current_level.value) AS type
--                                          FROM prev_level,
--                                               jsonb_array_elements(prev_level.value) AS entry,
--                                               jsonb_each(entry) AS current_level
--                                          WHERE prev_level.type = 'array'
--                                            AND jsonb_typeof(entry) = 'object'
--
--                                          UNION ALL
--
--                                          SELECT prev_level.repository,
--                                                 prev_level.hub,
--                                                 prev_level.key,
--                                                 entry,
--                                                 jsonb_typeof(entry) AS type
--                                          FROM prev_level,
--                                               jsonb_array_elements(prev_level.value) AS entry
--                                          WHERE prev_level.type = 'array'
--                                            AND jsonb_typeof(entry) <> 'object'))
--     SELECT DISTINCT repository,
--                     hub,
--                     key                                                 AS attribute,
--                     array_to_json(ARRAY [collect_metadata.value]) ->> 0 AS value,
--                     type
--     FROM collect_metadata
--     WHERE collect_metadata.type NOT IN ('object', 'array')
--     WITH DATA;
--
-- CREATE MATERIALIZED VIEW IF NOT EXISTS standard_metamodel AS
--     WITH RECURSIVE collect_metadata AS (SELECT latest_datasets.repository,
--                                                latest_datasets.hub,
--                                                first_level.key,
--                                                first_level.value,
--                                                jsonb_typeof(first_level.value) AS type
--                                         FROM latest_datasets,
--                                              jsonb_each(latest_datasets.standard_content) first_level
--
--                                         UNION ALL
--
--                                         (WITH prev_level AS (
--                                             SELECT *
--                                             FROM collect_metadata
--                                         )
--                                          SELECT prev_level.repository,
--                                                 prev_level.hub,
--                                                 concat(prev_level.key, '->', current_level.key),
--                                                 current_level.value,
--                                                 jsonb_typeof(current_level.value) AS type
--                                          FROM prev_level,
--                                               jsonb_each(prev_level.value) AS current_level
--                                          WHERE prev_level.type = 'object'
--
--                                          UNION ALL
--
--                                          SELECT prev_level.repository,
--                                                 prev_level.hub,
--                                                 concat(prev_level.key, '->', current_level.key),
--                                                 current_level.value,
--                                                 jsonb_typeof(current_level.value) AS type
--                                          FROM prev_level,
--                                               jsonb_array_elements(prev_level.value) AS entry,
--                                               jsonb_each(entry) AS current_level
--                                          WHERE prev_level.type = 'array'
--                                            AND jsonb_typeof(entry) = 'object'
--
--                                          UNION ALL
--
--                                          SELECT prev_level.repository,
--                                                 prev_level.hub,
--                                                 prev_level.key,
--                                                 entry,
--                                                 jsonb_typeof(entry) AS type
--                                          FROM prev_level,
--                                               jsonb_array_elements(prev_level.value) AS entry
--                                          WHERE prev_level.type = 'array'
--                                            AND jsonb_typeof(entry) <> 'object'))
--     SELECT DISTINCT repository,
--                     hub,
--                     key                                                 AS attribute,
--                     array_to_json(ARRAY [collect_metadata.value]) ->> 0 AS value,
--                     type
--     FROM collect_metadata
--     WHERE collect_metadata.type NOT IN ('object', 'array')
--     WITH DATA;
--
-- CREATE MATERIALIZED VIEW IF NOT EXISTS fair_metamodel AS
--     WITH RECURSIVE collect_metadata AS (SELECT latest_datasets.repository,
--                                                latest_datasets.hub,
--                                                first_level.key,
--                                                first_level.value,
--                                                jsonb_typeof(first_level.value) AS type
--                                         FROM latest_datasets,
--                                              jsonb_each(latest_datasets.fair_content) first_level
--
--                                         UNION ALL
--
--                                         (WITH prev_level AS (
--                                             SELECT *
--                                             FROM collect_metadata
--                                         )
--                                          SELECT prev_level.repository,
--                                                 prev_level.hub,
--                                                 concat(prev_level.key, '->', current_level.key),
--                                                 current_level.value,
--                                                 jsonb_typeof(current_level.value) AS type
--                                          FROM prev_level,
--                                               jsonb_each(prev_level.value) AS current_level
--                                          WHERE prev_level.type = 'object'
--
--                                          UNION ALL
--
--                                          SELECT prev_level.repository,
--                                                 prev_level.hub,
--                                                 concat(prev_level.key, '->', current_level.key),
--                                                 current_level.value,
--                                                 jsonb_typeof(current_level.value) AS type
--                                          FROM prev_level,
--                                               jsonb_array_elements(prev_level.value) AS entry,
--                                               jsonb_each(entry) AS current_level
--                                          WHERE prev_level.type = 'array'
--                                            AND jsonb_typeof(entry) = 'object'
--
--                                          UNION ALL
--
--                                          SELECT prev_level.repository,
--                                                 prev_level.hub,
--                                                 prev_level.key,
--                                                 entry,
--                                                 jsonb_typeof(entry) AS type
--                                          FROM prev_level,
--                                               jsonb_array_elements(prev_level.value) AS entry
--                                          WHERE prev_level.type = 'array'
--                                            AND jsonb_typeof(entry) <> 'object'))
--     SELECT DISTINCT repository,
--                     hub,
--                     key                                                 AS attribute,
--                     array_to_json(ARRAY [collect_metadata.value]) ->> 0 AS value,
--                     type
--     FROM collect_metadata
--     WHERE collect_metadata.type NOT IN ('object', 'array')
--     WITH DATA;
--
-- CREATE MATERIALIZED VIEW IF NOT EXISTS source_array_of_objects AS
--     WITH RECURSIVE collect_metadata AS (SELECT latest_datasets.repository,
--                                                latest_datasets.hub,
--                                                first_level.key,
--                                                NULL                            AS prev_key,
--                                                first_level.value,
--                                                jsonb_typeof(first_level.value) AS type
--                                         FROM latest_datasets,
--                                              jsonb_each(latest_datasets.curated_content) first_level
--
--                                         UNION ALL
--
--                                         (WITH prev_level AS (
--                                             SELECT *
--                                             FROM collect_metadata
--                                         )
--                                          SELECT prev_level.repository,
--                                                 prev_level.hub,
--                                                 concat(prev_level.key, '->', current_level.key),
--                                                 NULL                              AS prev_key,
--                                                 current_level.value,
--                                                 jsonb_typeof(current_level.value) AS type
--                                          FROM prev_level,
--                                               jsonb_each(prev_level.value) AS current_level
--                                          WHERE prev_level.type = 'object'
--
--                                          UNION ALL
--
--                                          SELECT prev_level.repository,
--                                                 prev_level.hub,
--                                                 concat(prev_level.key, '->', current_level.key),
--                                                 prev_level.key                    AS prev_key,
--                                                 current_level.value,
--                                                 jsonb_typeof(current_level.value) AS type
--                                          FROM prev_level,
--                                               jsonb_array_elements(prev_level.value) AS entry,
--                                               jsonb_each(entry) AS current_level
--                                          WHERE prev_level.type = 'array'
--                                            AND jsonb_typeof(entry) = 'object'
--
--                                          UNION ALL
--
--                                          SELECT prev_level.repository,
--                                                 prev_level.hub,
--                                                 prev_level.key,
--                                                 NULL                AS prev_key,
--                                                 entry,
--                                                 jsonb_typeof(entry) AS type
--                                          FROM prev_level,
--                                               jsonb_array_elements(prev_level.value) AS entry
--                                          WHERE prev_level.type = 'array'
--                                            AND jsonb_typeof(entry) <> 'object'))
--     SELECT DISTINCT repository, hub, prev_key AS attribute
--     FROM collect_metadata
--     WHERE prev_key IS NOT NULL
--     WITH DATA;
--
-- CREATE MATERIALIZED VIEW IF NOT EXISTS standard_array_of_objects AS
--     WITH RECURSIVE collect_metadata AS (SELECT latest_datasets.repository,
--                                                latest_datasets.hub,
--                                                first_level.key,
--                                                NULL                            AS prev_key,
--                                                first_level.value,
--                                                jsonb_typeof(first_level.value) AS type
--                                         FROM latest_datasets,
--                                              jsonb_each(latest_datasets.standard_content) first_level
--
--                                         UNION ALL
--
--                                         (WITH prev_level AS (
--                                             SELECT *
--                                             FROM collect_metadata
--                                         )
--                                          SELECT prev_level.repository,
--                                                 prev_level.hub,
--                                                 concat(prev_level.key, '->', current_level.key),
--                                                 NULL                              AS prev_key,
--                                                 current_level.value,
--                                                 jsonb_typeof(current_level.value) AS type
--                                          FROM prev_level,
--                                               jsonb_each(prev_level.value) AS current_level
--                                          WHERE prev_level.type = 'object'
--
--                                          UNION ALL
--
--                                          SELECT prev_level.repository,
--                                                 prev_level.hub,
--                                                 concat(prev_level.key, '->', current_level.key),
--                                                 prev_level.key                    AS prev_key,
--                                                 current_level.value,
--                                                 jsonb_typeof(current_level.value) AS type
--                                          FROM prev_level,
--                                               jsonb_array_elements(prev_level.value) AS entry,
--                                               jsonb_each(entry) AS current_level
--                                          WHERE prev_level.type = 'array'
--                                            AND jsonb_typeof(entry) = 'object'
--
--                                          UNION ALL
--
--                                          SELECT prev_level.repository,
--                                                 prev_level.hub,
--                                                 prev_level.key,
--                                                 NULL                AS prev_key,
--                                                 entry,
--                                                 jsonb_typeof(entry) AS type
--                                          FROM prev_level,
--                                               jsonb_array_elements(prev_level.value) AS entry
--                                          WHERE prev_level.type = 'array'
--                                            AND jsonb_typeof(entry) <> 'object'))
--     SELECT DISTINCT repository, hub, prev_key AS attribute
--     FROM collect_metadata
--     WHERE prev_key IS NOT NULL
--     WITH DATA;
--
-- CREATE MATERIALIZED VIEW IF NOT EXISTS fair_array_of_objects AS
--     WITH RECURSIVE collect_metadata AS (SELECT latest_datasets.repository,
--                                                latest_datasets.hub,
--                                                first_level.key,
--                                                NULL                            AS prev_key,
--                                                first_level.value,
--                                                jsonb_typeof(first_level.value) AS type
--                                         FROM latest_datasets,
--                                              jsonb_each(latest_datasets.fair_content) first_level
--
--                                         UNION ALL
--
--                                         (WITH prev_level AS (
--                                             SELECT *
--                                             FROM collect_metadata
--                                         )
--                                          SELECT prev_level.repository,
--                                                 prev_level.hub,
--                                                 concat(prev_level.key, '->', current_level.key),
--                                                 NULL                              AS prev_key,
--                                                 current_level.value,
--                                                 jsonb_typeof(current_level.value) AS type
--                                          FROM prev_level,
--                                               jsonb_each(prev_level.value) AS current_level
--                                          WHERE prev_level.type = 'object'
--
--                                          UNION ALL
--
--                                          SELECT prev_level.repository,
--                                                 prev_level.hub,
--                                                 concat(prev_level.key, '->', current_level.key),
--                                                 prev_level.key                    AS prev_key,
--                                                 current_level.value,
--                                                 jsonb_typeof(current_level.value) AS type
--                                          FROM prev_level,
--                                               jsonb_array_elements(prev_level.value) AS entry,
--                                               jsonb_each(entry) AS current_level
--                                          WHERE prev_level.type = 'array'
--                                            AND jsonb_typeof(entry) = 'object'
--
--                                          UNION ALL
--
--                                          SELECT prev_level.repository,
--                                                 prev_level.hub,
--                                                 prev_level.key,
--                                                 NULL                AS prev_key,
--                                                 entry,
--                                                 jsonb_typeof(entry) AS type
--                                          FROM prev_level,
--                                               jsonb_array_elements(prev_level.value) AS entry
--                                          WHERE prev_level.type = 'array'
--                                            AND jsonb_typeof(entry) <> 'object'))
--     SELECT DISTINCT repository, hub, prev_key AS attribute
--     FROM collect_metadata
--     WHERE prev_key IS NOT NULL
--     WITH DATA;
--
