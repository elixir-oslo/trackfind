package no.uio.ifi.trackfind.backend.data.providers.ihec;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.Date;

/**
 * POJO for deserialization of Release data from IHEC (using Gson).
 */
@Data
class Release implements Comparable<Release> {

    @SerializedName("assembly")
    private String assembly;

    @SerializedName("id")
    private Integer id;

    @SerializedName("integration_date")
    private Date integrationDate;

    @SerializedName("publishing_group")
    private String publishingGroup;

    @SerializedName("release_policies_url")
    private String releasePoliciesUrl;

    @SerializedName("releasing_group")
    private String releasingGroup;

    @SerializedName("species")
    private String species;

    @SerializedName("taxon_id")
    private Integer taxonId;

    @Override
    public int compareTo(Release that) {
        return this.getId().compareTo(that.getId());
    }

}