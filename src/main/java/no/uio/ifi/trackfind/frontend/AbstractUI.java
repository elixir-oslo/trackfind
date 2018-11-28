package no.uio.ifi.trackfind.frontend;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.HierarchicalDataProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.ui.*;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import no.uio.ifi.trackfind.backend.data.TreeNode;
import no.uio.ifi.trackfind.backend.data.providers.AbstractDataProvider;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.backend.services.SchemaService;
import no.uio.ifi.trackfind.backend.services.TrackFindService;
import no.uio.ifi.trackfind.frontend.components.TrackFindTree;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

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

    protected SchemaService schemaService;
    protected TrackFindProperties properties;
    protected TrackFindService trackFindService;

    protected TabSheet tabSheet;

    protected Collection<AbstractDataProvider> dataProviders = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public TrackFindTree<TreeNode> getCurrentTree() {
        return (TrackFindTree<TreeNode>) tabSheet.getSelectedTab();
    }

    @SuppressWarnings("unchecked")
    protected AbstractDataProvider getCurrentDataProvider() {
        return (AbstractDataProvider) getCurrentTree().getDataProvider();
    }

    protected VerticalLayout buildOuterLayout(HorizontalLayout headerLayout, HorizontalLayout mainLayout, HorizontalLayout footerLayout) {
        VerticalLayout outerLayout = new VerticalLayout(headerLayout, mainLayout, footerLayout);
        outerLayout.setSizeFull();
        outerLayout.setExpandRatio(headerLayout, 0.05f);
        outerLayout.setExpandRatio(mainLayout, 0.9f);
        outerLayout.setExpandRatio(footerLayout, 0.05f);
        return outerLayout;
    }

    protected HorizontalLayout buildFooterLayout() {
        String implementationVersion = getClass().getPackage().getImplementationVersion();
        implementationVersion = implementationVersion == null ? "dev" : implementationVersion;
        Label footerLabel = new Label("Version: " + implementationVersion);
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

    @SuppressWarnings("unchecked")
    protected void refreshTrees(boolean raw) {
//        for (AbstractDataProvider dataProvider : dataProviders) {
//            TreeDataProvider treeDataProvider = entry.getKey();
//            TreeData treeData = treeDataProvider.getTreeData().clear();
//            TreeNode root = new TreeNode(dataProvider.getMetamodelTree(raw), properties.getLevelsSeparator());
//            Collection<TreeNode> children = root.getChildren().parallelStream().collect(Collectors.toSet());
//            treeData.addRootItems(children);
//            children.forEach(c -> fillTreeData(treeData, c));
//            treeDataProvider.refreshAll();
//        }
    }

    @Autowired
    public void setSchemaService(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @Autowired
    public void setProperties(TrackFindProperties properties) {
        this.properties = properties;
    }

    @Autowired
    public void setTrackFindService(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

}
