package no.uio.ifi.trackfind.frontend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vaadin.data.HasValue;
import com.vaadin.event.MouseEvents;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import no.uio.ifi.trackfind.backend.services.TrackFindService;
import no.uio.ifi.trackfind.frontend.data.TreeNode;
import no.uio.ifi.trackfind.frontend.providers.TrackDataProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Map;

@SpringUI
public class TrackFindUI extends UI {

    private final TrackFindService trackFindService;
    private final Gson gson;

    @Autowired
    public TrackFindUI(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        Label headerLabel = new Label("TrackFind");
        HorizontalLayout headerLayout = new HorizontalLayout(headerLabel);
        headerLayout.setSizeFull();
        headerLayout.setComponentAlignment(headerLabel, Alignment.TOP_CENTER);

        Tree<TreeNode> tree = new Tree<>();
        tree.setSelectionMode(Grid.SelectionMode.MULTI);
        TrackDataProvider trackDataProvider = new TrackDataProvider(new TreeNode(trackFindService.getMetamodelTree()));
        tree.setDataProvider(trackDataProvider);
        TextField valuesFilterTextField = new TextField("Filter values", (HasValue.ValueChangeListener<String>) event -> {
            trackDataProvider.setValuesFilter(event.getValue());
            trackDataProvider.refreshAll();
        });
        valuesFilterTextField.setValueChangeMode(ValueChangeMode.EAGER);
        valuesFilterTextField.setWidth(100, Unit.PERCENTAGE);
        tree.setSizeFull();
        Panel treePanel = new Panel("Model browser", tree);
        treePanel.setSizeFull();
        VerticalLayout treeLayout = new VerticalLayout(treePanel, valuesFilterTextField);
        treeLayout.setSizeFull();
        treeLayout.setExpandRatio(treePanel, 1f);

        TextArea queryTextArea = new TextArea();
        queryTextArea.setSizeFull();
        Panel queryPanel = new Panel("Search query", queryTextArea);
        queryPanel.setSizeFull();
        TextField sampleQueryTextField = new TextField("Sample query", "sample_id: SRS306625_*_471 OR other_attributes>lab: U??D AND ihec_data_portal>assay: (WGB-Seq OR something)");
        sampleQueryTextField.setEnabled(false);
        sampleQueryTextField.setWidth(100, Unit.PERCENTAGE);
        VerticalLayout queryLayout = new VerticalLayout(queryPanel, sampleQueryTextField);
        queryLayout.setSizeFull();
        queryLayout.setExpandRatio(queryPanel, 1f);

        TextArea dataTextArea = new TextArea();
        dataTextArea.setSizeFull();
        dataTextArea.setReadOnly(true);
        Panel dataPanel = new Panel("Data", dataTextArea);
        dataPanel.setSizeFull();
        queryPanel.addClickListener((MouseEvents.ClickListener) event -> {
            Collection<Map> result = trackFindService.search(sampleQueryTextField.getValue());
            dataTextArea.setValue(gson.toJson(result));
        });
        VerticalLayout dataLayout = new VerticalLayout(dataPanel);
        dataLayout.setSizeFull();

        HorizontalLayout mainLayout = new HorizontalLayout(treeLayout, queryLayout, dataLayout);
        mainLayout.setSizeFull();

        Label footerLabel = new Label("2017");
        HorizontalLayout footerLayout = new HorizontalLayout(footerLabel);
        footerLayout.setSizeFull();
        footerLayout.setComponentAlignment(footerLabel, Alignment.BOTTOM_CENTER);

        VerticalLayout outerLayout = new VerticalLayout(headerLayout, mainLayout, footerLayout);
        outerLayout.setSizeFull();
        outerLayout.setExpandRatio(headerLayout, 0.05f);
        outerLayout.setExpandRatio(mainLayout, 0.9f);
        outerLayout.setExpandRatio(footerLayout, 0.05f);

        setContent(outerLayout);
    }

}
