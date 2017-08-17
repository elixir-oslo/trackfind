package no.uio.ifi.trackfind.backend.data.providers.icgc;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * POJO for deserialization of Download ID from ICGC (using Gson).
 *
 * @author Dmytro Titov
 */
@Data
class Download {

    @SerializedName("downloadId")
    private String downloadId;

}