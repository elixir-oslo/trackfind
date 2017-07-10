package no.uio.ifi.trackfind.frontend;

import com.google.common.collect.Multimap;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Tree;
import com.vaadin.ui.UI;
import no.uio.ifi.trackfind.backend.services.TrackFindService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@SpringUI
public class TrackFindUI extends UI {

    private final TrackFindService trackFindService;

    @Autowired
    public TrackFindUI(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        Multimap<String, String> metamodelFlat = trackFindService.getMetamodelFlat();
        Tree<String> tree = new Tree<>();
        TreeData<String> treeData = new TreeData<>();
        for (String attribute : metamodelFlat.keySet()) {
            treeData.addItem(null, attribute);
        }
        for (Map.Entry<String, String> entry : metamodelFlat.entries()) {
            try {
                treeData.addItem(entry.getKey(), entry.getValue());
            } catch (IllegalArgumentException e) {
                System.out.println("e = " + e);
            }
        }
        TreeDataProvider<String> treeDataProvider = new TreeDataProvider<>(treeData);
        tree.setDataProvider(treeDataProvider);
        setContent(tree);
    }

}
