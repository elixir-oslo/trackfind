package no.uio.ifi.trackfind.frontend;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.HasValue;
import com.vaadin.data.TreeData;
import com.vaadin.data.ValueProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.server.Sizeable;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.shared.ui.dnd.DropEffect;
import com.vaadin.shared.ui.dnd.EffectAllowed;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import com.vaadin.ui.components.grid.TreeGridDragSource;
import com.vaadin.ui.dnd.DropTargetExtension;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.dao.Mapping;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.backend.repositories.MappingRepository;
import no.uio.ifi.trackfind.frontend.components.TrackFindTree;
import no.uio.ifi.trackfind.frontend.data.TreeNode;
import no.uio.ifi.trackfind.frontend.filters.AdminTreeFilter;
import no.uio.ifi.trackfind.frontend.listeners.TextFieldDropListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.vaadin.dialogs.ConfirmDialog;

import javax.transaction.Transactional;
import java.util.*;
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

    private MappingRepository mappingRepository;

    private Button addStaticMappingButton;
    private Button addDynamicMappingButton;
    private VerticalLayout attributesMappingLayout;

    private Map<ComboBox<String>, TextField> attributesStaticMapping = new HashMap<>();
    private Map<ComboBox<String>, TextArea> attributesDynamicMapping = new HashMap<>();

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

        for (DataProvider dataProvider : trackFindService.getDataProviders()) {
            TrackFindTree<TreeNode> tree = buildTree(dataProvider);
            refreshTrees(true);
            tabSheet.addTab(tree, dataProvider.getName());
        }

        tabSheet.addSelectedTabChangeListener((TabSheet.SelectedTabChangeListener) event -> loadConfiguration());

        Panel treePanel = new Panel("Model browser", tabSheet);
        treePanel.setSizeFull();

        TextField attributesFilterTextField = new TextField("Filter attributes", (HasValue.ValueChangeListener<String>) event -> {
            TreeDataProvider<TreeNode> dataProvider = getCurrentTreeDataProvider();
            ((AdminTreeFilter) dataProvider.getFilter()).setAttributesFilter(event.getValue());
            dataProvider.refreshAll();
        });
        attributesFilterTextField.setValueChangeMode(ValueChangeMode.EAGER);
        attributesFilterTextField.setWidth(100, Sizeable.Unit.PERCENTAGE);

        addStaticMappingButton = new Button("Add static mapping");
        addStaticMappingButton.setWidth(100, Unit.PERCENTAGE);
        addStaticMappingButton.setEnabled(!CollectionUtils.isEmpty(getCurrentTree().getSelectedItems()));
        addStaticMappingButton.addClickListener((Button.ClickListener) event -> {
            Set<TreeNode> selectedItems = getCurrentTree().getSelectedItems();
            String sourceAttribute = CollectionUtils.isEmpty(selectedItems) ? "" : selectedItems.iterator().next().getPath();
            addStaticMappingPair("", sourceAttribute.replaceAll("'", ""));
        });

        addDynamicMappingButton = new Button("Add dynamic mapping");
        addDynamicMappingButton.setWidth(100, Unit.PERCENTAGE);
        addDynamicMappingButton.addClickListener((Button.ClickListener) event -> {
            Set<TreeNode> selectedItems = getCurrentTree().getSelectedItems();
            String sourceAttribute = CollectionUtils.isEmpty(selectedItems) ? "" : selectedItems.iterator().next().getPath();
            addDynamicMappingPair("", sourceAttribute);
        });

        HorizontalLayout mappingButtons = new HorizontalLayout(addStaticMappingButton, addDynamicMappingButton);
        mappingButtons.setWidth(100, Unit.PERCENTAGE);
        VerticalLayout treeLayout = new VerticalLayout(treePanel, attributesFilterTextField, mappingButtons);
        treeLayout.setSizeFull();
        treeLayout.setExpandRatio(treePanel, 1f);
        return treeLayout;
    }

    @SuppressWarnings("unchecked")
    protected TrackFindTree<TreeNode> buildTree(DataProvider dataProvider) {
        TreeDataProvider treeDataProvider = new TreeDataProvider(new TreeData());
        treeDataProvider.setFilter(new AdminTreeFilter(treeDataProvider, ""));
        treeDataProvider.setSortOrder((ValueProvider<TreeNode, String>) TreeNode::toString, SortDirection.ASCENDING);
        dataProviders.put(treeDataProvider, dataProvider);

        TrackFindTree<TreeNode> tree = new TrackFindTree<>(dataProvider);
        tree.setSelectionMode(Grid.SelectionMode.SINGLE);
        tree.addSelectionListener((SelectionListener<TreeNode>) event -> addStaticMappingButton.setEnabled(!CollectionUtils.isEmpty(event.getAllSelectedItems())));
        tree.setDataProvider(treeDataProvider);
        tree.setSizeFull();
        tree.setStyleGenerator((StyleGenerator<TreeNode>) item -> treeDataProvider.getChildCount(item.getQuery()) == 0 ? "no-children-tree-node" : null);

        TreeGridDragSource<TreeNode> dragSource = new TreeGridDragSource<>((TreeGrid<TreeNode>) tree.getCompositionRoot());
        dragSource.setEffectAllowed(EffectAllowed.COPY);

        return tree;
    }

    @Override
    protected void fillTreeData(TreeData<TreeNode> treeData, TreeNode treeNode) {
        if (treeNode.isFinalAttribute()) {
            return;
        }
        treeData.addItems(treeNode, treeNode.getChildren());
        for (TreeNode child : treeNode.getChildren()) {
            fillTreeData(treeData, child);
        }
    }

    private HorizontalLayout buildMainLayout(Component... layouts) {
        HorizontalLayout mainLayout = new HorizontalLayout(layouts);
        mainLayout.setSizeFull();
        return mainLayout;
    }

    @SuppressWarnings("PMD.NPathComplexity")
    private VerticalLayout buildAttributesMappingLayout() {
        attributesMappingLayout = new VerticalLayout();
        attributesMappingLayout.setWidth(100, Unit.PERCENTAGE);
        Panel attributesMappingPanel = new Panel("Mappings", attributesMappingLayout);
        attributesMappingPanel.setSizeFull();
        Button saveButton = new Button("Save");
        saveButton.setSizeFull();
        saveButton.addClickListener((Button.ClickListener) event -> saveConfiguration());
        Button crawlButton = new Button("Crawl");
        crawlButton.setSizeFull();
        crawlButton.addClickListener((Button.ClickListener) event -> ConfirmDialog.show(getUI(),
                "Are you sure? " +
                        "Crawling is time-consuming process and will lead to changing the metadata of in the database.",
                (ConfirmDialog.Listener) dialog -> {
                    if (dialog.isConfirmed()) {
                        getCurrentDataProvider().crawlRemoteRepository();
                    }
                }));
        Button applyMappingsButton = new Button("Apply mappings");
        applyMappingsButton.setSizeFull();
        applyMappingsButton.addClickListener((Button.ClickListener) event -> ConfirmDialog.show(getUI(),
                "Are you sure? " +
                        "Applying attribute mappings will change the metadata structure in the database.",
                (ConfirmDialog.Listener) dialog -> {
                    if (dialog.isConfirmed()) {
                        getCurrentDataProvider().applyMappings();
                    }
                }));
        HorizontalLayout buttonsLayout = new HorizontalLayout(saveButton, crawlButton, applyMappingsButton);
        buttonsLayout.setWidth(100, Unit.PERCENTAGE);
        buttonsLayout.setEnabled(!properties.isDemoMode());
        VerticalLayout attributesMappingOuterLayout = new VerticalLayout(attributesMappingPanel, buttonsLayout);
        attributesMappingOuterLayout.setSizeFull();
        attributesMappingOuterLayout.setExpandRatio(attributesMappingPanel, 0.8f);
        return attributesMappingOuterLayout;
    }

    private HorizontalLayout buildAttributeToAttributeLayout(String basicAttribute, String sourceAttribute) {
        HorizontalLayout attributeToAttributeLayout = new HorizontalLayout();
        attributeToAttributeLayout.setWidth(100, Unit.PERCENTAGE);

        TextField sourceAttributeTextField = new TextField("Source attribute name", sourceAttribute);
        sourceAttributeTextField.setWidth(100, Unit.PERCENTAGE);
        sourceAttributeTextField.setReadOnly(true);
        DropTargetExtension<TextField> dropTarget = new DropTargetExtension<>(sourceAttributeTextField);
        dropTarget.setDropEffect(DropEffect.COPY);
        dropTarget.addDropListener(new TextFieldDropListener(sourceAttributeTextField));

        ComboBox<String> targetAttributeComboBox = buildBasicAttributesComboBox(basicAttribute);
        targetAttributeComboBox.setWidth(100, Unit.PERCENTAGE);
        attributesStaticMapping.put(targetAttributeComboBox, sourceAttributeTextField);

        Button deleteMappingButton = new Button("Delete mapping");
        deleteMappingButton.setWidth(100, Unit.PERCENTAGE);
        deleteMappingButton.addClickListener((Button.ClickListener) event -> {
            ((AbstractLayout) attributeToAttributeLayout.getParent()).removeComponent(attributeToAttributeLayout);
            attributesStaticMapping.remove(targetAttributeComboBox);
        });

        attributeToAttributeLayout.addComponent(targetAttributeComboBox);
        attributeToAttributeLayout.setComponentAlignment(targetAttributeComboBox, Alignment.BOTTOM_LEFT);
        attributeToAttributeLayout.addComponent(sourceAttributeTextField);
        attributeToAttributeLayout.setComponentAlignment(sourceAttributeTextField, Alignment.BOTTOM_LEFT);
        attributeToAttributeLayout.addComponent(deleteMappingButton);
        attributeToAttributeLayout.setComponentAlignment(deleteMappingButton, Alignment.BOTTOM_LEFT);
        return attributeToAttributeLayout;
    }

    private HorizontalLayout buildAttributeToScriptLayout(String basicAttribute, String script) {
        HorizontalLayout attributeToScriptLayout = new HorizontalLayout();
        attributeToScriptLayout.setWidth(100, Unit.PERCENTAGE);

        ComboBox<String> targetAttributeComboBox = buildBasicAttributesComboBox(basicAttribute);
        targetAttributeComboBox.setWidth(100, Unit.PERCENTAGE);
        TextArea scriptTextArea = new TextArea("Script", script);
        scriptTextArea.setWidth(100, Unit.PERCENTAGE);
        attributesDynamicMapping.put(targetAttributeComboBox, scriptTextArea);

        Button deleteMappingButton = new Button("Delete mapping");
        deleteMappingButton.setWidth(100, Unit.PERCENTAGE);
        deleteMappingButton.addClickListener((Button.ClickListener) event -> {
            ((AbstractLayout) attributeToScriptLayout.getParent()).removeComponent(attributeToScriptLayout);
            attributesDynamicMapping.remove(targetAttributeComboBox);
        });

        attributeToScriptLayout.addComponent(targetAttributeComboBox);
        attributeToScriptLayout.setComponentAlignment(targetAttributeComboBox, Alignment.BOTTOM_LEFT);
        attributeToScriptLayout.addComponent(scriptTextArea);
        attributeToScriptLayout.setComponentAlignment(scriptTextArea, Alignment.BOTTOM_LEFT);
        attributeToScriptLayout.addComponent(deleteMappingButton);
        attributeToScriptLayout.setComponentAlignment(deleteMappingButton, Alignment.BOTTOM_LEFT);
        return attributeToScriptLayout;
    }

    private ComboBox<String> buildBasicAttributesComboBox(String targetAttribute) {
        ComboBox<String> targetAttributeName = new ComboBox<>("Target attribute name", properties.getBasicAttributes());
        targetAttributeName.setSelectedItem(targetAttribute);
        return targetAttributeName;
    }

    private void loadConfiguration() {
        String repository = getCurrentDataProvider().getName();
        Collection<Mapping> mappings = mappingRepository.findByRepository(repository);
        Collection<Mapping> staticMappings = mappings.stream().filter(Mapping::getStaticMapping).collect(Collectors.toSet());
        Collection<Mapping> dynamicMappings = mappings.stream().filter(m -> !m.getStaticMapping()).collect(Collectors.toSet());
        attributesStaticMapping.clear();
        attributesDynamicMapping.clear();
        attributesMappingLayout.removeAllComponents();
        for (Mapping mapping : staticMappings) {
            addStaticMappingPair(mapping.getTo(), mapping.getFrom());
        }
        for (Mapping mapping : dynamicMappings) {
            addDynamicMappingPair(mapping.getTo(), mapping.getFrom());
        }
    }

    private void addStaticMappingPair(String basicAttribute, String sourceAttribute) {
        attributesMappingLayout.addComponent(buildAttributeToAttributeLayout(basicAttribute, sourceAttribute));
    }

    private void addDynamicMappingPair(String basicAttribute, String script) {
        attributesMappingLayout.addComponent(buildAttributeToScriptLayout(basicAttribute, script));
    }

    @Transactional
    protected void saveConfiguration() {
        String repository = getCurrentDataProvider().getName();
        Collection<Mapping> mappings = new HashSet<>();
        for (Map.Entry<ComboBox<String>, TextField> mappingPair : attributesStaticMapping.entrySet()) {
            Mapping mapping = new Mapping();
            mapping.setRepository(repository);
            mapping.setStaticMapping(true);
            mapping.setFrom(mappingPair.getValue().getValue());
            mapping.setTo(mappingPair.getKey().getValue());
            mappings.add(mapping);
        }
        for (Map.Entry<ComboBox<String>, TextArea> mappingPair : attributesDynamicMapping.entrySet()) {
            Mapping mapping = new Mapping();
            mapping.setRepository(repository);
            mapping.setStaticMapping(false);
            mapping.setFrom(mappingPair.getValue().getValue());
            mapping.setTo(mappingPair.getKey().getValue());
            mappings.add(mapping);
        }
        mappingRepository.saveAll(mappings);
        Notification.show("Mappings saved. Press \"Apply\" for changes to take effect.");
    }

    @Autowired
    public void setMappingRepository(MappingRepository mappingRepository) {
        this.mappingRepository = mappingRepository;
    }

}
