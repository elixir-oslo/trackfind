package no.uio.ifi.trackfind.frontend;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.HasValue;
import com.vaadin.data.provider.Query;
import com.vaadin.event.FieldEvents;
import com.vaadin.event.ShortcutAction;
import com.vaadin.server.Sizeable;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.shared.ui.dnd.DropEffect;
import com.vaadin.shared.ui.dnd.EffectAllowed;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import com.vaadin.ui.components.grid.TreeGridDragSource;
import com.vaadin.ui.dnd.DropTargetExtension;
import com.vaadin.ui.dnd.event.DropListener;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.frontend.components.KeyboardInterceptorExtension;
import no.uio.ifi.trackfind.frontend.components.TrackFindTree;
import no.uio.ifi.trackfind.frontend.data.TreeNode;
import no.uio.ifi.trackfind.frontend.listeners.RemoveSelectedItemsShortcutListener;
import no.uio.ifi.trackfind.frontend.listeners.TextFieldDropListener;
import no.uio.ifi.trackfind.frontend.listeners.TreeItemClickListener;
import no.uio.ifi.trackfind.frontend.listeners.TreeSelectionListener;
import no.uio.ifi.trackfind.frontend.providers.TrackDataProvider;
import no.uio.ifi.trackfind.frontend.providers.impl.AdminTrackDataProvider;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    private Map<String, TextField> basicAttributes;
    private ListSelect<String> exportList;
    private CheckBox publishedCheckBox;

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        HorizontalLayout headerLayout = buildHeaderLayout();
        VerticalLayout treeLayout = buildTreeLayout();
        VerticalLayout fieldsLayout = buildFieldsLayout();
        VerticalLayout exportLayout = buildExportLayout();
        HorizontalLayout mainLayout = buildMainLayout(treeLayout, fieldsLayout, exportLayout);
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

        VerticalLayout treeLayout = new VerticalLayout(treePanel, attributesFilterTextField);
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
        AdminTrackDataProvider trackDataProvider = new AdminTrackDataProvider(new TreeNode(dataProvider.getMetamodelTree(true)));
        tree.setDataProvider(trackDataProvider);
        tree.setSizeFull();
        tree.setStyleGenerator((StyleGenerator<TreeNode>) item -> item.isFinalAttribute() || item.isValue() ? null : "disabled-tree-node");
        return tree;
    }

    private HorizontalLayout buildMainLayout(VerticalLayout treeLayout, VerticalLayout queryLayout, VerticalLayout resultsLayout) {
        HorizontalLayout mainLayout = new HorizontalLayout(treeLayout, queryLayout, resultsLayout);
        mainLayout.setSizeFull();
        return mainLayout;
    }

    private VerticalLayout buildExportLayout() {
        exportList = new ListSelect<>("Attributes to export");
        exportList.setSizeFull();
        exportList.addShortcutListener(new RemoveSelectedItemsShortcutListener<>(exportList, ShortcutAction.KeyCode.DELETE));
        exportList.addShortcutListener(new RemoveSelectedItemsShortcutListener<>(exportList, ShortcutAction.KeyCode.BACKSPACE));
        Button saveButton = new Button("Save");
        publishedCheckBox = new CheckBox("Published");
        saveButton.setWidth(100, Unit.PERCENTAGE);
        saveButton.addClickListener((Button.ClickListener) event -> saveConfiguration());
        VerticalLayout searchExportLayout = new VerticalLayout(exportList, publishedCheckBox, saveButton);
        searchExportLayout.setSizeFull();
        searchExportLayout.setExpandRatio(exportList, 1f);
        return searchExportLayout;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private VerticalLayout buildFieldsLayout() {
        basicAttributes = new HashMap<>();
        VerticalLayout queryLayout = new VerticalLayout();
        for (String basicAttribute : DataProvider.BASIC_ATTRIBUTES) {
            TextField basicAttributeTextField = new TextField(basicAttribute);
            basicAttributeTextField.setWidth(100, Unit.PERCENTAGE);
            basicAttributeTextField.setReadOnly(true);
            DropTargetExtension<TextField> dropTarget = new DropTargetExtension<>(basicAttributeTextField);
            dropTarget.setDropEffect(DropEffect.COPY);
            dropTarget.addDropListener(new TextFieldDropListener(basicAttributeTextField));
            dropTarget.addDropListener((DropListener<TextField>) event -> publishedCheckBox.setEnabled(isEverythingFilled()));
            Button clearButton = new Button("Clear");
            clearButton.addClickListener((Button.ClickListener) event -> basicAttributeTextField.clear());
            clearButton.addClickListener((Button.ClickListener) event -> publishedCheckBox.setEnabled(isEverythingFilled()));
            HorizontalLayout attributeLayout = new HorizontalLayout(basicAttributeTextField, clearButton);
            attributeLayout.setComponentAlignment(basicAttributeTextField, Alignment.BOTTOM_LEFT);
            attributeLayout.setComponentAlignment(clearButton, Alignment.BOTTOM_LEFT);
            attributeLayout.setExpandRatio(basicAttributeTextField, 0.8f);
            attributeLayout.setExpandRatio(clearButton, 0.2f);
            attributeLayout.setWidth(100, Unit.PERCENTAGE);
            basicAttributes.put(basicAttribute, basicAttributeTextField);
            queryLayout.addComponent(attributeLayout);
        }
        queryLayout.setSizeFull();
        return queryLayout;
    }

    boolean isEverythingFilled() {
        return !CollectionUtils.isEmpty(getAttributesToExport()) &&
                basicAttributes.values().stream().noneMatch(tf -> StringUtils.isEmpty(tf.getValue()));
    }

    private void loadConfiguration() {
        DataProvider.Configuration configuration = getCurrentDataProvider().loadConfiguration();
        publishedCheckBox.setValue(configuration.isPublished());
        publishedCheckBox.setEnabled(isEverythingFilled());
        exportList.setItems(configuration.getAttributesToExport());
        for (Map.Entry<String, TextField> mapping : basicAttributes.entrySet()) {
            String value = configuration.getAttributesMapping().getOrDefault(mapping.getKey(), "");
            mapping.getValue().setValue(value);
        }
    }

    private void saveConfiguration() {
        DataProvider currentDataProvider = getCurrentDataProvider();
        DataProvider.Configuration configuration = currentDataProvider.loadConfiguration();
        configuration.setPublished(publishedCheckBox.getValue());
        configuration.setAttributesToExport(getAttributesToExport());
        for (Map.Entry<String, TextField> mapping : basicAttributes.entrySet()) {
            configuration.getAttributesMapping().put(mapping.getKey(), mapping.getValue().getValue());
        }
        currentDataProvider.saveConfiguration(configuration);
    }

    private Set<String> getAttributesToExport() {
        return exportList.getDataProvider().fetch(new Query<>()).collect(Collectors.toSet());
    }

}
