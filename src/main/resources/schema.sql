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

CREATE TABLE IF NOT EXISTS hubs
(
  repository   VARCHAR(255) NOT NULl,
  hub          VARCHAR(255) NOT NULl,
  id_attribute VARCHAR(255),
  PRIMARY KEY (repository, hub)
);

CREATE SEQUENCE IF NOT EXISTS source_ids_sequence
  START 1 INCREMENT 1;

CREATE TABLE IF NOT EXISTS source
(
  id              BIGINT       NOT NULl,
  repository      VARCHAR(255) NOT NULl,
  hub             VARCHAR(255) NOT NULl,
  content         JSONB        NOT NULl,
  raw_version     BIGINT       NOT NULl,
  curated_version BIGINT       NOT NULl,
  PRIMARY KEY (id, raw_version, curated_version)
);

CREATE INDEX IF NOT EXISTS source_index
  ON source (id, repository, hub, raw_version, curated_version);

CREATE INDEX IF NOT EXISTS source_content_index
  ON source
    USING gin (content);

CREATE TABLE IF NOT EXISTS standard
(
  id               BIGINT NOT NULl,
  content          JSONB  NOT NULl,
  raw_version      BIGINT NOT NULl,
  curated_version  BIGINT NOT NULl,
  standard_version BIGINT NOT NULl,
  PRIMARY KEY (id, standard_version),
  FOREIGN KEY (id, raw_version, curated_version) REFERENCES source (id, raw_version, curated_version)
);

CREATE INDEX IF NOT EXISTS standard_index
  ON standard (id, raw_version, curated_version, standard_version);

CREATE INDEX IF NOT EXISTS standard_content_index
  ON standard
    USING gin (content);

CREATE MATERIALIZED VIEW IF NOT EXISTS datasets AS
SELECT source.id                                                                      AS id,
       source.repository                                                              AS repository,
       source.hub                                                                     AS hub,
       source.content                                                                 AS curated_content,
       standard.content                                                               AS standard_content,
       jsonb_recursive_merge(source.content, COALESCE(standard.content, '{}'::jsonb)) AS fair_content,
       source.raw_version                                                             AS raw_version,
       source.curated_version                                                         AS curated_version,
       COALESCE(standard.standard_version, 0)                                         AS standard_version,
       CONCAT(source.raw_version, ':', source.curated_version, ':',
              COALESCE(standard.standard_version, 0))                                 AS version
FROM source
       LEFT JOIN standard ON source.id = standard.id AND source.raw_version = standard.raw_version AND
                             source.curated_version = standard.curated_version
GROUP BY source.id, source.repository, source.hub, source.content, standard.content, source.raw_version,
         source.curated_version, standard.standard_version
  WITH DATA;

CREATE INDEX IF NOT EXISTS datasets_index
  ON datasets (id, repository, hub, raw_version, curated_version, standard_version, version);

CREATE MATERIALIZED VIEW IF NOT EXISTS latest_datasets AS
  WITH max_raw_versions AS (SELECT id, MAX(raw_version) as max_version
                            FROM datasets
                            GROUP BY id),
    filtered_by_raw AS (SELECT datasets.*
                        FROM datasets
                               INNER JOIN max_raw_versions
                                          ON datasets.id = max_raw_versions.id AND
                                             datasets.raw_version = max_raw_versions.max_version),
    max_curated_versions AS (SELECT id, MAX(curated_version) as max_version
                             FROM filtered_by_raw
                             GROUP BY id),
    filtered_by_curated AS (SELECT filtered_by_raw.*
                            FROM filtered_by_raw
                                   INNER JOIN max_curated_versions
                                              ON filtered_by_raw.id = max_curated_versions.id AND
                                                 filtered_by_raw.curated_version = max_curated_versions.max_version),
    max_standard_versions AS (SELECT id, MAX(standard_version) as max_version
                              FROM filtered_by_curated
                              GROUP BY id),
    filtered_by_standard AS (SELECT filtered_by_curated.*
                             FROM filtered_by_curated
                                    INNER JOIN max_standard_versions
                                               ON filtered_by_curated.id = max_standard_versions.id AND
                                                  filtered_by_curated.standard_version =
                                                  max_standard_versions.max_version)
    SELECT *
    FROM filtered_by_standard
  WITH DATA;

CREATE INDEX IF NOT EXISTS latest_datasets_index
  ON latest_datasets (id, repository, hub, raw_version, curated_version, standard_version, version);

CREATE MATERIALIZED VIEW IF NOT EXISTS source_metamodel AS
  WITH RECURSIVE collect_metadata AS (SELECT latest_datasets.repository,
                                             latest_datasets.hub,
                                             first_level.key,
                                             first_level.value,
                                             jsonb_typeof(first_level.value) AS type
                                      FROM latest_datasets,
                                           jsonb_each(latest_datasets.curated_content) first_level

                                      UNION ALL

                                      (WITH prev_level AS (
                                        SELECT *
                                        FROM collect_metadata
                                        )
                                        SELECT prev_level.repository,
                                               prev_level.hub,
                                               concat(prev_level.key, '->', current_level.key),
                                               current_level.value,
                                               jsonb_typeof(current_level.value) AS type
                                        FROM prev_level,
                                             jsonb_each(prev_level.value) AS current_level
                                        WHERE prev_level.type = 'object'

                                        UNION ALL

                                        SELECT prev_level.repository,
                                               prev_level.hub,
                                               concat(prev_level.key, '->', current_level.key),
                                               current_level.value,
                                               jsonb_typeof(current_level.value) AS type
                                        FROM prev_level,
                                             jsonb_array_elements(prev_level.value) AS entry,
                                             jsonb_each(entry) AS current_level
                                        WHERE prev_level.type = 'array'
                                          AND jsonb_typeof(entry) = 'object'

                                        UNION ALL

                                        SELECT prev_level.repository,
                                               prev_level.hub,
                                               prev_level.key,
                                               entry,
                                               jsonb_typeof(entry) AS type
                                        FROM prev_level,
                                             jsonb_array_elements(prev_level.value) AS entry
                                        WHERE prev_level.type = 'array'
                                          AND jsonb_typeof(entry) <> 'object'))
    SELECT DISTINCT repository,
                    hub,
                    key                                                 AS attribute,
                    array_to_json(ARRAY [collect_metadata.value]) ->> 0 AS value,
                    type
    FROM collect_metadata
    WHERE collect_metadata.type NOT IN ('object', 'array')
  WITH DATA;

CREATE MATERIALIZED VIEW IF NOT EXISTS standard_metamodel AS
  WITH RECURSIVE collect_metadata AS (SELECT latest_datasets.repository,
                                             latest_datasets.hub,
                                             first_level.key,
                                             first_level.value,
                                             jsonb_typeof(first_level.value) AS type
                                      FROM latest_datasets,
                                           jsonb_each(latest_datasets.standard_content) first_level

                                      UNION ALL

                                      (WITH prev_level AS (
                                        SELECT *
                                        FROM collect_metadata
                                        )
                                        SELECT prev_level.repository,
                                               prev_level.hub,
                                               concat(prev_level.key, '->', current_level.key),
                                               current_level.value,
                                               jsonb_typeof(current_level.value) AS type
                                        FROM prev_level,
                                             jsonb_each(prev_level.value) AS current_level
                                        WHERE prev_level.type = 'object'

                                        UNION ALL

                                        SELECT prev_level.repository,
                                               prev_level.hub,
                                               concat(prev_level.key, '->', current_level.key),
                                               current_level.value,
                                               jsonb_typeof(current_level.value) AS type
                                        FROM prev_level,
                                             jsonb_array_elements(prev_level.value) AS entry,
                                             jsonb_each(entry) AS current_level
                                        WHERE prev_level.type = 'array'
                                          AND jsonb_typeof(entry) = 'object'

                                        UNION ALL

                                        SELECT prev_level.repository,
                                               prev_level.hub,
                                               prev_level.key,
                                               entry,
                                               jsonb_typeof(entry) AS type
                                        FROM prev_level,
                                             jsonb_array_elements(prev_level.value) AS entry
                                        WHERE prev_level.type = 'array'
                                          AND jsonb_typeof(entry) <> 'object'))
    SELECT DISTINCT repository,
                    hub,
                    key                                                 AS attribute,
                    array_to_json(ARRAY [collect_metadata.value]) ->> 0 AS value,
                    type
    FROM collect_metadata
    WHERE collect_metadata.type NOT IN ('object', 'array')
  WITH DATA;

CREATE MATERIALIZED VIEW IF NOT EXISTS fair_metamodel AS
  WITH RECURSIVE collect_metadata AS (SELECT latest_datasets.repository,
                                             latest_datasets.hub,
                                             first_level.key,
                                             first_level.value,
                                             jsonb_typeof(first_level.value) AS type
                                      FROM latest_datasets,
                                           jsonb_each(latest_datasets.fair_content) first_level

                                      UNION ALL

                                      (WITH prev_level AS (
                                        SELECT *
                                        FROM collect_metadata
                                        )
                                        SELECT prev_level.repository,
                                               prev_level.hub,
                                               concat(prev_level.key, '->', current_level.key),
                                               current_level.value,
                                               jsonb_typeof(current_level.value) AS type
                                        FROM prev_level,
                                             jsonb_each(prev_level.value) AS current_level
                                        WHERE prev_level.type = 'object'

                                        UNION ALL

                                        SELECT prev_level.repository,
                                               prev_level.hub,
                                               concat(prev_level.key, '->', current_level.key),
                                               current_level.value,
                                               jsonb_typeof(current_level.value) AS type
                                        FROM prev_level,
                                             jsonb_array_elements(prev_level.value) AS entry,
                                             jsonb_each(entry) AS current_level
                                        WHERE prev_level.type = 'array'
                                          AND jsonb_typeof(entry) = 'object'

                                        UNION ALL

                                        SELECT prev_level.repository,
                                               prev_level.hub,
                                               prev_level.key,
                                               entry,
                                               jsonb_typeof(entry) AS type
                                        FROM prev_level,
                                             jsonb_array_elements(prev_level.value) AS entry
                                        WHERE prev_level.type = 'array'
                                          AND jsonb_typeof(entry) <> 'object'))
    SELECT DISTINCT repository,
                    hub,
                    key                                                 AS attribute,
                    array_to_json(ARRAY [collect_metadata.value]) ->> 0 AS value,
                    type
    FROM collect_metadata
    WHERE collect_metadata.type NOT IN ('object', 'array')
  WITH DATA;

CREATE MATERIALIZED VIEW IF NOT EXISTS source_array_of_objects AS
  WITH RECURSIVE collect_metadata AS (SELECT latest_datasets.repository,
                                             latest_datasets.hub,
                                             first_level.key,
                                             NULL                            AS prev_key,
                                             first_level.value,
                                             jsonb_typeof(first_level.value) AS type
                                      FROM latest_datasets,
                                           jsonb_each(latest_datasets.curated_content) first_level

                                      UNION ALL

                                      (WITH prev_level AS (
                                        SELECT *
                                        FROM collect_metadata
                                        )
                                        SELECT prev_level.repository,
                                               prev_level.hub,
                                               concat(prev_level.key, '->', current_level.key),
                                               NULL                              AS prev_key,
                                               current_level.value,
                                               jsonb_typeof(current_level.value) AS type
                                        FROM prev_level,
                                             jsonb_each(prev_level.value) AS current_level
                                        WHERE prev_level.type = 'object'

                                        UNION ALL

                                        SELECT prev_level.repository,
                                               prev_level.hub,
                                               concat(prev_level.key, '->', current_level.key),
                                               prev_level.key                    AS prev_key,
                                               current_level.value,
                                               jsonb_typeof(current_level.value) AS type
                                        FROM prev_level,
                                             jsonb_array_elements(prev_level.value) AS entry,
                                             jsonb_each(entry) AS current_level
                                        WHERE prev_level.type = 'array'
                                          AND jsonb_typeof(entry) = 'object'

                                        UNION ALL

                                        SELECT prev_level.repository,
                                               prev_level.hub,
                                               prev_level.key,
                                               NULL                AS prev_key,
                                               entry,
                                               jsonb_typeof(entry) AS type
                                        FROM prev_level,
                                             jsonb_array_elements(prev_level.value) AS entry
                                        WHERE prev_level.type = 'array'
                                          AND jsonb_typeof(entry) <> 'object'))
    SELECT DISTINCT repository, hub, prev_key AS attribute
    FROM collect_metadata
    WHERE prev_key IS NOT NULL
  WITH DATA;

CREATE MATERIALIZED VIEW IF NOT EXISTS standard_array_of_objects AS
  WITH RECURSIVE collect_metadata AS (SELECT latest_datasets.repository,
                                             latest_datasets.hub,
                                             first_level.key,
                                             NULL                            AS prev_key,
                                             first_level.value,
                                             jsonb_typeof(first_level.value) AS type
                                      FROM latest_datasets,
                                           jsonb_each(latest_datasets.standard_content) first_level

                                      UNION ALL

                                      (WITH prev_level AS (
                                        SELECT *
                                        FROM collect_metadata
                                        )
                                        SELECT prev_level.repository,
                                               prev_level.hub,
                                               concat(prev_level.key, '->', current_level.key),
                                               NULL                              AS prev_key,
                                               current_level.value,
                                               jsonb_typeof(current_level.value) AS type
                                        FROM prev_level,
                                             jsonb_each(prev_level.value) AS current_level
                                        WHERE prev_level.type = 'object'

                                        UNION ALL

                                        SELECT prev_level.repository,
                                               prev_level.hub,
                                               concat(prev_level.key, '->', current_level.key),
                                               prev_level.key                    AS prev_key,
                                               current_level.value,
                                               jsonb_typeof(current_level.value) AS type
                                        FROM prev_level,
                                             jsonb_array_elements(prev_level.value) AS entry,
                                             jsonb_each(entry) AS current_level
                                        WHERE prev_level.type = 'array'
                                          AND jsonb_typeof(entry) = 'object'

                                        UNION ALL

                                        SELECT prev_level.repository,
                                               prev_level.hub,
                                               prev_level.key,
                                               NULL                AS prev_key,
                                               entry,
                                               jsonb_typeof(entry) AS type
                                        FROM prev_level,
                                             jsonb_array_elements(prev_level.value) AS entry
                                        WHERE prev_level.type = 'array'
                                          AND jsonb_typeof(entry) <> 'object'))
    SELECT DISTINCT repository, hub, prev_key AS attribute
    FROM collect_metadata
    WHERE prev_key IS NOT NULL
  WITH DATA;

CREATE MATERIALIZED VIEW IF NOT EXISTS fair_array_of_objects AS
  WITH RECURSIVE collect_metadata AS (SELECT latest_datasets.repository,
                                             latest_datasets.hub,
                                             first_level.key,
                                             NULL                            AS prev_key,
                                             first_level.value,
                                             jsonb_typeof(first_level.value) AS type
                                      FROM latest_datasets,
                                           jsonb_each(latest_datasets.fair_content) first_level

                                      UNION ALL

                                      (WITH prev_level AS (
                                        SELECT *
                                        FROM collect_metadata
                                        )
                                        SELECT prev_level.repository,
                                               prev_level.hub,
                                               concat(prev_level.key, '->', current_level.key),
                                               NULL                              AS prev_key,
                                               current_level.value,
                                               jsonb_typeof(current_level.value) AS type
                                        FROM prev_level,
                                             jsonb_each(prev_level.value) AS current_level
                                        WHERE prev_level.type = 'object'

                                        UNION ALL

                                        SELECT prev_level.repository,
                                               prev_level.hub,
                                               concat(prev_level.key, '->', current_level.key),
                                               prev_level.key                    AS prev_key,
                                               current_level.value,
                                               jsonb_typeof(current_level.value) AS type
                                        FROM prev_level,
                                             jsonb_array_elements(prev_level.value) AS entry,
                                             jsonb_each(entry) AS current_level
                                        WHERE prev_level.type = 'array'
                                          AND jsonb_typeof(entry) = 'object'

                                        UNION ALL

                                        SELECT prev_level.repository,
                                               prev_level.hub,
                                               prev_level.key,
                                               NULL                AS prev_key,
                                               entry,
                                               jsonb_typeof(entry) AS type
                                        FROM prev_level,
                                             jsonb_array_elements(prev_level.value) AS entry
                                        WHERE prev_level.type = 'array'
                                          AND jsonb_typeof(entry) <> 'object'))
    SELECT DISTINCT repository, hub, prev_key AS attribute
    FROM collect_metadata
    WHERE prev_key IS NOT NULL
  WITH DATA;

CREATE TABLE IF NOT EXISTS mappings
(
  id         BIGSERIAL PRIMARY KEY,
  repository VARCHAR(255) NOT NULl,
  hub        VARCHAR(255) NOT NULl,
  map_to     VARCHAR,
  map_from   VARCHAR      NOT NULl,
  static     BOOLEAN      NOT NULl,
  UNIQUE (repository, map_to, map_from, static)
);
