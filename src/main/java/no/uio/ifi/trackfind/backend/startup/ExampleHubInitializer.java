package no.uio.ifi.trackfind.backend.startup;

import no.uio.ifi.trackfind.backend.data.providers.example.ExampleDataProvider;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import no.uio.ifi.trackfind.backend.pojo.TfObjectType;
import no.uio.ifi.trackfind.backend.pojo.TfReference;
import no.uio.ifi.trackfind.backend.services.impl.MetamodelService;
import no.uio.ifi.trackfind.backend.services.impl.TrackFindService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;

@Component
@Profile("!prod")
public class ExampleHubInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private TrackFindService trackFindService;
    private ExampleDataProvider exampleDataProvider;
    private MetamodelService metamodelService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String name = exampleDataProvider.getName();
        Collection<TfHub> trackHubs = trackFindService.getTrackHubs(name, true);
        if (CollectionUtils.isEmpty(trackHubs)) {
            trackFindService.activateHubs(Collections.singleton(new TfHub(name, name, exampleDataProvider.getFetchURI(name))));
            exampleDataProvider.crawlRemoteRepository(name);
            Collection<TfObjectType> objectTypes = metamodelService.getObjectTypes(name, name);
            TfObjectType experiments = objectTypes.stream().filter(ot -> ot.getName().equalsIgnoreCase("experiments")).findAny().orElseThrow(RuntimeException::new);
            TfObjectType nonStandardSamples = objectTypes.stream().filter(ot -> ot.getName().equalsIgnoreCase("non_standard_samples")).findAny().orElseThrow(RuntimeException::new);
            TfObjectType samples = objectTypes.stream().filter(ot -> ot.getName().equalsIgnoreCase("samples")).findAny().orElseThrow(RuntimeException::new);
            TfObjectType studies = objectTypes.stream().filter(ot -> ot.getName().equalsIgnoreCase("studies")).findAny().orElseThrow(RuntimeException::new);
            TfObjectType tracks = objectTypes.stream().filter(ot -> ot.getName().equalsIgnoreCase("tracks")).findAny().orElseThrow(RuntimeException::new);
            metamodelService.addReference(new TfReference(null, tracks, "'experiment_ref'", experiments, "'local_id'"));
            metamodelService.addReference(new TfReference(null, experiments, "'sample_ref'", samples, "'local_id'"));
            metamodelService.addReference(new TfReference(null, experiments, "'sample_ref'", nonStandardSamples, "'local_id'"));
            metamodelService.addReference(new TfReference(null, experiments, "'study_ref'", studies, "'local_id'"));
        }
    }

    @Autowired
    public void setTrackFindService(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

    @Autowired
    public void setExampleDataProvider(ExampleDataProvider exampleDataProvider) {
        this.exampleDataProvider = exampleDataProvider;
    }

    @Autowired
    public void setMetamodelService(MetamodelService metamodelService) {
        this.metamodelService = metamodelService;
    }

}
