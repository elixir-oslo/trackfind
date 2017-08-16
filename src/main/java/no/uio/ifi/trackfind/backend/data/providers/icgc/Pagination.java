package no.uio.ifi.trackfind.backend.data.providers.icgc;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * POJO for deserialization of pagination data from ICGC (using Gson).
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
    private Integer pages;

    @SerializedName("sort")
    private String sort;

    @SerializedName("order")
    private String order;

}