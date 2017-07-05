package no.uio.ifi.trackfind.services;

import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import no.uio.ifi.trackfind.metamodel.Metamodel;
import no.uio.ifi.trackfind.model.Dataset;
import no.uio.ifi.trackfind.model.Grid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collection;
import java.util.Map;

@Service
public class TrackFindService {

    private final Gson gson;

    private Collection<Dataset> datasets;
    private Metamodel metamodel;

    @Autowired
    public TrackFindService(Gson gson) {
        this.gson = gson;
    }

    @PostConstruct
    public void postConstruct() throws Exception {
        fillDatasets();
        fillMetamodel();
    }

    public Metamodel getMetamodel() {
        return metamodel;
    }

    private void fillDatasets() throws FileNotFoundException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("getDataHub.json").getFile());
        JsonReader reader = new JsonReader(new FileReader(file));
        Grid grid = gson.fromJson(reader, Grid.class);

        for (Map.Entry<String, Dataset> entry : grid.getDatasets().entrySet()) {
            String id = entry.getKey();
            Dataset dataset = entry.getValue();
            dataset.setId(id);
            dataset.setSampleAttributes(grid.getSamples().get(dataset.getSampleId()));
        }

        datasets = grid.getDatasets().values();
    }

    private void fillMetamodel() {
        metamodel = new Metamodel();
        for (Dataset dataset : datasets) {
            processAttributes(dataset.getAnalysisAttributes(), metamodel.getAnalysisAttributes());
            processAttributes(dataset.getExperimentAttributes(), metamodel.getExperimentAttributes());
            processAttributes(dataset.getIhecDataPortal(), metamodel.getIhecDataPortal());
            processAttributes(dataset.getOtherAttributes(), metamodel.getOtherAttributes());
            processAttributes(dataset.getSampleAttributes(), metamodel.getSampleAttributes());
        }
    }

    private void processAttributes(Map<String, String> modelAttributes, Multimap<String, String> metamodelAttributes) {
        if (modelAttributes == null || metamodelAttributes == null) {
            return;
        }
        for (Map.Entry<String, String> entry : modelAttributes.entrySet()) {
            String attribute = entry.getKey();
            String value = entry.getValue();
            if (StringUtils.isEmpty(value) || value.contains("http://")) {
                continue;
            }
            metamodelAttributes.put(attribute, value);
        }
    }


}
