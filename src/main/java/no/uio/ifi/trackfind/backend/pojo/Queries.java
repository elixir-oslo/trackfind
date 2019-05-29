package no.uio.ifi.trackfind.backend.pojo;

public interface Queries {

    String REFRESH_MATERIALIZED_VIEWS = "" +
            "REFRESH MATERIALIZED VIEW tf_latest_objects;" +
            "REFRESH MATERIALIZED VIEW tf_metamodel;" +
            "REFRESH MATERIALIZED VIEW tf_array_of_objects;" +
            "REFRESH MATERIALIZED VIEW tf_attributes;" +
            "";

    String CHECK_SEARCH_USER_EXISTS = "SELECT count(*) FROM pg_catalog.pg_roles WHERE rolname = 'search'";

    String CREATE_SEARCH_USER = "CREATE USER search PASSWORD 'search'; GRANT SELECT ON tf_latest_objects TO search;";

}
