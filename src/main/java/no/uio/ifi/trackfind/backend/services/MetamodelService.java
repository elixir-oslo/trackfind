package no.uio.ifi.trackfind.backend.services;

import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for getting metamodel information: attibutes, subattributes, values, etc.
 */
// TODO: cover with tests
@Service
public class MetamodelService {

    private TrackFindService trackFindService;
    private TrackFindProperties properties;

    @Cacheable("metamodel-attributes")
    public Collection<String> getAttributes(String provider, String filter, boolean raw, boolean top) {
        DataProvider dataProvider = trackFindService.getDataProvider(provider);
        Set<String> attributes = top ? dataProvider.getMetamodelTree(raw).keySet() : dataProvider.getMetamodelFlat(raw).asMap().keySet();
        return attributes.parallelStream().filter(a -> a.contains(filter)).collect(Collectors.toSet());
    }

    @Cacheable("metamodel-subattributes")
    public Collection<String> getSubAttributes(String provider, String attribute, String filter, boolean raw) {
        DataProvider dataProvider = trackFindService.getDataProvider(provider);
        Set<String> attributes = dataProvider.getMetamodelFlat(raw).asMap().keySet();
        Set<String> filteredAttributes = attributes.stream().filter(a -> a.contains(filter)).collect(Collectors.toSet());
        String separator = properties.getLevelsSeparator();
        if (filteredAttributes.contains(attribute)) {
            return Collections.emptySet();
        }
        return filteredAttributes
                .parallelStream()
                .filter(a -> a.startsWith(attribute))
                .map(a -> a.replace(attribute, ""))
                .map(a -> (a.contains(separator) ? a.substring(separator.length()) : a))
                .map(a -> (a.contains(separator) ? a.substring(0, a.indexOf(separator)) : a))
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
    }

    @Cacheable("metamodel-values")
    public Collection<String> getValues(String provider, String attribute, String filter, boolean raw) {
        DataProvider dataProvider = trackFindService.getDataProvider(provider);
        return dataProvider.getMetamodelFlat(raw).get(attribute).parallelStream().filter(a -> a.contains(filter)).collect(Collectors.toSet());
    }

    @Autowired
    public void setTrackFindService(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

    @Autowired
    public void setProperties(TrackFindProperties properties) {
        this.properties = properties;
    }

}
