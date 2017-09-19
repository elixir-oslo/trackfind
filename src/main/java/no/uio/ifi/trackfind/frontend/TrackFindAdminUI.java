package no.uio.ifi.trackfind.frontend;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.HasValue;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.server.Sizeable;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.shared.ui.dnd.DropEffect;
import com.vaadin.shared.ui.dnd.EffectAllowed;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import com.vaadin.ui.components.grid.TreeGridDragSource;
import com.vaadin.ui.dnd.DropTargetExtension;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.frontend.components.TrackFindTree;
import no.uio.ifi.trackfind.frontend.data.TreeNode;
import no.uio.ifi.trackfind.frontend.listeners.TextFieldDropListener;
import no.uio.ifi.trackfind.frontend.providers.TrackDataProvider;
import no.uio.ifi.trackfind.frontend.providers.impl.AdminTrackDataProvider;
import org.apache.commons.io.FileUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Admin Vaadin UI of the application.
 * Uses custom theme (VAADIN/themes/trackfind/trackfind.scss).
 * Uses custom WidgetSet (TrackFindWidgetSet.gwt.xml).
 *
 * @author Dmytro Titov
 */
@SpringUI(path = "/admin")
@Widgetset("TrackFindWidgetSet")
@Title("Dashboard")
@Theme("trackfind")
@Slf4j
public class TrackFindAdminUI extends AbstractUI {

    private Button addMappingButton;
    private VerticalLayout attributesMappingLayout;

    private Map<TextField, ComboBox<String>> attributesMapping = new HashMap<>();
    private CheckBox publishedCheckBox;

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        HorizontalLayout headerLayout = buildHeaderLayout();
        VerticalLayout treeLayout = buildTreeLayout();
        VerticalLayout attributesMappingOuterLayout = buildAttributesMappingLayout();
        HorizontalLayout mainLayout = buildMainLayout(treeLayout, attributesMappingOuterLayout);
        HorizontalLayout footerLayout = buildFooterLayout();
        VerticalLayout outerLayout = buildOuterLayout(headerLayout, mainLayout, footerLayout);
        setContent(outerLayout);
        loadConfiguration();
    }

    protected VerticalLayout buildTreeLayout() {
        tabSheet = new TabSheet();
        tabSheet.setSizeFull();

        for (DataProvider dataProvider : trackFindService.getDataProviders(false)) {
            TrackFindTree<TreeNode> tree = buildTree(dataProvider);
            tabSheet.addTab(tree, dataProvider.getName());
        }

        tabSheet.addSelectedTabChangeListener((TabSheet.SelectedTabChangeListener) event -> loadConfiguration());

        Panel treePanel = new Panel("Model browser", tabSheet);
        treePanel.setSizeFull();

        TextField attributesFilterTextField = new TextField("Filter attributes", (HasValue.ValueChangeListener<String>) event -> {
            TrackDataProvider dataProvider = getCurrentTrackDataProvider();
            dataProvider.setAttributesFilter(event.getValue());
            dataProvider.refreshAll();
        });
        attributesFilterTextField.setValueChangeMode(ValueChangeMode.EAGER);
        attributesFilterTextField.setWidth(100, Sizeable.Unit.PERCENTAGE);

        addMappingButton = new Button("Add mapping");
        addMappingButton.setWidth(100, Unit.PERCENTAGE);
        addMappingButton.setEnabled(!CollectionUtils.isEmpty(getCurrentTree().getSelectedItems()));
        addMappingButton.addClickListener((Button.ClickListener) event -> {
            Set<TreeNode> selectedItems = getCurrentTree().getSelectedItems();
            String sourceAttribute = CollectionUtils.isEmpty(selectedItems) ? "" : selectedItems.iterator().next().getPath();
            addMappingPair(sourceAttribute, "");
            publishedCheckBox.setEnabled(isEverythingFilled());
        });

        VerticalLayout treeLayout = new VerticalLayout(treePanel, attributesFilterTextField, addMappingButton);
        treeLayout.setSizeFull();
        treeLayout.setExpandRatio(treePanel, 1f);
        return treeLayout;
    }

    @SuppressWarnings("unchecked")
    protected TrackFindTree<TreeNode> buildTree(DataProvider dataProvider) {
        TrackFindTree<TreeNode> tree = new TrackFindTree<>(dataProvider);
        tree.setSelectionMode(Grid.SelectionMode.SINGLE);
        tree.addSelectionListener((SelectionListener<TreeNode>) event -> addMappingButton.setEnabled(!CollectionUtils.isEmpty(event.getAllSelectedItems())));
        TreeGridDragSource<TreeNode> dragSource = new TreeGridDragSource<>((TreeGrid<TreeNode>) tree.getCompositionRoot());
        dragSource.setEffectAllowed(EffectAllowed.COPY);
        AdminTrackDataProvider trackDataProvider = new AdminTrackDataProvider(new TreeNode(dataProvider.getMetamodelTree()));
        tree.setDataProvider(trackDataProvider);
        tree.setSizeFull();
        tree.setStyleGenerator((StyleGenerator<TreeNode>) item -> item.isFinalAttribute() || item.isValue() ? null : "disabled-tree-node");
        return tree;
    }

    private HorizontalLayout buildMainLayout(Component... layouts) {
        HorizontalLayout mainLayout = new HorizontalLayout(layouts);
        mainLayout.setSizeFull();
        return mainLayout;
    }

    private VerticalLayout buildAttributesMappingLayout() {
        attributesMappingLayout = new VerticalLayout();
        attributesMappingLayout.setSizeUndefined();
        Panel attributesMappingPanel = new Panel("Mappings", attributesMappingLayout);
        attributesMappingPanel.setSizeFull();
        publishedCheckBox = new CheckBox("Published");
        Button saveButton = new Button("Save");
        saveButton.setWidth(100, Unit.PERCENTAGE);
        saveButton.addClickListener((Button.ClickListener) event -> saveConfiguration());
        VerticalLayout attributesMappingOuterLayout = new VerticalLayout(attributesMappingPanel, publishedCheckBox, saveButton);
        attributesMappingOuterLayout.setSizeFull();
        attributesMappingOuterLayout.setExpandRatio(attributesMappingPanel, 1f);
        return attributesMappingOuterLayout;
    }

    private HorizontalLayout buildAttributesPairLayout(String sourceAttribute, String targetAttribute) {
        HorizontalLayout attributesPairLayout = new HorizontalLayout();

        TextField sourceAttributeTextField = new TextField("Source attribute name", sourceAttribute);
        sourceAttributeTextField.setReadOnly(true);
        DropTargetExtension<TextField> dropTarget = new DropTargetExtension<>(sourceAttributeTextField);
        dropTarget.setDropEffect(DropEffect.COPY);
        dropTarget.addDropListener(new TextFieldDropListener(sourceAttributeTextField));

        ComboBox<String> targetAttributeComboBox = buildGlobalAttributesComboBox(targetAttribute);
        targetAttributeComboBox.addValueChangeListener((HasValue.ValueChangeListener<String>) event -> publishedCheckBox.setEnabled(isEverythingFilled()));

        attributesMapping.put(sourceAttributeTextField, targetAttributeComboBox);

        Button deleteMappingButton = new Button("Delete mapping");
        deleteMappingButton.addClickListener((Button.ClickListener) event -> {
            ((AbstractLayout) attributesPairLayout.getParent()).removeComponent(attributesPairLayout);
            attributesMapping.remove(sourceAttributeTextField);
            publishedCheckBox.setEnabled(isEverythingFilled());
        });

        attributesPairLayout.addComponent(sourceAttributeTextField);
        attributesPairLayout.addComponent(targetAttributeComboBox);
        attributesPairLayout.addComponent(deleteMappingButton);
        attributesPairLayout.setComponentAlignment(deleteMappingButton, Alignment.BOTTOM_LEFT);
        attributesPairLayout.setSizeUndefined();
        return attributesPairLayout;
    }

    private ComboBox<String> buildGlobalAttributesComboBox(String targetAttribute) {
        List<String> globalAttributes = new ArrayList<>();
        try {
            globalAttributes = FileUtils.readLines(new File("indices/.gattrs"), Charset.defaultCharset());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        ComboBox<String> targetAttributeName = new ComboBox<>("Target attribute name", globalAttributes);
        targetAttributeName.setSelectedItem(targetAttribute);
        return targetAttributeName;
    }

    private boolean isEverythingFilled() {
        return attributesMapping.keySet().stream().noneMatch(tf -> StringUtils.isEmpty(tf.getValue())) &&
                attributesMapping.values().stream().noneMatch(tf -> StringUtils.isEmpty(tf.getValue()));
    }

    private void loadConfiguration() {
        DataProvider.Configuration configuration = getCurrentDataProvider().loadConfiguration();
        attributesMapping.clear();
        attributesMappingLayout.removeAllComponents();
        for (Map.Entry<String, String> mapping : configuration.getAttributesMapping().entrySet()) {
            addMappingPair(mapping.getKey(), mapping.getValue());
        }
        publishedCheckBox.setValue(configuration.isPublished());
        publishedCheckBox.setEnabled(isEverythingFilled());
    }

    private void addMappingPair(String sourceAttribute, String targetAttribute) {
        HorizontalLayout attributesPairLayout = buildAttributesPairLayout(sourceAttribute, targetAttribute);
        attributesMappingLayout.addComponent(attributesPairLayout);
    }

    private void saveConfiguration() {
        DataProvider currentDataProvider = getCurrentDataProvider();
        DataProvider.Configuration configuration = currentDataProvider.loadConfiguration();
        configuration.setPublished(publishedCheckBox.getValue());
        configuration.getAttributesMapping().clear();
        for (Map.Entry<TextField, ComboBox<String>> mapping : attributesMapping.entrySet()) {
            configuration.getAttributesMapping().put(mapping.getKey().getValue(), mapping.getValue().getValue());
        }
        currentDataProvider.saveConfiguration(configuration);
    }

}
