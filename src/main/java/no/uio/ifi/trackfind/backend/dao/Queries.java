package no.uio.ifi.trackfind.backend.dao;

public interface Queries {

    String METAMODEL_VIEW = "CREATE OR REPLACE VIEW %s_metamodel AS\n" +
            "  WITH RECURSIVE collect_metadata AS (SELECT datasets.repository,\n" +
            "                                             first_level.key,\n" +
            "                                             first_level.value,\n" +
            "                                             jsonb_typeof(first_level.value) AS type\n" +
            "                                      FROM datasets,\n" +
            "                                           jsonb_each(datasets.%s_content) first_level\n" +
            "                                      GROUP BY datasets.repository, first_level.key, first_level.value, raw_version, curated_version, standard_version\n" +
            "                                      HAVING raw_version = MAX(raw_version)\n" +
            "                                         AND curated_version = MAX(curated_version)\n" +
            "                                         AND standard_version = MAX(standard_version)\n" +
            "\n" +
            "                                      UNION ALL\n" +
            "\n" +
            "                                      (WITH prev_level AS (\n" +
            "                                          SELECT *\n" +
            "                                          FROM collect_metadata\n" +
            "                                      )\n" +
            "                                      SELECT prev_level.repository,\n" +
            "                                             concat(prev_level.key, '%s', current_level.key),\n" +
            "                                             current_level.value,\n" +
            "                                             jsonb_typeof(current_level.value) AS type\n" +
            "                                      FROM prev_level,\n" +
            "                                           jsonb_each(prev_level.value) AS current_level\n" +
            "                                      WHERE prev_level.type = 'object'\n" +
            "\n" +
            "                                      UNION ALL\n" +
            "\n" +
            "                                      SELECT prev_level.repository,\n" +
            "                                             concat(prev_level.key, '%s', current_level.key),\n" +
            "                                             current_level.value,\n" +
            "                                             jsonb_typeof(current_level.value) AS type\n" +
            "                                      FROM prev_level,\n" +
            "                                           jsonb_array_elements(prev_level.value) AS entry,\n" +
            "                                           jsonb_each(entry) AS current_level\n" +
            "                                      WHERE prev_level.type = 'array'\n" +
            "                                        AND jsonb_typeof(entry) = 'object'\n" +
            "\n" +
            "                                      UNION ALL\n" +
            "\n" +
            "                                      SELECT prev_level.repository,\n" +
            "                                             prev_level.key,\n" +
            "                                             entry,\n" +
            "                                             jsonb_typeof(entry) AS type\n" +
            "                                      FROM prev_level,\n" +
            "                                           jsonb_array_elements(prev_level.value) AS entry\n" +
            "                                      WHERE prev_level.type = 'array'\n" +
            "                                        AND jsonb_typeof(entry) <> 'object'))\n" +
            "  SELECT DISTINCT repository, key AS attribute, array_to_json(ARRAY[collect_metadata.value])->>0 AS value, type\n" +
            "  FROM collect_metadata\n" +
            "  WHERE collect_metadata.type NOT IN ('object', 'array')";

}
