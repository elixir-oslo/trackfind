package no.uio.ifi.trackfind.frontend;

import com.vaadin.data.HasValue;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.shared.ui.dnd.EffectAllowed;
import com.vaadin.ui.*;
import com.vaadin.ui.components.grid.TreeGridDragSource;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.backend.services.TrackFindService;
import no.uio.ifi.trackfind.frontend.components.KeyboardInterceptorExtension;
import no.uio.ifi.trackfind.frontend.components.TrackFindTree;
import no.uio.ifi.trackfind.frontend.data.TreeNode;
import no.uio.ifi.trackfind.frontend.listeners.TreeItemClickListener;
import no.uio.ifi.trackfind.frontend.listeners.TreeSelectionListener;
import no.uio.ifi.trackfind.frontend.providers.TrackDataProvider;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Main Vaadin UI of the application.
 * Capable of displaying metadata for repositories, constructing and executing search queries along with exporting the results.
 * Uses custom theme (VAADIN/themes/trackfind/trackfind.scss).
 * Uses custom WidgetSet (TrackFindWidgetSet.gwt.xml).
 *
 * @author Dmytro Titov
 */
@Slf4j
public abstract class AbstractUI extends UI {

    private TrackFindService trackFindService;

    private TabSheet tabSheet;

    @SuppressWarnings("unchecked")
    protected DataProvider getCurrentDataProvider() {
        TrackFindTree<TreeNode> tree = (TrackFindTree<TreeNode>) tabSheet.getSelectedTab();
        return tree.getTrackDataProvider();
    }

    protected VerticalLayout buildOuterLayout(HorizontalLayout headerLayout, HorizontalLayout mainLayout, HorizontalLayout footerLayout) {
        VerticalLayout outerLayout = new VerticalLayout(headerLayout, mainLayout, footerLayout);
        outerLayout.setSizeFull();
        outerLayout.setExpandRatio(headerLayout, 0.05f);
        outerLayout.setExpandRatio(mainLayout, 0.9f);
        outerLayout.setExpandRatio(footerLayout, 0.05f);
        return outerLayout;
    }

    @SuppressWarnings("unchecked")
    protected VerticalLayout buildTreeLayout() {
        tabSheet = new TabSheet();
        tabSheet.setSizeFull();

        for (DataProvider dataProvider : trackFindService.getDataProviders()) {
            TrackFindTree<TreeNode> tree = buildTree(dataProvider);
            tabSheet.addTab(tree, dataProvider.getName());
        }

        Panel treePanel = new Panel("Model browser", tabSheet);
        treePanel.setSizeFull();

        TextField attributesFilterTextField = new TextField("Filter attributes", (HasValue.ValueChangeListener<String>) event -> {
            TrackFindTree<TreeNode> tree = (TrackFindTree<TreeNode>) tabSheet.getSelectedTab();
            TrackDataProvider dataProvider = (TrackDataProvider) tree.getDataProvider();
            dataProvider.setAttributesFilter(event.getValue());
            dataProvider.refreshAll();
        });
        attributesFilterTextField.setValueChangeMode(ValueChangeMode.EAGER);
        attributesFilterTextField.setWidth(100, Sizeable.Unit.PERCENTAGE);

        TextField valuesFilterTextField = new TextField("Filter values", (HasValue.ValueChangeListener<String>) event -> {
            TrackFindTree<TreeNode> tree = (TrackFindTree<TreeNode>) tabSheet.getSelectedTab();
            TrackDataProvider dataProvider = (TrackDataProvider) tree.getDataProvider();
            dataProvider.setValuesFilter(event.getValue());
            dataProvider.refreshAll();
        });
        valuesFilterTextField.setValueChangeMode(ValueChangeMode.EAGER);
        valuesFilterTextField.setWidth(100, Sizeable.Unit.PERCENTAGE);

        VerticalLayout treeLayout = new VerticalLayout(treePanel, attributesFilterTextField, valuesFilterTextField);
        treeLayout.setSizeFull();
        treeLayout.setExpandRatio(treePanel, 1f);
        return treeLayout;
    }

    @SuppressWarnings("unchecked")
    protected TrackFindTree<TreeNode> buildTree(DataProvider dataProvider) {
        TrackFindTree<TreeNode> tree = new TrackFindTree<>(dataProvider);
        tree.setSelectionMode(Grid.SelectionMode.MULTI);
        tree.addItemClickListener(new TreeItemClickListener(tree));
        tree.addSelectionListener(new TreeSelectionListener(tree, new KeyboardInterceptorExtension(tree)));
        TreeGridDragSource<TreeNode> dragSource = new TreeGridDragSource<>((TreeGrid<TreeNode>) tree.getCompositionRoot());
        dragSource.setEffectAllowed(EffectAllowed.COPY);
        TrackDataProvider trackDataProvider = new TrackDataProvider(new TreeNode(dataProvider.getMetamodelTree(false)));
        tree.setDataProvider(trackDataProvider);
        tree.setSizeFull();
        tree.setStyleGenerator((StyleGenerator<TreeNode>) item -> item.isFinalAttribute() || item.isLeaf() ? null : "disabled-tree-node");
        return tree;
    }

    protected HorizontalLayout buildFooterLayout() {
        Label footerLabel = new Label("2017");
        HorizontalLayout footerLayout = new HorizontalLayout(footerLabel);
        footerLayout.setSizeFull();
        footerLayout.setComponentAlignment(footerLabel, Alignment.BOTTOM_CENTER);
        return footerLayout;
    }

    protected HorizontalLayout buildHeaderLayout() {
        Label headerLabel = new Label("TrackFind");
        HorizontalLayout headerLayout = new HorizontalLayout(headerLabel);
        headerLayout.setSizeFull();
        headerLayout.setComponentAlignment(headerLabel, Alignment.TOP_CENTER);
        return headerLayout;
    }

    @Autowired
    public void setTrackFindService(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

}
