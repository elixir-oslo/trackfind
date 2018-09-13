package no.uio.ifi.trackfind.backend.dao;

public interface Queries {

    String METAMODEL_VIEW = "\n" +
            "CREATE OR REPLACE VIEW metamodel AS\n" +
            "  WITH RECURSIVE collect_metadata AS (\n" +
            "    SELECT datasets.repository,\n" +
            "           datasets.version,\n" +
            "           first_level.key,\n" +
            "           first_level.value,\n" +
            "           json_typeof(first_level.value) AS type\n" +
            "    FROM datasets,\n" +
            "         json_each(datasets.raw_dataset) first_level\n" +
            "\n" +
            "    UNION ALL\n" +
            "\n" +
            "    (WITH prev_level AS (\n" +
            "        SELECT *\n" +
            "        FROM collect_metadata\n" +
            "    )\n" +
            "    SELECT prev_level.repository,\n" +
            "           prev_level.version,\n" +
            "           concat(prev_level.key, '>', current_level.key),\n" +
            "           current_level.value,\n" +
            "           json_typeof(current_level.value) AS type\n" +
            "    FROM prev_level,\n" +
            "         json_each(prev_level.value) AS current_level\n" +
            "    WHERE prev_level.type = 'object'\n" +
            "\n" +
            "    UNION ALL\n" +
            "\n" +
            "    SELECT prev_level.repository,\n" +
            "           prev_level.version,\n" +
            "           concat(prev_level.key, '>', current_level.key),\n" +
            "           current_level.value,\n" +
            "           json_typeof(current_level.value) AS type\n" +
            "    FROM prev_level,\n" +
            "         json_array_elements(prev_level.value) AS entries,\n" +
            "         json_each(entries) AS current_level\n" +
            "    WHERE prev_level.type = 'array')\n" +
            "  )\n" +
            "  SELECT DISTINCT repository, version, key AS attribute, array_to_json(ARRAY[collect_metadata.value])->>0 AS value , type\n" +
            "  FROM collect_metadata\n" +
            "  WHERE collect_metadata.type NOT IN ('object', 'array')";

}
