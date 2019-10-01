package no.uio.ifi.trackfind.backend.startup;

import no.uio.ifi.trackfind.backend.data.providers.blueprint.BlueprintDataProvider;
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
public class BlueprintHubInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private TrackFindService trackFindService;
    private BlueprintDataProvider blueprintDataProvider;
    private MetamodelService metamodelService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String name = blueprintDataProvider.getName();
        Collection<TfHub> trackHubs = trackFindService.getTrackHubs(name, true);
        if (CollectionUtils.isEmpty(trackHubs)) {
            trackFindService.activateHubs(Collections.singleton(new TfHub(name, name, blueprintDataProvider.getFetchURI())));
            blueprintDataProvider.crawlRemoteRepository(name);
            Collection<TfObjectType> objectTypes = metamodelService.getObjectTypes(name, name);
            TfObjectType experiments = objectTypes.stream().filter(ot -> ot.getName().equalsIgnoreCase("experiments")).findAny().orElseThrow(RuntimeException::new);
            TfObjectType samples = objectTypes.stream().filter(ot -> ot.getName().equalsIgnoreCase("samples")).findAny().orElseThrow(RuntimeException::new);
            TfObjectType studies = objectTypes.stream().filter(ot -> ot.getName().equalsIgnoreCase("studies")).findAny().orElseThrow(RuntimeException::new);
            TfObjectType tracks = objectTypes.stream().filter(ot -> ot.getName().equalsIgnoreCase("tracks")).findAny().orElseThrow(RuntimeException::new);
            metamodelService.addReference(new TfReference(null, tracks, "'experiment_ref'", experiments, "'local_id'"));
            metamodelService.addReference(new TfReference(null, experiments, "'sample_ref'", samples, "'local_id'"));
            metamodelService.addReference(new TfReference(null, experiments, "'study_ref'", studies, "'local_id'"));
        }
    }

    @Autowired
    public void setTrackFindService(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

    @Autowired
    public void setBlueprintDataProvider(BlueprintDataProvider blueprintDataProvider) {
        this.blueprintDataProvider = blueprintDataProvider;
    }

    @Autowired
    public void setMetamodelService(MetamodelService metamodelService) {
        this.metamodelService = metamodelService;
    }

}
