package no.uio.ifi.trackfind.frontend;

import com.vaadin.data.HasValue;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
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
        tree.setSelectionMode(Grid.SelectionMode.MULTI);
        TrackDataProvider trackDataProvider = new TrackDataProvider(new TreeNode(trackFindService.getMetamodelTree()));
        tree.setDataProvider(trackDataProvider);

        HorizontalLayout headerLayout = new HorizontalLayout();

        TextField valuesFilterField = new TextField("Filter values", (HasValue.ValueChangeListener<String>) event -> {
            trackDataProvider.setValuesFilter(event.getValue());
            trackDataProvider.refreshAll();
        });
        valuesFilterField.setValueChangeMode(ValueChangeMode.EAGER);
        valuesFilterField.setWidth(100, Unit.PERCENTAGE);
        Panel treePanel = new Panel("Model Browser", tree);
        treePanel.setSizeFull();
        VerticalLayout treeLayout = new VerticalLayout(treePanel, valuesFilterField);
        treeLayout.setSizeFull();
        treeLayout.setExpandRatio(treePanel, 1f);

        Panel midPanel = new Panel("Something");
        VerticalLayout midLayout = new VerticalLayout(midPanel);

        Panel dataPanel = new Panel("Data");
        VerticalLayout dataLayout = new VerticalLayout(dataPanel);

        HorizontalLayout mainLayout = new HorizontalLayout(treeLayout, midLayout, dataLayout);
        mainLayout.setSizeFull();

        HorizontalLayout footerLayout = new HorizontalLayout();

        VerticalLayout outerLayout = new VerticalLayout(headerLayout, mainLayout, footerLayout);
        outerLayout.setSizeFull();
        outerLayout.setExpandRatio(mainLayout, 1f);

        setContent(outerLayout);
    }

}
