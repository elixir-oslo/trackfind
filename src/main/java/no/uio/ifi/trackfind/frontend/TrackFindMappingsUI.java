package no.uio.ifi.trackfind.frontend;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.HasValue;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import com.vaadin.ui.renderers.ButtonRenderer;
import com.vaadin.ui.renderers.ClickableRenderer;
import elemental.json.JsonValue;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.TreeNode;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import no.uio.ifi.trackfind.backend.pojo.TfMapping;
import no.uio.ifi.trackfind.backend.pojo.TfScript;
import no.uio.ifi.trackfind.backend.repositories.HubRepository;
import no.uio.ifi.trackfind.backend.repositories.ScriptRepository;
import no.uio.ifi.trackfind.backend.services.MetamodelService;
import no.uio.ifi.trackfind.frontend.components.TrackFindTree;
import no.uio.ifi.trackfind.frontend.filters.TreeFilter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;
import org.vaadin.aceeditor.AceTheme;
import org.vaadin.dialogs.ConfirmDialog;

import java.util.Collection;
import java.util.Optional;

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

    private Button addMappingButton = new Button("Add mapping");
    private ComboBox<String> attributesComboBox = new ComboBox<>();
    private ComboBox<String> categoriesComboBox = new ComboBox<>();

    private Grid<TfMapping> grid = new Grid<>(TfMapping.class);
    private AceEditor script = new AceEditor();

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        HorizontalLayout headerLayout = buildHeaderLayout();
        VerticalLayout treeLayout = buildTreeLayout();
        VerticalLayout mappingsLayout = buildMappingsLayout();
        VerticalLayout scriptsLayout = buildScriptsLayout();
        HorizontalLayout mainLayout = buildMainLayout(treeLayout, mappingsLayout, scriptsLayout);
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
            tree.addSelectionListener((SelectionListener<TreeNode>) event -> {
                Optional<String> value = attributesComboBox.getSelectedItem();
                Optional<TreeNode> item = event.getFirstSelectedItem();
                if (value.isPresent() && item.isPresent()) {
                    addMappingButton.setEnabled(true);
                } else {
                    addMappingButton.setEnabled(false);
                }
            });
            tabSheet.addTab(tree, hub.getName());
        }

        tabSheet.addSelectedTabChangeListener((TabSheet.SelectedTabChangeListener) event -> loadConfiguration());

        Panel treePanel = new Panel("Model browser", tabSheet);
        treePanel.setSizeFull();

//        TextField attributesFilterTextField = createFilter(true);

        addMappingButton = new Button("Add mapping");
        addMappingButton.setEnabled(false);
        addMappingButton.setWidth("100%");

        attributesComboBox = new ComboBox<>();
        attributesComboBox.setEnabled(false);
        attributesComboBox.setWidth("100%");
        attributesComboBox.addValueChangeListener((HasValue.ValueChangeListener<String>) event -> {
            String value = event.getValue();
            TrackFindTree<TreeNode> currentTree = getCurrentTree();
            if (StringUtils.isNotEmpty(value) && CollectionUtils.isNotEmpty(currentTree.getSelectedItems())) {
                addMappingButton.setEnabled(true);
            } else {
                addMappingButton.setEnabled(false);
            }
        });

        categoriesComboBox = new ComboBox<>();
        categoriesComboBox.setWidth("100%");
        categoriesComboBox.setItems(schemaService.getAttributes().keySet());
        categoriesComboBox.addValueChangeListener((HasValue.ValueChangeListener<String>) event -> {
            String value = event.getValue();
            attributesComboBox.clear();
            addMappingButton.setEnabled(false);
            if (StringUtils.isEmpty(value)) {
                attributesComboBox.setEnabled(false);
            } else {
                attributesComboBox.setEnabled(true);
                attributesComboBox.setItems(schemaService.getAttributes().get(value));
            }
        });

        VerticalLayout treeLayout = new VerticalLayout(treePanel, categoriesComboBox, attributesComboBox, addMappingButton);
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

    private HorizontalLayout buildMainLayout(VerticalLayout treeLayout, VerticalLayout mappingsLayout, VerticalLayout scriptsLayout) {
        HorizontalLayout mainLayout = new HorizontalLayout(treeLayout, mappingsLayout, scriptsLayout);
        mainLayout.setExpandRatio(treeLayout, 0.33f);
        mainLayout.setExpandRatio(mappingsLayout, 0.33f);
        mainLayout.setExpandRatio(scriptsLayout, 0.33f);
        mainLayout.setSizeFull();
        return mainLayout;
    }

    @SuppressWarnings("unchecked")
    private VerticalLayout buildMappingsLayout() {
        TfHub hub = getCurrentHub();
        grid.setSizeFull();
        grid.addColumn(TfMapping::getFromAttribute).setCaption("From attribute").setId("0");
        grid.addColumn(m -> m.getToObjectType().getName()).setCaption("To category").setId("1");
        grid.addColumn(TfMapping::getToAttribute).setCaption("To attribute").setId("2");
        grid.removeColumn("fromAttribute");
        grid.removeColumn("toObjectType");
        grid.removeColumn("toAttribute");
        ButtonRenderer buttonRenderer = new ButtonRenderer((ClickableRenderer.RendererClickListener<TfMapping>) event -> {
            metamodelService.deleteMapping(event.getItem());
            grid.setItems(metamodelService.getMappings(hub.getRepository(), hub.getName()));
        }) {
            @Override
            public JsonValue encode(Object value) {
                return super.encode("Delete");
            }
        };
        grid.getColumn("id").setRenderer(buttonRenderer).setCaption("Action");
        grid.setColumnOrder("0", "1", "2", "id");
        grid.setItems(metamodelService.getMappings(hub.getRepository(), hub.getName()));

        Panel mappingsPanel = new Panel("Mappings", grid);
        mappingsPanel.setSizeFull();

        VerticalLayout mappingsLayout = new VerticalLayout(mappingsPanel);
        mappingsLayout.setSizeFull();
        mappingsLayout.setExpandRatio(mappingsPanel, 1f);
        mappingsLayout.setMargin(new MarginInfo(true, false, true, false));
        return mappingsLayout;
    }

    private VerticalLayout buildScriptsLayout() {
        script.setSizeFull();
        script.setTheme(AceTheme.github);
        script.setMode(AceMode.coffee);

        TabSheet scriptsTabSheet = new TabSheet();
        scriptsTabSheet.setSizeFull();
        scriptsTabSheet.addTab(script, "Script 1");

        scriptsTabSheet.addSelectedTabChangeListener((TabSheet.SelectedTabChangeListener) event -> {
            if (event.getTabSheet().getSelectedTab().equals(script)) {
                script.focus();
            }
        });

        Panel scriptsPanel = new Panel("Scripts", scriptsTabSheet);
        scriptsPanel.setSizeFull();
        Button saveButton = new Button("Save");
        saveButton.setSizeFull();
        saveButton.addClickListener((Button.ClickListener) event -> saveConfiguration());
        Button applyMappingsButton = new Button("Apply mappings");
        applyMappingsButton.setSizeFull();
        applyMappingsButton.addClickListener((Button.ClickListener) event -> {
            TfHub currentHub = getCurrentHub();
            Collection<TfScript> scripts = metamodelService.getScripts(currentHub.getRepository(), currentHub.getName());
            if (!grid.iterator().hasNext() || CollectionUtils.isEmpty(scripts)) {
                Notification.show("You should have either mappings or scripts in order to proceed.", Notification.Type.WARNING_MESSAGE);
                return;
            }
            ConfirmDialog.show(getUI(),
                    "Are you sure? " +
                            "Applying attribute scripts is time-consuming process and will lead to changing the data in the database.",
                    (ConfirmDialog.Listener) dialog -> {
                        if (dialog.isConfirmed()) {
                            DataProvider dataProvider = trackFindService.getDataProvider(currentHub.getRepository());
                            dataProvider.applyMappings(currentHub.getName());
                        }
                    });
        });

        HorizontalLayout buttonsLayout = new HorizontalLayout(saveButton, applyMappingsButton);
        buttonsLayout.setWidth(100, Unit.PERCENTAGE);
        buttonsLayout.setEnabled(!properties.isDemoMode());
        VerticalLayout scriptsLayout = new VerticalLayout(scriptsPanel, buttonsLayout);
        scriptsLayout.setSizeFull();
        scriptsLayout.setExpandRatio(scriptsPanel, 1f);
        return scriptsLayout;
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
