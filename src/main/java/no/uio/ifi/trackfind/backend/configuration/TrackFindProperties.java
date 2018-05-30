package no.uio.ifi.trackfind.backend.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Configuration
@PropertySources({
        @PropertySource("classpath:trackfind.properties"),
        @PropertySource(value = "file:trackfind.properties", ignoreResourceNotFound = true),
})
@Lazy
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
    private @NotBlank String scriptingResultVariableName;

    private @NotBlank String advancedSectionName;
    private @NotBlank String basicSectionName;
    private @NotBlank String levelsSeparator;
    private @NotBlank String idAttribute;
    private @NotBlank String advancedIdAttribute;
    private @NotBlank String revisionAttribute;

    private List<String> basicAttributes;

    private boolean demoMode;

}
