package no.uio.ifi.trackfind.frontend;

import com.vaadin.data.HasValue;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.*;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import no.uio.ifi.trackfind.backend.data.TreeNode;
import no.uio.ifi.trackfind.backend.services.SchemaService;
import no.uio.ifi.trackfind.backend.services.TrackFindService;
import no.uio.ifi.trackfind.frontend.components.TrackFindTree;
import no.uio.ifi.trackfind.frontend.filters.TreeFilter;
import no.uio.ifi.trackfind.frontend.providers.TrackFindDataProvider;
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

    protected SchemaService schemaService;
    protected TrackFindProperties properties;
    protected TrackFindService trackFindService;
    protected TrackFindDataProvider trackFindDataProvider;

    protected TabSheet tabSheet;

    @SuppressWarnings("unchecked")
    public TrackFindTree<TreeNode> getCurrentTree() {
        return (TrackFindTree<TreeNode>) tabSheet.getSelectedTab();
    }

    @SuppressWarnings("unchecked")
    protected TfHub getCurrentHub() {
        return getCurrentTree().getHub();
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
    protected TextField createFilter(boolean attributes) {
        TextField attributesFilterTextField = new TextField(attributes ? "Filter attributes" : "Filter values", (HasValue.ValueChangeListener<String>) event -> {
            TrackFindTree<TreeNode> currentTree = getCurrentTree();
            TreeFilter filter = (TreeFilter) ((TreeGrid<TreeNode>) currentTree.getCompositionRoot()).getFilter();
            if (attributes) {
                filter.setAttributesFilter(event.getValue());
            } else {
                filter.setValuesFilter(event.getValue());
            }
            currentTree.getDataProvider().refreshAll();
        });
        attributesFilterTextField.setValueChangeMode(ValueChangeMode.EAGER);
        attributesFilterTextField.setWidth(100, Unit.PERCENTAGE);
        return attributesFilterTextField;
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

    @Autowired
    public void setTrackFindDataProvider(TrackFindDataProvider trackFindDataProvider) {
        this.trackFindDataProvider = trackFindDataProvider;
    }

}
