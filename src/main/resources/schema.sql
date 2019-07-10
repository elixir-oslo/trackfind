CREATE OR REPLACE FUNCTION jsonb_recursive_merge(a jsonb, b jsonb)
    RETURNS JSONB
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
    repository VARCHAR NOT NULL,
    name       VARCHAR NOT NULL,
    UNIQUE (repository, name)
);

CREATE OR REPLACE FUNCTION check_current_version(hub_id BIGINT)
    RETURNS BOOLEAN
    LANGUAGE SQL AS
$$
SELECT COUNT(tfv.id) = 1
FROM tf_versions tfv
WHERE tfv.hub_id = hub_id
  AND tfv.current = TRUE
$$;

CREATE OR REPLACE FUNCTION check_previous_version(hub_id BIGINT)
    RETURNS BOOLEAN
    LANGUAGE SQL AS
$$
SELECT COUNT(tfv.id) <= 1
FROM tf_versions tfv
WHERE tfv.hub_id = hub_id
  AND tfv.previous = TRUE
$$;

CREATE TABLE IF NOT EXISTS tf_versions
(
    id        BIGSERIAL PRIMARY KEY,
    hub_id    BIGINT REFERENCES tf_hubs (id),
    version   BIGINT  NOT NULL,
    current   BOOLEAN NOT NULL,
    previous  BOOLEAN NOT NULL,
    operation VARCHAR NOT NULL,
    username  VARCHAR NOT NULL,
    time      TIMESTAMP,
    UNIQUE (hub_id, version),
    CHECK ( check_current_version(hub_id) ),
    CHECK ( check_previous_version(hub_id) )
);

CREATE TABLE IF NOT EXISTS tf_object_types
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR NOT NULL,
    version_id BIGINT  NOT NULL REFERENCES tf_versions (id),
    UNIQUE (name, version_id)
);

CREATE OR REPLACE FUNCTION check_reference(from_object_type_id BIGINT, to_object_type_id BIGINT)
    RETURNS BOOLEAN
    LANGUAGE SQL AS
$$
SELECT fot.version_id = tot.version_id
FROM tf_object_types fot,
     tf_object_types tot
WHERE fot.id = from_object_type_id
  AND tot.id = to_object_type_id
$$;

CREATE SEQUENCE IF NOT EXISTS tf_objects_ids_sequence
    START 1 INCREMENT 1;

CREATE TABLE IF NOT EXISTS tf_objects
(
    id             BIGSERIAL PRIMARY KEY,
    object_type_id BIGINT NOT NULL REFERENCES tf_object_types (id),
    content        JSONB  NOT NULL
);

CREATE INDEX IF NOT EXISTS tf_objects_id_index
    ON tf_objects (id);

CREATE INDEX IF NOT EXISTS tf_objects_object_type_id_index
    ON tf_objects (object_type_id);

CREATE INDEX IF NOT EXISTS tf_objects_content_index
    ON tf_objects
        USING gin (content);

CREATE TABLE IF NOT EXISTS tf_references
(
    id                  BIGSERIAL PRIMARY KEY,
    from_object_type_id BIGINT  NOT NULL REFERENCES tf_object_types (id),
    from_attribute      VARCHAR NOT NULL,
    to_object_type_id   BIGINT  NOT NULL REFERENCES tf_object_types (id),
    to_attribute        VARCHAR NOT NULL,
    UNIQUE (from_object_type_id, from_attribute, to_object_type_id, to_attribute),
    CHECK ( check_reference(from_object_type_id, to_object_type_id) )
);

CREATE TABLE IF NOT EXISTS tf_scripts
(
    id         BIGSERIAL PRIMARY KEY,
    version_id BIGINT NOT NULL REFERENCES tf_versions (id),
    index      BIGINT NOT NULL,
    script     TEXT   NOT NULL
);

CREATE OR REPLACE VIEW tf_latest_versions AS
    WITH max_versions AS (SELECT hub_id, MAX(version) "max_version"
                          FROM tf_versions
                          GROUP BY hub_id)
    SELECT v.*
    FROM max_versions m,
         tf_versions v
    WHERE m.hub_id = v.hub_id
      AND m.max_version = v.version;

CREATE MATERIALIZED VIEW IF NOT EXISTS tf_latest_objects AS
SELECT o.*
FROM tf_latest_versions lv,
     tf_object_types ot,
     tf_objects o
WHERE lv.id = ot.version_id
  AND ot.id = o.object_type_id;

CREATE INDEX IF NOT EXISTS tf_latest_objects_id_index
    ON tf_latest_objects (id);

CREATE INDEX IF NOT EXISTS tf_latest_objects_object_type_id_index
    ON tf_latest_objects (object_type_id);

CREATE INDEX IF NOT EXISTS tf_latest_objects_content_index
    ON tf_latest_objects
        USING gin (content);

CREATE MATERIALIZED VIEW IF NOT EXISTS tf_metamodel AS
    WITH RECURSIVE collect_metadata AS (SELECT tf_latest_objects.object_type_id,
                                               first_level.key,
                                               first_level.value,
                                               jsonb_typeof(first_level.value) AS type
                                        FROM tf_latest_objects,
                                             jsonb_each(tf_latest_objects.content) first_level

                                        UNION ALL

                                        (WITH prev_level AS (
                                            SELECT *
                                            FROM collect_metadata
                                        )
                                         SELECT prev_level.object_type_id,
                                                concat(prev_level.key, '->', current_level.key),
                                                current_level.value,
                                                jsonb_typeof(current_level.value) AS type
                                         FROM prev_level,
                                              jsonb_each(prev_level.value) AS current_level
                                         WHERE prev_level.type = 'object'

                                         UNION ALL

                                         SELECT prev_level.object_type_id,
                                                concat(prev_level.key, '->', current_level.key),
                                                current_level.value,
                                                jsonb_typeof(current_level.value) AS type
                                         FROM prev_level,
                                              jsonb_array_elements(prev_level.value) AS entry,
                                              jsonb_each(entry) AS current_level
                                         WHERE prev_level.type = 'array'
                                           AND jsonb_typeof(entry) = 'object'

                                         UNION ALL

                                         SELECT prev_level.object_type_id,
                                                prev_level.key,
                                                entry,
                                                jsonb_typeof(entry) AS type
                                         FROM prev_level,
                                              jsonb_array_elements(prev_level.value) AS entry
                                         WHERE prev_level.type = 'array'
                                           AND jsonb_typeof(entry) <> 'object'))
    SELECT DISTINCT object_type_id,
                    key                                                 AS attribute,
                    array_to_json(ARRAY [collect_metadata.value]) ->> 0 AS value,
                    type
    FROM collect_metadata
    WHERE collect_metadata.type NOT IN ('object', 'array')
    WITH DATA;

CREATE MATERIALIZED VIEW IF NOT EXISTS tf_attributes AS
SELECT DISTINCT object_type_id, attribute
FROM tf_metamodel;

CREATE INDEX IF NOT EXISTS tf_attributes_object_type_id_attribute_index
    ON tf_attributes (object_type_id, attribute);

CREATE MATERIALIZED VIEW IF NOT EXISTS tf_array_of_objects AS
    WITH RECURSIVE collect_metadata AS (SELECT tf_latest_objects.object_type_id,
                                               first_level.key,
                                               NULL                            AS prev_key,
                                               first_level.value,
                                               jsonb_typeof(first_level.value) AS type
                                        FROM tf_latest_objects,
                                             jsonb_each(tf_latest_objects.content) first_level

                                        UNION ALL

                                        (WITH prev_level AS (
                                            SELECT *
                                            FROM collect_metadata
                                        )
                                         SELECT prev_level.object_type_id,
                                                concat(prev_level.key, '->', current_level.key),
                                                NULL                              AS prev_key,
                                                current_level.value,
                                                jsonb_typeof(current_level.value) AS type
                                         FROM prev_level,
                                              jsonb_each(prev_level.value) AS current_level
                                         WHERE prev_level.type = 'object'

                                         UNION ALL

                                         SELECT prev_level.object_type_id,
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

                                         SELECT prev_level.object_type_id,
                                                prev_level.key,
                                                NULL                AS prev_key,
                                                entry,
                                                jsonb_typeof(entry) AS type
                                         FROM prev_level,
                                              jsonb_array_elements(prev_level.value) AS entry
                                         WHERE prev_level.type = 'array'
                                           AND jsonb_typeof(entry) <> 'object'))
    SELECT DISTINCT object_type_id, prev_key AS attribute
    FROM collect_metadata
    WHERE prev_key IS NOT NULL
    WITH DATA;
