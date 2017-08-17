package no.uio.ifi.trackfind.backend.data.providers.gdc;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import no.uio.ifi.trackfind.backend.data.providers.Page;
import no.uio.ifi.trackfind.backend.data.providers.icgc.ICGCPage;

import java.util.Collection;
import java.util.Map;

/**
 * POJO for deserialization of paginated entries from GDC (using Gson).
 *
 * @author Dmytro Titov
 */
@Data
public class GDCPage implements Page {

    @SerializedName("data")
    private ICGCPage subpage;

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPagesTotal() {
        return getSubpage().getPagination().getPages();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Map<String, Object>> getEntries() {
        return getSubpage().getEntries();
    }

}