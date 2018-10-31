CREATE SEQUENCE IF NOT EXISTS source_ids_sequence
  START 1 INCREMENT 1;

CREATE TABLE IF NOT EXISTS source
(
  id              BIGINT PRIMARY KEY,
  repository      VARCHAR(255) NOT NULl,
  content         JSONB        NOT NULl,
  raw_version     BIGINT       NOT NULl,
  curated_version BIGINT       NOT NULl,
  UNIQUE (id, raw_version, curated_version)
);

CREATE INDEX IF NOT EXISTS source_content_index
  ON source
  USING gin (content);

CREATE TABLE IF NOT EXISTS standard
(
  id      BIGINT PRIMARY KEY REFERENCES source (id),
  content JSONB  NOT NULl,
  version BIGINT NOT NULl,
  UNIQUE (id, version)
);

CREATE INDEX IF NOT EXISTS standard_content_index
  ON standard
  USING gin (content);

CREATE OR REPLACE VIEW datasets AS
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
               COALESCE(standard.version, 0)                                                                                AS standard_version,
               CONCAT(source.raw_version, ':', source.curated_version, ':', COALESCE(standard.version, 0))                  AS version
        FROM source
               LEFT JOIN standard on source.id = standard.id) AS sub_query;

CREATE TABLE IF NOT EXISTS mappings
(
  id         BIGSERIAL PRIMARY KEY,
  repository VARCHAR(255) NOT NULl,
  map_to     VARCHAR,
  map_from   VARCHAR      NOT NULl,
  static     BOOLEAN      NOT NULl,
  UNIQUE (repository, map_to, map_from, static)
);
