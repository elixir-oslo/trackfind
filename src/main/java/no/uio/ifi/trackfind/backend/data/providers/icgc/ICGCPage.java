package no.uio.ifi.trackfind.backend.data.providers.icgc;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import no.uio.ifi.trackfind.backend.data.providers.Page;

import java.util.Collection;
import java.util.Map;

/**
 * POJO for deserialization of paginated entries from ICGC (using Gson).
 */
@Data
public class ICGCPage implements Page {

    @SerializedName("hits")
    private Collection<Map<String, Object>> entries;

    @SerializedName("pagination")
    private Pagination pagination;

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPagesTotal() {
        return getPagination().getPages();
    }

}