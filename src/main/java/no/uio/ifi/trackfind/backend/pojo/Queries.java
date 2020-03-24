package no.uio.ifi.trackfind.backend.pojo;

public interface Queries {

    String REFRESH_MATERIALIZED_VIEWS = "" +
            "REFRESH MATERIALIZED VIEW tf_current_objects;" +
            "REFRESH MATERIALIZED VIEW tf_metamodel;" +
            "REFRESH MATERIALIZED VIEW tf_array_of_objects;" +
            "REFRESH MATERIALIZED VIEW tf_attributes;" +
            "";

    String CHECK_SEARCH_USER_EXISTS = "SELECT count(*) FROM pg_catalog.pg_roles WHERE rolname = 'search'";

    String CREATE_SEARCH_USER = "CREATE USER search PASSWORD 'search'; GRANT SELECT ON tf_current_objects TO search;";

    String ADD_AT_LEST_ONE_ADMIN_CONSTRAINT = "" +
            "ALTER TABLE tf_users\n" +
            "    DROP CONSTRAINT IF EXISTS check_at_least_one_active_admin;" +
            "ALTER TABLE tf_users\n" +
            "    ADD CONSTRAINT check_at_least_one_active_admin CHECK ( check_at_least_one_active_admin(id, active, admin) );" +
            "";

}
