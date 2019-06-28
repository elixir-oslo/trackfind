package no.uio.ifi.trackfind.backend.startup;

import no.uio.ifi.trackfind.backend.data.providers.example.ExampleDataProvider;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import no.uio.ifi.trackfind.backend.pojo.TfObjectType;
import no.uio.ifi.trackfind.backend.pojo.TfReference;
import no.uio.ifi.trackfind.backend.services.MetamodelService;
import no.uio.ifi.trackfind.backend.services.TrackFindService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;

@Component
public class ExampleHubInitializer implements ApplicationListener<ApplicationReadyEvent> {

    public static final String TEST_ENV = "TEST_ENV";
    public static final String EXAMPLE = "Example";

    private TrackFindService trackFindService;
    private ExampleDataProvider exampleDataProvider;
    private MetamodelService metamodelService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String testEnv = System.getenv(TEST_ENV);
        if (!"true".equalsIgnoreCase(testEnv)) {
            return;
        }

        try {
            trackFindService.activateHubs(Collections.singleton(new TfHub(EXAMPLE, EXAMPLE)));
        } catch (DataIntegrityViolationException ignore) {
        }
        exampleDataProvider.crawlRemoteRepository(EXAMPLE);
        Collection<TfObjectType> objectTypes = metamodelService.getObjectTypes(EXAMPLE, EXAMPLE);
        TfObjectType experiments = objectTypes.stream().filter(ot -> ot.getName().equalsIgnoreCase("experiments")).findAny().get();
        TfObjectType nonStandardSamples = objectTypes.stream().filter(ot -> ot.getName().equalsIgnoreCase("non_standard_samples")).findAny().get();
        TfObjectType samples = objectTypes.stream().filter(ot -> ot.getName().equalsIgnoreCase("samples")).findAny().get();
        TfObjectType studies = objectTypes.stream().filter(ot -> ot.getName().equalsIgnoreCase("studies")).findAny().get();
        TfObjectType tracks = objectTypes.stream().filter(ot -> ot.getName().equalsIgnoreCase("tracks")).findAny().get();
        metamodelService.addReference(new TfReference(null, tracks, "'experiment_ref'", experiments, "'local_id'"));
        metamodelService.addReference(new TfReference(null, experiments, "'sample_ref'", samples, "'local_id'"));
        metamodelService.addReference(new TfReference(null, experiments, "'sample_ref'", nonStandardSamples, "'local_id'"));
        metamodelService.addReference(new TfReference(null, experiments, "'study_ref'", studies, "'local_id'"));
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
