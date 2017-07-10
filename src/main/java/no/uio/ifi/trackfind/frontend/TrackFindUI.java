package no.uio.ifi.trackfind.frontend;

import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Tree;
import com.vaadin.ui.UI;
import no.uio.ifi.trackfind.backend.services.TrackFindService;
import no.uio.ifi.trackfind.frontend.data.TreeNode;
import no.uio.ifi.trackfind.frontend.providers.TrackDataProvider;
import org.springframework.beans.factory.annotation.Autowired;

@SpringUI
public class TrackFindUI extends UI {

    private final TrackFindService trackFindService;

    @Autowired
    public TrackFindUI(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        Tree<TreeNode> tree = new Tree<>();
        TrackDataProvider trackDataProvider = new TrackDataProvider(new TreeNode(trackFindService.getMetamodelTree()));
        tree.setDataProvider(trackDataProvider);
        setContent(tree);
    }

}
