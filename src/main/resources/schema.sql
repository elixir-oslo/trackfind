CREATE SEQUENCE IF NOT EXISTS source_ids_sequence
  START 1 INCREMENT 1;

CREATE TABLE IF NOT EXISTS source
(
  id              BIGINT       NOT NULl,
  repository      VARCHAR(255) NOT NULl,
  content         JSONB        NOT NULl,
  raw_version     BIGINT       NOT NULl,
  curated_version BIGINT       NOT NULl,
  PRIMARY KEY (id, raw_version, curated_version)
);

CREATE INDEX IF NOT EXISTS source_index
  ON source(id, repository, raw_version, curated_version);

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
  ON standard(id, raw_version, curated_version, standard_version);

CREATE INDEX IF NOT EXISTS standard_content_index
  ON standard
  USING gin (content);

CREATE MATERIALIZED VIEW IF NOT EXISTS datasets AS
  SELECT
         sub_query.id,
         sub_query.repository,
         sub_query.curated_content,
         sub_query.standard_content,
         JSONB_SET(JSONB_SET(sub_query.fair_content, '{fair, id}', TO_JSONB(sub_query.id)), '{fair, version}', TO_JSONB(sub_query.version)) AS fair_content,
         sub_query.raw_version,
         sub_query.curated_version,
         sub_query.standard_version,
         sub_query.version
  FROM (SELECT
               source.id                                                                                                    AS id,
               source.repository                                                                                            AS repository,
               source.content                                                                                               AS curated_content,
               standard.content                                                                                             AS standard_content,
               source.content || JSONB_BUILD_OBJECT('fair', COALESCE(standard.content, '{}'::jsonb))                        AS fair_content,
               source.raw_version                                                                                           AS raw_version,
               source.curated_version                                                                                       AS curated_version,
               COALESCE(standard.standard_version, 0)                                                                       AS standard_version,
               CONCAT(source.raw_version, ':', source.curated_version, ':', COALESCE(standard.standard_version, 0))         AS version
        FROM source
               LEFT JOIN standard on source.id = standard.id) AS sub_query
WITH DATA;

CREATE INDEX IF NOT EXISTS datasets_index
  ON datasets(id, repository, raw_version, curated_version, standard_version, version);

CREATE MATERIALIZED VIEW IF NOT EXISTS latest_datasets AS
  WITH max_raw_versions AS (SELECT id as id, MAX(raw_version) as max_version FROM datasets GROUP BY id),
      filtered_by_raw AS (SELECT datasets.*
                          FROM datasets
                                 INNER JOIN max_raw_versions
                                   ON datasets.id = max_raw_versions.id AND datasets.raw_version = max_raw_versions.max_version),
      max_curated_versions AS (SELECT id as id, MAX(curated_version) as max_version FROM filtered_by_raw GROUP BY id),
      filtered_by_curated AS (SELECT filtered_by_raw.*
                              FROM filtered_by_raw
                                     INNER JOIN max_curated_versions
                                       ON filtered_by_raw.id = max_curated_versions.id AND filtered_by_raw.curated_version = max_curated_versions.max_version),
      max_standard_versions AS (SELECT id as id, MAX(standard_version) as max_version FROM filtered_by_curated GROUP BY id),
      filtered_by_standard AS (SELECT filtered_by_curated.*
                               FROM filtered_by_curated
                                      INNER JOIN max_standard_versions
                                        ON filtered_by_curated.id = max_standard_versions.id AND filtered_by_curated.standard_version = max_standard_versions.max_version)
  SELECT *
  FROM filtered_by_standard;

CREATE INDEX IF NOT EXISTS latest_datasets_index
  ON latest_datasets(id, repository, raw_version, curated_version, standard_version, version);

CREATE TABLE IF NOT EXISTS mappings
(
  id         BIGSERIAL PRIMARY KEY,
  repository VARCHAR(255) NOT NULl,
  map_to     VARCHAR,
  map_from   VARCHAR      NOT NULl,
  static     BOOLEAN      NOT NULl,
  UNIQUE (repository, map_to, map_from, static)
);
