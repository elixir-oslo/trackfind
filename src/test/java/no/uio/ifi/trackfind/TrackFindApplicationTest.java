package no.uio.ifi.trackfind;

import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import no.uio.ifi.trackfind.metamodel.Metamodel;
import no.uio.ifi.trackfind.model.Dataset;
import no.uio.ifi.trackfind.model.Grid;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.util.Collection;
import java.util.Map;

public class TrackFindApplicationTest {

    private Gson gson = new Gson();
    private Collection<Dataset> datasets;

    @Before
    public void setUp() throws Exception {
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

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void main() throws Exception {
        Metamodel metamodel = new Metamodel();
        for (Dataset dataset : datasets) {
            processAttributes(dataset.getAnalysisAttributes(), metamodel.getAnalysisAttributes());
            processAttributes(dataset.getExperimentAttributes(), metamodel.getExperimentAttributes());
            processAttributes(dataset.getIhecDataPortal(), metamodel.getIhecDataPortal());
            processAttributes(dataset.getOtherAttributes(), metamodel.getOtherAttributes());
            processAttributes(dataset.getSampleAttributes(), metamodel.getSampleAttributes());
        }
        System.out.println(gson.toJson(metamodel));
    }

    private void processAttributes(Map<String, String> modelAttributes, Multimap<String, String> metamodelAttributes) {
        if (modelAttributes == null || metamodelAttributes == null) {
            return;
        }
        for (Map.Entry<String, String> entry : modelAttributes.entrySet()) {
            metamodelAttributes.put(entry.getKey(), entry.getValue());
        }
    }

}