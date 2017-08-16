package no.uio.ifi.trackfind.backend.data.providers;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.Collection;
import java.util.Map;

/**
 * Page interface.
 */
public interface Page {

    /**
     * Gets total number of pages.
     *
     * @return Total number of pages.
     */
    long getPagesTotal();

    /**
     * Get entries of this page.
     *
     * @return Entries on this page.
     */
    Collection<Map<String, Object>> getEntries();

    /**
     * POJO for deserialization of pagination data (using Gson).
     */
    @Data
    class Pagination {

        @SerializedName("count")
        private Integer count;

        @SerializedName("total")
        private Long total;

        @SerializedName("size")
        private Integer size;

        @SerializedName("from")
        private Integer from;

        @SerializedName("page")
        private Integer page;

        @SerializedName("pages")
        private Long pages;

        @SerializedName("sort")
        private String sort;

    }

}
