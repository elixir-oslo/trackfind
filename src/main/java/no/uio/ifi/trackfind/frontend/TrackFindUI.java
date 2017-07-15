package no.uio.ifi.trackfind.frontend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.HasValue;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.shared.ui.dnd.DropEffect;
import com.vaadin.shared.ui.dnd.EffectAllowed;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import com.vaadin.ui.components.grid.TreeGridDragSource;
import com.vaadin.ui.dnd.DropTargetExtension;
import no.uio.ifi.trackfind.backend.services.TrackFindService;
import no.uio.ifi.trackfind.frontend.components.KeyboardInterceptorExtension;
import no.uio.ifi.trackfind.frontend.components.TrackFindTree;
import no.uio.ifi.trackfind.frontend.data.TreeNode;
import no.uio.ifi.trackfind.frontend.listeners.TextAreaDropListener;
import no.uio.ifi.trackfind.frontend.listeners.TreeItemClickListener;
import no.uio.ifi.trackfind.frontend.listeners.TreeSelectionListener;
import no.uio.ifi.trackfind.frontend.providers.TrackDataProvider;
import org.springframework.beans.factory.annotation.Autowired;

@SpringUI
@Widgetset("TrackFindWidgetSet")
@Title("TrackFind")
@Theme("trackfind")
public class TrackFindUI extends UI {

    private final TrackFindService trackFindService;
    private final Gson gson;

    private TextArea queryTextArea;
    private TextArea dataTextArea;

    @Autowired
    public TrackFindUI(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        HorizontalLayout headerLayout = buildHeaderLayout();
        VerticalLayout treeLayout = buildTreeLayout();
        VerticalLayout queryLayout = buildQueryLayout();
        VerticalLayout dataLayout = buildDataLayout();
        HorizontalLayout mainLayout = buildMainLayout(treeLayout, queryLayout, dataLayout);
        HorizontalLayout footerLayout = buildFooterLayout();
        VerticalLayout outerLayout = buildOuterLayout(headerLayout, mainLayout, footerLayout);
        setContent(outerLayout);
    }

    private VerticalLayout buildOuterLayout(HorizontalLayout headerLayout, HorizontalLayout mainLayout, HorizontalLayout footerLayout) {
        VerticalLayout outerLayout = new VerticalLayout(headerLayout, mainLayout, footerLayout);
        outerLayout.setSizeFull();
        outerLayout.setExpandRatio(headerLayout, 0.05f);
        outerLayout.setExpandRatio(mainLayout, 0.9f);
        outerLayout.setExpandRatio(footerLayout, 0.05f);
        return outerLayout;
    }

    private HorizontalLayout buildMainLayout(VerticalLayout treeLayout, VerticalLayout queryLayout, VerticalLayout dataLayout) {
        HorizontalLayout mainLayout = new HorizontalLayout(treeLayout, queryLayout, dataLayout);
        mainLayout.setSizeFull();
        return mainLayout;
    }

    private VerticalLayout buildDataLayout() {
        dataTextArea = new TextArea();
        dataTextArea.setSizeFull();
        dataTextArea.setReadOnly(true);
        dataTextArea.addStyleName("scrollable-text-area");
        Panel dataPanel = new Panel("Data", dataTextArea);
        dataPanel.setSizeFull();

        VerticalLayout dataLayout = new VerticalLayout(dataPanel);
        dataLayout.setSizeFull();
        return dataLayout;
    }

    private VerticalLayout buildQueryLayout() {
        queryTextArea = new TextArea();
        queryTextArea.setSizeFull();
        queryTextArea.addShortcutListener(new ShortcutListener("Execute query", ShortcutAction.KeyCode.ENTER, new int[]{ShortcutAction.ModifierKey.CTRL}) {
            @Override
            public void handleAction(Object sender, Object target) {
                executeQuery(queryTextArea.getValue());
            }
        });
        queryTextArea.addShortcutListener(new ShortcutListener("Execute query", ShortcutAction.KeyCode.ENTER, new int[]{ShortcutAction.ModifierKey.META}) {
            @Override
            public void handleAction(Object sender, Object target) {
                executeQuery(queryTextArea.getValue());
            }
        });
        DropTargetExtension<TextArea> dropTarget = new DropTargetExtension<>(queryTextArea);
        dropTarget.setDropEffect(DropEffect.COPY);
        dropTarget.addDropListener(new TextAreaDropListener(queryTextArea));
        Panel queryPanel = new Panel("Search query", queryTextArea);
        queryPanel.setSizeFull();
        TextField sampleQueryTextField = new TextField("Sample query", "sample_id: SRS306625_*_471 OR other_attributes>lab: U??D AND ihec_data_portal>assay: (WGB-Seq OR something)");
        sampleQueryTextField.setEnabled(false);
        sampleQueryTextField.setWidth(100, Unit.PERCENTAGE);
        VerticalLayout queryLayout = new VerticalLayout(queryPanel, sampleQueryTextField);
        queryLayout.setSizeFull();
        queryLayout.setExpandRatio(queryPanel, 1f);
        return queryLayout;
    }

    private void executeQuery(String query) {
        dataTextArea.setValue(gson.toJson(trackFindService.search(query)));
    }

    @SuppressWarnings("unchecked")
    private VerticalLayout buildTreeLayout() {
        TrackFindTree<TreeNode> tree = new TrackFindTree<>();
        tree.setSelectionMode(Grid.SelectionMode.MULTI);
        tree.addItemClickListener(new TreeItemClickListener(tree));
        tree.addSelectionListener(new TreeSelectionListener(tree, new KeyboardInterceptorExtension(tree)));
        TreeGridDragSource<TreeNode> dragSource = new TreeGridDragSource<>((TreeGrid<TreeNode>) tree.getCompositionRoot());
        dragSource.setEffectAllowed(EffectAllowed.COPY);
        TrackDataProvider trackDataProvider = new TrackDataProvider(new TreeNode(trackFindService.getMetamodelTree()));
        tree.setDataProvider(trackDataProvider);
        TextField valuesFilterTextField = new TextField("Filter values", (HasValue.ValueChangeListener<String>) event -> {
            trackDataProvider.setValuesFilter(event.getValue());
            trackDataProvider.refreshAll();
        });
        valuesFilterTextField.setValueChangeMode(ValueChangeMode.EAGER);
        valuesFilterTextField.setWidth(100, Unit.PERCENTAGE);
        tree.setSizeFull();
        tree.setStyleGenerator((StyleGenerator<TreeNode>) item -> item.isFinalAttribute() || item.isLeaf() ? null : "disabled-tree-node");
        Panel treePanel = new Panel("Model browser", tree);
        treePanel.setSizeFull();
        VerticalLayout treeLayout = new VerticalLayout(treePanel, valuesFilterTextField);
        treeLayout.setSizeFull();
        treeLayout.setExpandRatio(treePanel, 1f);
        return treeLayout;
    }

    private HorizontalLayout buildFooterLayout() {
        Label footerLabel = new Label("2017");
        HorizontalLayout footerLayout = new HorizontalLayout(footerLabel);
        footerLayout.setSizeFull();
        footerLayout.setComponentAlignment(footerLabel, Alignment.BOTTOM_CENTER);
        return footerLayout;
    }

    private HorizontalLayout buildHeaderLayout() {
        Label headerLabel = new Label("TrackFind");
        HorizontalLayout headerLayout = new HorizontalLayout(headerLabel);
        headerLayout.setSizeFull();
        headerLayout.setComponentAlignment(headerLabel, Alignment.TOP_CENTER);
        return headerLayout;
    }

}
