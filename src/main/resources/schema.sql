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
  SELECT source.id                                                                                                    AS id,
         source.repository                                                                                            AS repository,
         source.content                                                                                               AS curated_content,
         standard.content                                                                                             AS standard_content,
         source.content ||
         JSONB_BUILD_OBJECT('fair', JSONB_SET(COALESCE(standard.content, '{}'::jsonb), '{id}', TO_JSONB(source.id)))  AS fair_content,
         source.raw_version                                                                                           AS raw_version,
         source.curated_version                                                                                       AS curated_version,
         COALESCE(standard.version, 0)                                                                                AS standard_version,
         CONCAT(source.raw_version, ':', source.curated_version, ':', COALESCE(standard.version, 0))                  AS version
  FROM source
         LEFT JOIN standard on source.id = standard.id;

CREATE TABLE IF NOT EXISTS mappings
(
  repository VARCHAR(255) NOT NULl,
  map_to     VARCHAR(255) NOT NULl,
  map_from   VARCHAR(255) NOT NULl,
  static     BOOLEAN      NOT NULl,
  PRIMARY KEY (repository, map_to)
);
