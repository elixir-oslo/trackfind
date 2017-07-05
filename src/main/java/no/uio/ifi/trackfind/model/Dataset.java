package no.uio.ifi.trackfind.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.Map;

@Data
public class Dataset {

    private String id;

    @SerializedName("analysis_attributes")
    private Map<String, String> analysisAttributes;

    @SerializedName("experiment_attributes")
    private Map<String, String> experimentAttributes;

    @SerializedName("ihec_data_portal")
    private Map<String, String> ihecDataPortal;

    @SerializedName("other_attributes")
    private Map<String, String> otherAttributes;

    @SerializedName("raw_data_url")
    private String rawDataUrl;

    @SerializedName("sample_id")
    private String sampleId;

    private Map<String, String> sampleAttributes;

}
