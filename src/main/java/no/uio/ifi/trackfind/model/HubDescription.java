package no.uio.ifi.trackfind.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class HubDescription {


    @SerializedName("assembly")
    public String assembly;

    @SerializedName("date")
    public String date;

    @SerializedName("description")
    public String description;

    @SerializedName("email")
    public String email;

    @SerializedName("publishing_group")
    public String publishingGroup;

    @SerializedName("releasing_group")
    public String releasingGroup;

    @SerializedName("taxon_id")
    public Integer taxonId;

}
