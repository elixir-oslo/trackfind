package no.uio.ifi.trackfind.frontend;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.TreeNode;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import no.uio.ifi.trackfind.backend.pojo.TfScript;
import no.uio.ifi.trackfind.backend.repositories.HubRepository;
import no.uio.ifi.trackfind.backend.repositories.ScriptRepository;
import no.uio.ifi.trackfind.backend.services.MetamodelService;
import no.uio.ifi.trackfind.frontend.components.TrackFindTree;
import no.uio.ifi.trackfind.frontend.filters.TreeFilter;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;
import org.vaadin.aceeditor.AceTheme;
import org.vaadin.dialogs.ConfirmDialog;

import java.util.Collection;

/**
 * Mappings Vaadin UI of the application.
 * Uses custom theme (VAADIN/themes/trackfind/trackfind.scss).
 * Uses custom WidgetSet (TrackFindWidgetSet.gwt.xml).
 *
 * @author Dmytro Titov
 */
@SpringUI(path = "/mappings")
@Widgetset("TrackFindWidgetSet")
@Title("Mappings")
@Theme("trackfind")
@Slf4j
public class TrackFindMappingsUI extends AbstractUI {

    private MetamodelService metamodelService;
    private ScriptRepository scriptRepository;
    private HubRepository hubRepository;

    private AceEditor script;

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        HorizontalLayout headerLayout = buildHeaderLayout();
        VerticalLayout attributesMappingOuterLayout = buildAttributesMappingLayout();
        VerticalLayout treeLayout = buildTreeLayout();
        HorizontalLayout mainLayout = buildMainLayout(treeLayout, attributesMappingOuterLayout);
        HorizontalLayout footerLayout = buildFooterLayout();
        VerticalLayout outerLayout = buildOuterLayout(headerLayout, mainLayout, footerLayout);
        setContent(outerLayout);
        loadConfiguration();
    }

    protected VerticalLayout buildTreeLayout() {
        tabSheet = new TabSheet();
        tabSheet.setSizeFull();

        for (TfHub hub : trackFindService.getTrackHubs(true)) {
            TrackFindTree<TreeNode> tree = buildTree(hub);
            tabSheet.addTab(tree, hub.getName());
        }

        tabSheet.addSelectedTabChangeListener((TabSheet.SelectedTabChangeListener) event -> loadConfiguration());

        Panel treePanel = new Panel("Model browser", tabSheet);
        treePanel.setSizeFull();

//        TextField attributesFilterTextField = createFilter(true);

        VerticalLayout treeLayout = new VerticalLayout(treePanel);
        treeLayout.setSizeFull();
        treeLayout.setExpandRatio(treePanel, 1f);
        return treeLayout;
    }

    @SuppressWarnings("unchecked")
    protected TrackFindTree<TreeNode> buildTree(TfHub hub) {
        TrackFindTree<TreeNode> tree = new TrackFindTree<>(hub);
        tree.setDataProvider(trackFindDataProvider);
        tree.setSelectionMode(Grid.SelectionMode.SINGLE);
        tree.setSizeFull();
        tree.setStyleGenerator((StyleGenerator<TreeNode>) item -> item.isAttribute() ? null : "value-tree-node");

        TreeGrid<TreeNode> treeGrid = (TreeGrid<TreeNode>) tree.getCompositionRoot();
        treeGrid.setFilter(new TreeFilter(hub, "", ""));

        return tree;
    }

    private HorizontalLayout buildMainLayout(VerticalLayout treeLayout, VerticalLayout attributesMappingOuterLayout) {
        HorizontalLayout mainLayout = new HorizontalLayout(treeLayout, attributesMappingOuterLayout);
        mainLayout.setExpandRatio(treeLayout, 0.33f);
        mainLayout.setExpandRatio(attributesMappingOuterLayout, 0.66f);
        mainLayout.setSizeFull();
        return mainLayout;
    }

    private VerticalLayout buildAttributesMappingLayout() {
        script = new AceEditor();
        script.setSizeFull();
        script.setTheme(AceTheme.github);
        script.setMode(AceMode.coffee);

        TabSheet mappingsTabSheet = new TabSheet();
        mappingsTabSheet.setSizeFull();
        mappingsTabSheet.addTab(script, "Script 1");

        mappingsTabSheet.addSelectedTabChangeListener((TabSheet.SelectedTabChangeListener) event -> {
            if (event.getTabSheet().getSelectedTab().equals(script)) {
                script.focus();
            }
        });

        Panel attributesMappingPanel = new Panel("Scripts", mappingsTabSheet);
        attributesMappingPanel.setSizeFull();
        Button saveButton = new Button("Save");
        saveButton.setSizeFull();
        saveButton.addClickListener((Button.ClickListener) event -> saveConfiguration());
        Button applyMappingsButton = new Button("Apply mappings");
        applyMappingsButton.setSizeFull();
        applyMappingsButton.addClickListener((Button.ClickListener) event -> ConfirmDialog.show(getUI(),
                "Are you sure? " +
                        "Applying attribute scripts is time-consuming process and will lead to changing the data in the database.",
                (ConfirmDialog.Listener) dialog -> {
                    if (dialog.isConfirmed()) {
                        TfHub currentHub = getCurrentHub();
                        DataProvider dataProvider = trackFindService.getDataProvider(currentHub.getRepository());
                        dataProvider.applyMappings(currentHub.getName());
                    }
                }));

        HorizontalLayout buttonsLayout = new HorizontalLayout(saveButton, applyMappingsButton);
        buttonsLayout.setWidth(100, Unit.PERCENTAGE);
        buttonsLayout.setEnabled(!properties.isDemoMode());
        VerticalLayout attributesMappingOuterLayout = new VerticalLayout(attributesMappingPanel, buttonsLayout);
        attributesMappingOuterLayout.setSizeFull();
        attributesMappingOuterLayout.setExpandRatio(attributesMappingPanel, 1f);
        return attributesMappingOuterLayout;
    }

    private void loadConfiguration() {
        TfHub currentHub = getCurrentHub();
        Collection<TfScript> scripts = metamodelService.getScripts(currentHub.getRepository(), currentHub.getName());
        if (CollectionUtils.isNotEmpty(scripts)) {
            script.setValue(scripts.iterator().next().getScript());
        }
    }

    private void saveConfiguration() {
        TfHub currentHub = getCurrentHub();
        Collection<TfScript> scripts = metamodelService.getScripts(currentHub.getRepository(), currentHub.getName());
        scriptRepository.deleteInBatch(scripts);
        script.getOptionalValue().ifPresent(s -> {
            TfScript script = new TfScript();
            script.setIndex(0L);
            script.setVersion(currentHub.getCurrentVersion().orElseThrow(RuntimeException::new));
            script.setScript(s);
            scriptRepository.save(script);
        });
        hubRepository.save(currentHub);
        Notification.show("Scripts saved. Press \"Apply\" for changes to take effect.");
    }

    @Autowired
    public void setMetamodelService(MetamodelService metamodelService) {
        this.metamodelService = metamodelService;
    }

    @Autowired
    public void setScriptRepository(ScriptRepository scriptRepository) {
        this.scriptRepository = scriptRepository;
    }

    @Autowired
    public void setHubRepository(HubRepository hubRepository) {
        this.hubRepository = hubRepository;
    }

}
