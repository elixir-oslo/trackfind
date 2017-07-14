package no.uio.ifi.trackfind.frontend;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vaadin.annotations.Theme;
import com.vaadin.data.HasValue;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutListener;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.MouseEventDetails;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import no.uio.ifi.trackfind.backend.services.TrackFindService;
import no.uio.ifi.trackfind.frontend.data.TreeNode;
import no.uio.ifi.trackfind.frontend.providers.TrackDataProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

@SpringUI
@Theme("trackfind")
public class TrackFindUI extends UI {

    private final TrackFindService trackFindService;
    private final Gson gson;

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
        TextArea queryTextArea = new TextArea();
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

    private VerticalLayout buildTreeLayout() {
        Tree<TreeNode> tree = new Tree<>();
        tree.setSelectionMode(Grid.SelectionMode.MULTI);
        tree.addItemClickListener((Tree.ItemClickListener<TreeNode>) event -> {
            MouseEventDetails mouseEventDetails = event.getMouseEventDetails();
            Tree<TreeNode> source = event.getSource();
            Set<TreeNode> selectedItems = source.getSelectedItems();
            TreeNode last;
            try {
                last = Iterables.getLast(selectedItems);
            } catch (NoSuchElementException e) {
                return;
            }
            TreeNode current = event.getItem();

            if (mouseEventDetails.isCtrlKey() || mouseEventDetails.isMetaKey()) {
                selectedItems.stream().filter(tn -> tn.getLevel() != current.getLevel() || tn.getParent() != current.getParent()).forEach(tree::deselect);
            } else if (mouseEventDetails.isShiftKey()) {
                selectedItems.stream().filter(tn -> tn.getLevel() != current.getLevel() || tn.getParent() != current.getParent()).forEach(tree::deselect);
                selectedItems = source.getSelectedItems();
                if (selectedItems.contains(last)) {
                    TreeNode parent = current.getParent();
                    List<TreeNode> children = parent.fetchChildren().sorted().collect(Collectors.toList());
                    int indexOfLast = children.indexOf(last);
                    int indexOfCurrent = children.indexOf(current);
                    TreeNode from;
                    TreeNode to;
                    if (indexOfCurrent > indexOfLast) {
                        from = last;
                        to = current;
                    } else {
                        from = current;
                        to = last;
                    }
                    boolean started = false;
                    boolean finished = false;
                    for (TreeNode child : children) {
                        if (from.equals(child)) {
                            started = true;
                        } else if (to.equals(child)) {
                            finished = true;
                        }
                        if (started && !finished) {
                            tree.select(child);
                        }
                    }
                }
            } else {
                selectedItems.forEach(tree::deselect);
            }
        });
        tree.addSelectionListener((SelectionListener<TreeNode>) event -> {
            if (event.isUserOriginated()) {
                TreeNode last = Iterables.getLast(event.getAllSelectedItems());
                if (!(last.isFinalAttribute() || last.isLeaf())) {
                    tree.deselect(last);
                }
            }
        });
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
