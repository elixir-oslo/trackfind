package no.uio.ifi.trackfind.backend.operations;

/**
 * Possible operations on the data:
 * - fetching it from the remote source;
 * - curating data;
 * - mapping attributes.
 */
public enum Operation {

    CRAWLING, CURATION, VERSION_CHANGE

}
