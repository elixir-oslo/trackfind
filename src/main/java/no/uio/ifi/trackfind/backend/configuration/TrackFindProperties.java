package no.uio.ifi.trackfind.backend.configuration;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

@Configuration
@PropertySource(value = {"classpath:trackfind.properties", "file:trackfind.properties"}, ignoreResourceNotFound = true)
@ConfigurationProperties
@Data
public class TrackFindProperties {

    private @NotBlank String indicesFolder;
    private @NotBlank String archiveFolder;

    private String gitRemote;
    private String gitToken;
    private boolean gitAutopush;

    private @NotBlank String scriptingLanguage;
    private @NotBlank String scriptingDatasetVariableName;

    private @NotBlank String advancedSectionName;
    private @NotBlank String basicSectionName;
    private @NotBlank String levelsSeparator;
    private @NotBlank String idAttribute;
    private @NotBlank String advancedIdAttribute;
    private @NotBlank String browserAttribute;
    private @NotBlank String dataTypeAttribute;
    private @NotBlank String dataURLAttribute;
    private @NotBlank String dataSourceAttribute;
    private @NotBlank String revisionAttribute;

    private List<String> basicAttributes;

    private boolean demoMode;

}
