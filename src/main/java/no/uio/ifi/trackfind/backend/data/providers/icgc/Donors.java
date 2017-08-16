package no.uio.ifi.trackfind.backend.data.providers.icgc;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.Collection;
import java.util.Map;

/**
 * POJO for deserialization of donors data from ICGC (using Gson).
 */
@Data
class Donors {

    @SerializedName("hits")
    private Collection<Map<String, Object>> hits;

    @SerializedName("pagination")
    private Pagination pagination;

}
