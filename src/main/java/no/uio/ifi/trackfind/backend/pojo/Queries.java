package no.uio.ifi.trackfind.backend.pojo;

public interface Queries {

    String REFRESH_MATERIALIZED_VIEWS = "" +
            "REFRESH MATERIALIZED VIEW datasets;" +
            "REFRESH MATERIALIZED VIEW latest_datasets;" +
            "REFRESH MATERIALIZED VIEW source_metamodel;" +
            "REFRESH MATERIALIZED VIEW standard_metamodel;" +
            "REFRESH MATERIALIZED VIEW fair_metamodel;" +
            "REFRESH MATERIALIZED VIEW source_array_of_objects;" +
            "REFRESH MATERIALIZED VIEW standard_array_of_objects;" +
            "REFRESH MATERIALIZED VIEW fair_array_of_objects;" +
            "";

    String CHECK_SEARCH_USER_EXISTS = "SELECT count(*) FROM pg_catalog.pg_roles WHERE rolname = 'search'";

    String CREATE_SEARCH_USER = "CREATE USER search PASSWORD 'search'; GRANT SELECT ON ALL TABLES IN SCHEMA public TO search;";

}
