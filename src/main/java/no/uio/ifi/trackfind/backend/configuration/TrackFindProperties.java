package no.uio.ifi.trackfind.backend.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Component
@PropertySources({
        @PropertySource("classpath:trackfind.properties"),
        @PropertySource(value = "file:trackfind.properties", ignoreResourceNotFound = true),
})
@ConfigurationProperties
@Data
public class TrackFindProperties {

    private @NotBlank String fairFieldName;
    private @NotBlank String levelsSeparator;
    private @NotBlank String scriptingLanguage;
    private @NotBlank String scriptingDatasetVariableName;
    private @NotBlank String scriptingResultVariableName;
    private boolean demoMode;

}
