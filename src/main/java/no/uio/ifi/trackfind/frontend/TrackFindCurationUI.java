package no.uio.ifi.trackfind.frontend;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.HasValue;
import com.vaadin.data.provider.Query;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import com.vaadin.ui.renderers.TextRenderer;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.TreeNode;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import no.uio.ifi.trackfind.backend.pojo.TfMapping;
import no.uio.ifi.trackfind.backend.pojo.TfObjectType;
import no.uio.ifi.trackfind.backend.pojo.TfVersion;
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
 * Curation Vaadin UI of the application.
 * Uses custom theme (VAADIN/themes/trackfind/trackfind.scss).
 * Uses custom WidgetSet (TrackFindWidgetSet.gwt.xml).
 *
 * @author Dmytro Titov
 */
@SpringUI(path = "/curation")
@Widgetset("TrackFindWidgetSet")
@Title("Curation")
@Theme("trackfind")
@Slf4j
public class TrackFindCurationUI extends AbstractUI {

    private MetamodelService metamodelService;

    private Button moveMappingUpButton = new Button("Move up ↑");
    private Button moveMappingDownButton = new Button("Move down ↓");
    private Button deleteMappingButton = new Button("Delete ✕");
    private Button addStaticMappingButton = new Button("Add static mapping");
    private Button addDynamicMappingButton = new Button("Add dynamic mapping");
    private Button saveButton = new Button("Save");
    private ComboBox<String> attributesComboBox = new ComboBox<>();
    private ComboBox<String> categoriesComboBox = new ComboBox<>();

    private Grid<TfMapping> grid = new Grid<>(TfMapping.class);
    private AceEditor script = new AceEditor();
    private Panel scriptsPanel = new Panel("Script", script);

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
        loadMappings();
    }

    protected VerticalLayout buildTreeLayout() {
        tabSheet = new TabSheet();
        tabSheet.setSizeFull();

        for (TfHub hub : trackFindService.getTrackHubs(true)) {
            TrackFindTree<TreeNode> tree = buildTree(hub);
            tree.addSelectionListener((SelectionListener<TreeNode>) event -> {
                Optional<String> value = attributesComboBox.getSelectedItem();
                Optional<TreeNode> item = event.getFirstSelectedItem();
                addStaticMappingButton.setEnabled(value.isPresent()
                        && item.isPresent()
                        && item.get().isAttribute()
                        && item.get().getLevel() != 0
                );
            });
            tabSheet.addTab(tree, hub.getName());
        }

        tabSheet.addSelectedTabChangeListener((TabSheet.SelectedTabChangeListener) event -> loadMappings());

        Panel treePanel = new Panel("Model browser", tabSheet);
        treePanel.setSizeFull();

        addStaticMappingButton.setEnabled(false);
        addStaticMappingButton.setWidth("100%");
        addStaticMappingButton.addClickListener((Button.ClickListener) event -> {
            TfHub currentHub = getCurrentHub();
            TfVersion currentVersion = currentHub.getCurrentVersion().orElseThrow(RuntimeException::new);
            TreeNode treeNode = getCurrentTree().getSelectedItems().iterator().next();
            String fromObjectTypeName = treeNode.getCategory();
            String toObjectTypeName = categoriesComboBox.getSelectedItem().orElseThrow(RuntimeException::new);
            Collection<TfObjectType> objectTypes = metamodelService.getObjectTypes(currentHub.getRepository(), currentHub.getName());
            TfObjectType toObjectType = objectTypes.stream().filter(ot -> ot.getName().equalsIgnoreCase(toObjectTypeName)).findAny().orElseThrow(RuntimeException::new);
            TfObjectType fromObjectType = objectTypes.stream().filter(ot -> ot.getName().equalsIgnoreCase(fromObjectTypeName)).findAny().orElseThrow(RuntimeException::new);
            TfMapping mapping = metamodelService.addMapping(new TfMapping(null,
                    null,
                    currentVersion,
                    fromObjectType,
                    treeNode.getSQLPath().replace(fromObjectTypeName + ".content" + separator, ""),
                    toObjectType,
                    attributesComboBox.getSelectedItem().orElseThrow(RuntimeException::new),
                    null)
            );
            saveMapping(mapping);
        });

        attributesComboBox = new ComboBox<>();
        attributesComboBox.setEnabled(false);
        attributesComboBox.setWidth("100%");
        attributesComboBox.addValueChangeListener((HasValue.ValueChangeListener<String>) event -> {
            String value = event.getValue();
            TrackFindTree<TreeNode> currentTree = getCurrentTree();
            if (StringUtils.isNotEmpty(value) && CollectionUtils.isNotEmpty(currentTree.getSelectedItems())) {
                addStaticMappingButton.setEnabled(true);
            } else {
                addStaticMappingButton.setEnabled(false);
            }
        });

        categoriesComboBox = new ComboBox<>();
        categoriesComboBox.setWidth("100%");
        categoriesComboBox.setItems(schemaService.getAttributes().keySet());
        categoriesComboBox.addValueChangeListener((HasValue.ValueChangeListener<String>) event -> {
            String value = event.getValue();
            attributesComboBox.clear();
            addStaticMappingButton.setEnabled(false);
            if (StringUtils.isEmpty(value)) {
                attributesComboBox.setEnabled(false);
            } else {
                attributesComboBox.setEnabled(true);
                attributesComboBox.setItems(schemaService.getAttributes().get(value));
            }
        });

        addDynamicMappingButton.setWidth("100%");
        addDynamicMappingButton.addClickListener((Button.ClickListener) event -> {
            TfHub currentHub = getCurrentHub();
            TfMapping mapping = new TfMapping();
            mapping.setVersion(currentHub.getCurrentVersion().orElseThrow(RuntimeException::new));
            saveMapping(mapping);
        });

        VerticalLayout treeLayout = new VerticalLayout(treePanel, categoriesComboBox, attributesComboBox, addStaticMappingButton, addDynamicMappingButton);
        treeLayout.setSizeFull();
        treeLayout.setExpandRatio(treePanel, 1f);
        return treeLayout;
    }

    private void saveMapping(TfMapping mapping) {
        TfHub currentHub = getCurrentHub();
        mapping = metamodelService.addMapping(mapping);
        grid.setItems(metamodelService.getMappings(currentHub.getRepository(), currentHub.getName()));
        grid.recalculateColumnWidths();
        grid.select(mapping);
    }

    @SuppressWarnings("unchecked")
    private TrackFindTree<TreeNode> buildTree(TfHub hub) {
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

    private VerticalLayout buildMappingsLayout() {
        TfHub currentHub = getCurrentHub();
        grid.setSizeFull();
        grid.addColumn(TfMapping::getOrderNumber).setCaption("Order").setId("0");
        grid.addColumn(m -> m.getFromObjectType() == null ? null : m.getFromObjectType().getName()).setCaption("From category").setId("1").setRenderer(new TextRenderer("Scripted"));
        grid.addColumn(TfMapping::getFromAttribute).setCaption("From attribute").setId("2").setRenderer(new TextRenderer("Scripted"));
        grid.addColumn(m -> m.getToObjectType() == null ? null : m.getToObjectType().getName()).setCaption("To category").setId("3").setRenderer(new TextRenderer("Scripted"));
        grid.addColumn(TfMapping::getToAttribute).setCaption("To attribute").setId("4").setRenderer(new TextRenderer("Scripted"));
        grid.removeColumn("orderNumber");
        grid.removeColumn("version");
        grid.removeColumn("fromObjectType");
        grid.removeColumn("fromAttribute");
        grid.removeColumn("toObjectType");
        grid.removeColumn("toAttribute");
        grid.removeColumn("script");
        grid.removeColumn("id");
        grid.setColumnOrder("0", "1", "2", "3", "4");
        grid.sort("0", SortDirection.ASCENDING);
        grid.setItems(metamodelService.getMappings(currentHub.getRepository(), currentHub.getName()));
        grid.recalculateColumnWidths();
        grid.addSelectionListener((SelectionListener<TfMapping>) event -> {
            moveMappingUpButton.setEnabled(event.getFirstSelectedItem().isPresent());
            moveMappingDownButton.setEnabled(event.getFirstSelectedItem().isPresent());
            deleteMappingButton.setEnabled(event.getFirstSelectedItem().isPresent());
            boolean isScript = event.getFirstSelectedItem().isPresent() && StringUtils.isEmpty(event.getFirstSelectedItem().get().getFromAttribute());
            script.setVisible(isScript);
            saveButton.setEnabled(isScript);
            if (isScript) {
                script.setValue(event.getFirstSelectedItem().get().getScript());
            }
        });

        moveMappingUpButton.setEnabled(false);
        moveMappingUpButton.setSizeFull();
        moveMappingUpButton.addClickListener((Button.ClickListener) event -> moveSelectedMapping(true));
        moveMappingDownButton.setEnabled(false);
        moveMappingDownButton.setSizeFull();
        moveMappingDownButton.addClickListener((Button.ClickListener) event -> moveSelectedMapping(false));
        deleteMappingButton.setEnabled(false);
        deleteMappingButton.setSizeFull();
        deleteMappingButton.addClickListener((Button.ClickListener) event -> deleteSelectedMapping());
        HorizontalLayout gridButtonsLayout = new HorizontalLayout(moveMappingUpButton, moveMappingDownButton, deleteMappingButton);
        gridButtonsLayout.setSizeFull();
        gridButtonsLayout.setExpandRatio(moveMappingUpButton, 0.33f);
        gridButtonsLayout.setExpandRatio(moveMappingDownButton, 0.33f);
        gridButtonsLayout.setExpandRatio(deleteMappingButton, 0.33f);

        Panel mappingsPanel = new Panel("Mappings and Scripts", grid);
        mappingsPanel.setSizeFull();

        VerticalLayout mappingsLayout = new VerticalLayout(mappingsPanel, gridButtonsLayout);
        mappingsLayout.setSizeFull();
        mappingsLayout.setExpandRatio(mappingsPanel, 0.95f);
        mappingsLayout.setExpandRatio(gridButtonsLayout, 0.05f);
        mappingsLayout.setMargin(new MarginInfo(true, false, true, false));
        return mappingsLayout;
    }

    private void moveSelectedMapping(boolean up) {
        TfHub currentHub = getCurrentHub();
        TfMapping mapping = getSelectedMapping().orElseThrow(RuntimeException::new);
        metamodelService.moveMapping(mapping, up);
        grid.setItems(metamodelService.getMappings(currentHub.getRepository(), currentHub.getName()));
        grid.recalculateColumnWidths();
        grid.select(mapping);
    }

    private void deleteSelectedMapping() {
        TfHub currentHub = getCurrentHub();
        metamodelService.deleteMapping(getSelectedMapping().orElseThrow(RuntimeException::new));
        grid.setItems(metamodelService.getMappings(currentHub.getRepository(), currentHub.getName()));
        grid.recalculateColumnWidths();
    }

    private Optional<TfMapping> getSelectedMapping() {
        return CollectionUtils.isEmpty(grid.getSelectedItems()) ? Optional.empty() : Optional.ofNullable(grid.getSelectedItems().iterator().next());
    }

    private VerticalLayout buildScriptsLayout() {
        script.setSizeFull();
        script.setTheme(AceTheme.github);
        script.setMode(AceMode.coffee);
        script.setVisible(false);

        scriptsPanel.setSizeFull();
        saveButton.setEnabled(false);
        saveButton.setSizeFull();
        saveButton.addClickListener((Button.ClickListener) event -> saveScript());
        Button applyMappingsButton = new Button("Apply mappings");
        applyMappingsButton.setSizeFull();
        applyMappingsButton.addClickListener((Button.ClickListener) event -> {
            TfHub currentHub = getCurrentHub();
            if (grid.getDataProvider().fetch(new Query<>()).count() == 0) {
                Notification.show("You should have at least one mapping in order to proceed.", Notification.Type.WARNING_MESSAGE);
                return;
            }
            ConfirmDialog.show(getUI(),
                    "Are you sure? " +
                            "Data curation is time-consuming process and will lead to changing the data in the database.",
                    (ConfirmDialog.Listener) dialog -> {
                        if (dialog.isConfirmed()) {
                            DataProvider dataProvider = trackFindService.getDataProvider(currentHub.getRepository());
                            dataProvider.runCuration(currentHub.getName());
                        }
                    });
        });

        HorizontalLayout buttonsLayout = new HorizontalLayout(saveButton, applyMappingsButton);
        buttonsLayout.setWidth(100, Unit.PERCENTAGE);
        VerticalLayout scriptsLayout = new VerticalLayout(scriptsPanel, buttonsLayout);
        scriptsLayout.setSizeFull();
        scriptsLayout.setExpandRatio(scriptsPanel, 1f);
        return scriptsLayout;
    }

    private void loadMappings() {
        TfHub currentHub = getCurrentHub();
        grid.setItems(metamodelService.getMappings(currentHub.getRepository(), currentHub.getName()));
    }

    private void saveScript() {
        Optional<TfMapping> selectedMapping = getSelectedMapping();
        selectedMapping.ifPresent(mapping -> {
            mapping.setScript(script.getValue());
            saveMapping(mapping);
        });
    }

    @Autowired
    public void setMetamodelService(MetamodelService metamodelService) {
        this.metamodelService = metamodelService;
    }

}
