package no.uio.ifi.trackfind.frontend;

import alexh.weak.Dynamic;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.component.VaadinClipboard;
import com.vaadin.component.VaadinClipboardImpl;
import com.vaadin.data.HasValue;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.HierarchicalDataCommunicator;
import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.event.CollapseEvent;
import com.vaadin.event.ExpandEvent;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.*;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.shared.ui.dnd.DropEffect;
import com.vaadin.shared.ui.dnd.EffectAllowed;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import com.vaadin.ui.components.grid.TreeGridDragSource;
import com.vaadin.ui.dnd.DropTargetExtension;
import com.vaadin.util.FileTypeResolver;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.TreeNode;
import no.uio.ifi.trackfind.backend.pojo.SearchResult;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import no.uio.ifi.trackfind.backend.pojo.TfObjectType;
import no.uio.ifi.trackfind.backend.services.impl.GSuiteService;
import no.uio.ifi.trackfind.backend.services.impl.MetamodelService;
import no.uio.ifi.trackfind.backend.services.impl.SchemaService;
import no.uio.ifi.trackfind.backend.services.impl.SearchService;
import no.uio.ifi.trackfind.frontend.components.KeyboardInterceptorExtension;
import no.uio.ifi.trackfind.frontend.components.ResultTreeItemWrapper;
import no.uio.ifi.trackfind.frontend.components.TrackFindTree;
import no.uio.ifi.trackfind.frontend.filters.TreeFilter;
import no.uio.ifi.trackfind.frontend.listeners.AddToQueryButtonClickListener;
import no.uio.ifi.trackfind.frontend.listeners.TextAreaDropListener;
import no.uio.ifi.trackfind.frontend.listeners.TreeItemClickListener;
import no.uio.ifi.trackfind.frontend.listeners.TreeSelectionListener;
import no.uio.ifi.trackfind.frontend.providers.TrackFindDataProvider;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Main Vaadin UI of the application.
 * Capable of displaying metadata for repositories, constructing and executing search queries along with exporting the results.
 * Uses custom theme (VAADIN/themes/trackfind/trackfind.scss).
 * Uses custom WidgetSet (TrackFindWidgetSet.gwt.xml).
 *
 * @author Dmytro Titov
 */
@SpringUI(path = "/")
@Widgetset("TrackFindWidgetSet")
@Title("TrackFind")
@Theme("trackfind")
@Slf4j
public class TrackFindMainUI extends AbstractUI {

    public static Map<String, List<String>> SHORTCUTS = Map.of(
            "Cell/Tissue type", List.of("samples", "sample_type", "summary"),
            "Experiment type", List.of("experiments", "technique", "term_label"),
            "Genome assembly", List.of("tracks", "assembly_name"),
            "Target", List.of("experiments", "target", "summary"),
            "File format", List.of("tracks", "file_format", "term_label"),
            "Type of condensed data", List.of("tracks", "type_of_condensed_data"),
            "Phenotype", List.of("samples", "phenotype", "term_label"),
            "Geometric track type", List.of("tracks", "geometric_track_type")
    );

    private ObjectMapper mapper;
    private MetamodelService metamodelService;
    private GSuiteService gSuiteService;
    private SearchService searchService;

    private Button copyButton = new Button("Copy to clipboard");
    private Button visitButton = new Button("Open in a new tab");
    private Button addToQueryButton = new Button("Add to query ➚ (⌥: OR, ⇧: NOT)");
    private List<TreeNode> expandedItems = new CopyOnWriteArrayList<>();
    private Button exportGSuiteButton = new Button("Export as GSuite file");
    private Button exportJSONButton = new Button("Export as JSON file");
    private CheckBoxGroup<String> categoriesChecklist = new CheckBoxGroup<>();
    private TextArea queryTextArea;
    private TextField limitTextField;
    private Panel resultsPanel;
    private Tree<ResultTreeItemWrapper> resultsTree = new Tree<>();
    private Grid<SearchResult> resultsTable = new Grid<>(SearchResult.class);
    private Collection<SearchResult> results = new ArrayList<>();
    private String jsonResult;
    private FileDownloader gSuiteFileDownloader;
    private FileDownloader jsonFileDownloader;


    @Override
    protected void init(VaadinRequest vaadinRequest) {
        HorizontalLayout headerLayout = buildHeaderLayout();
        VerticalLayout treeLayout = buildTreeLayout();
        VerticalLayout queryLayout = buildQueryLayout();
        VerticalLayout resultsLayout = buildResultsLayout();
        HorizontalLayout mainLayout = buildMainLayout(treeLayout, queryLayout, resultsLayout);
        mainLayout.setMargin(new MarginInfo(true, true, true, true));
        HorizontalLayout footerLayout = buildFooterLayout();
        VerticalLayout outerLayout = buildOuterLayout(headerLayout, mainLayout, footerLayout);
        setContent(outerLayout);
        String implementationVersion = getClass().getPackage().getImplementationVersion();
        implementationVersion = implementationVersion == null ? "dev" : implementationVersion;
        Page currentPage = Page.getCurrent();
        currentPage.setTitle("TrackFind: " + implementationVersion);
    }

    protected VerticalLayout buildTreeLayout() {
        tabSheet = new TabSheet();
        tabSheet.setSizeFull();

//        VerticalLayout popupContent = new VerticalLayout();
        copyButton.setWidthFull();
        copyButton.setEnabled(false);
        copyButton.addClickListener((Button.ClickListener) clickEvent -> {
            VaadinClipboard vaadinClipboard = VaadinClipboardImpl.GetInstance();
            vaadinClipboard.copyToClipboard(getCurrentTree().getSelectedItems().iterator().next().getValue(), b -> {
                // do nothing
            });
        });
//        popupContent.addComponent(copyButton);

        visitButton.setWidthFull();
        visitButton.setEnabled(false);
        visitButton.addClickListener((Button.ClickListener) clickEvent -> {
            TreeNode treeNode = getCurrentTree().getSelectedItems().iterator().next();
            String value = treeNode.getValue();
            try {
                new URL(value);
                getUI().getPage().open(value, "_blank");
            } catch (MalformedURLException e) {
                if (value.contains(":")) {
                    getUI().getPage().open("http://identifiers.org/resolve?query=" + value, "_blank");
                } else {
                    String category = treeNode.getPath().split(separator)[0];
                    String query = treeNode.getSQLPath();
                    try {
                        Collection<SearchResult> searchResults = searchService.search(
                                getCurrentHub().getRepository(),
                                getCurrentHub().getName(),
                                query.replaceAll("'term_label'->", "'term_label' ? "),
                                List.of(category),
                                1
                        ).getValue();
                        SearchResult searchResult = searchResults.iterator().next();
                        String termIDPath = StringUtils.substringBeforeLast(treeNode.getPath().replaceAll("term_label", "term_id"), separator);
                        String termID = Dynamic.from(searchResult.getContent()).get(termIDPath, separator).asString();
                        getUI().getPage().open(termID, "_blank");
                    } catch (SQLException ignored) {
                    }
                }
            }
        });

        for (TfHub hub : trackFindService.getTrackHubs(true)) {
            TrackFindTree<TreeNode> tree = buildMetamodelTree(hub);
            tabSheet.addTab(tree, hub.getDisplayName() != null ? hub.getDisplayName() : hub.getName());
        }

        ComboBox<String> shortcuts = new ComboBox<>("Shortcuts");
        shortcuts.setItems(SHORTCUTS.keySet());
        shortcuts.addValueChangeListener((HasValue.ValueChangeListener<String>) valueChangeEvent -> {
            if (StringUtils.isEmpty(valueChangeEvent.getValue())) {
                return;
            }
            TrackFindTree<TreeNode> currentTree = getCurrentTree();
            currentTree.getSelectedItems().forEach(currentTree::deselect);
            currentTree.collapse(expandedItems);
            List<String> path = SHORTCUTS.get(valueChangeEvent.getValue());
            List<TreeNode> nodesToExpand = getNodesByPath(path);
            if (CollectionUtils.isNotEmpty(nodesToExpand)) {
                currentTree.expand(nodesToExpand);
                currentTree.select(nodesToExpand.get(nodesToExpand.size() - 1));
            } else {
                Notification.show("Shortcut not found in the current hub.");
            }
        });

        CheckBox standardCheckbox = new CheckBox("Show only FAIRtracks attributes");
        standardCheckbox.addValueChangeListener((HasValue.ValueChangeListener<Boolean>) event -> {
            TreeFilter filter = getCurrentFilter();
            filter.setStandard(event.getValue());
            getCurrentTree().getDataProvider().refreshAll();
        });
        VerticalLayout checkboxLayout = new VerticalLayout(standardCheckbox);
        checkboxLayout.setSizeFull();
        checkboxLayout.setComponentAlignment(standardCheckbox, Alignment.BOTTOM_LEFT);
        checkboxLayout.setMargin(false);
        HorizontalLayout shortcutsLayout = new HorizontalLayout(shortcuts, checkboxLayout);
        shortcutsLayout.setMargin(false);
        VerticalLayout selectionLayout = new VerticalLayout(shortcutsLayout, tabSheet);
        selectionLayout.setSizeFull();
        selectionLayout.setExpandRatio(tabSheet, 1);
        Panel treePanel = new Panel("1. Select metadata value(s)", selectionLayout);
        treePanel.setSizeFull();

//        TextField attributesFilterTextField = createFilter(true);
        TextField valuesFilterTextField = createFilter(false);

        tabSheet.addSelectedTabChangeListener((TabSheet.SelectedTabChangeListener) event -> refreshCategoriesCheckList());

        addToQueryButton.setEnabled(false);
        KeyboardInterceptorExtension keyboardInterceptorExtension = new KeyboardInterceptorExtension(addToQueryButton);
        AddToQueryButtonClickListener addToQueryButtonClickListener = new AddToQueryButtonClickListener(this, keyboardInterceptorExtension, separator);
        addToQueryButton.addClickListener(addToQueryButtonClickListener);
        addToQueryButton.setWidth(100, Unit.PERCENTAGE);

        HorizontalLayout copyVisitButtonsLayout = new HorizontalLayout(copyButton, visitButton);
        copyVisitButtonsLayout.setWidthFull();
        copyVisitButtonsLayout.setExpandRatio(copyButton, 1);
        copyVisitButtonsLayout.setExpandRatio(visitButton, 1);

        VerticalLayout treeLayout = new VerticalLayout(treePanel, copyVisitButtonsLayout, valuesFilterTextField, addToQueryButton);
        treeLayout.setSizeFull();
        treeLayout.setExpandRatio(treePanel, 1f);
        return treeLayout;
    }

    protected List<TreeNode> getRootNodes() {
        TrackFindTree<TreeNode> currentTree = getCurrentTree();
        TrackFindDataProvider dataProvider = (TrackFindDataProvider) currentTree.getDataProvider();
        TreeFilter filter = getCurrentFilter();
        return dataProvider.fetch(new HierarchicalQuery<>(filter, null)).collect(Collectors.toList());
    }

    protected List<TreeNode> getNodesByParent(TreeNode parent) {
        TrackFindTree<TreeNode> currentTree = getCurrentTree();
        TrackFindDataProvider dataProvider = (TrackFindDataProvider) currentTree.getDataProvider();
        TreeFilter filter = getCurrentFilter();
        return dataProvider.fetch(new HierarchicalQuery<>(filter, parent)).collect(Collectors.toList());
    }

    protected List<TreeNode> getNodesByPath(List<String> path) {
        List<TreeNode> result = new ArrayList<>();
        List<TreeNode> nodes = getRootNodes();
        for (String value : path) {
            TreeNode treeNode = nodes.stream().filter(n -> n.getValue().equals(value)).findAny().orElse(null);
            result.add(treeNode);
            nodes = getNodesByParent(treeNode);
        }
        return result.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    protected TrackFindTree<TreeNode> buildMetamodelTree(TfHub hub) {
        TrackFindTree<TreeNode> tree = new TrackFindTree<>(hub);
        tree.setDataProvider(trackFindDataProvider);
        tree.setSelectionMode(Grid.SelectionMode.MULTI);
        tree.addItemClickListener(new TreeItemClickListener(tree));
        tree.setItemDescriptionGenerator((DescriptionGenerator<TreeNode>) treeNode -> {
            if (!treeNode.isStandard()) {
                return null;
            }
            if (treeNode.getParent() == null) { // category
                Map<String, String> categories = schemaService.getCategories();
                if (categories.containsKey(treeNode.getCategory())) {
                    return categories.get(treeNode.getCategory());
                }
                return null;
            }
            Collection<SchemaService.Attribute> attributes = schemaService.getAttributes().get(treeNode.getCategory());
            Optional<SchemaService.Attribute> optionalAttribute = attributes
                    .stream()
                    .filter(a -> (treeNode.getCategory() + separator + a.getPath().replace("'", "")).equals(treeNode.getPath()))
                    .findAny();
            if (optionalAttribute.isEmpty()) {
                return null;
            }
            return optionalAttribute.get().getDescription();
        });
        TreeGrid<TreeNode> treeGrid = (TreeGrid<TreeNode>) tree.getCompositionRoot();
        TreeFilter filter = new TreeFilter(hub, false, "", "");
        treeGrid.getDataCommunicator().setFilter(filter);
        tree.addSelectionListener(new TreeSelectionListener(tree, filter, copyButton, visitButton, addToQueryButton, new KeyboardInterceptorExtension(tree)));
        tree.setSizeFull();
        tree.setStyleGenerator((StyleGenerator<TreeNode>) item -> {
            if (item.isAttribute()) {
                if (item.isStandard()) {
                    return "standard-tree-node";
                } else {
                    return null;
                }
            } else {
                return "value-tree-node";
            }
        });

        tree.addExpandListener((ExpandEvent.ExpandListener<TreeNode>) event -> expandedItems.add(event.getExpandedItem()));
        tree.addCollapseListener((CollapseEvent.CollapseListener<TreeNode>) event -> expandedItems.remove(event.getCollapsedItem()));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            TreeGridDragSource<TreeNode> dragSource = new TreeGridDragSource<>(treeGrid);
            dragSource.setEffectAllowed(EffectAllowed.COPY);
        }

        return tree;
    }

    private HorizontalLayout buildMainLayout(VerticalLayout treeLayout, VerticalLayout queryLayout, VerticalLayout resultsLayout) {
        treeLayout.setMargin(false);
        treeLayout.setSizeFull();
        queryLayout.setMargin(new MarginInfo(false, true, true, true));
        queryLayout.setSizeFull();
        resultsLayout.setMargin(false);
        resultsLayout.setSizeFull();
        HorizontalLayout mainLayout = new HorizontalLayout(treeLayout, queryLayout, resultsLayout);
        mainLayout.setSizeFull();
        return mainLayout;
    }

    private VerticalLayout buildResultsLayout() {
        exportGSuiteButton.setEnabled(false);
        exportGSuiteButton.setWidth(100, Unit.PERCENTAGE);

        exportJSONButton.setEnabled(false);
        exportJSONButton.setWidth(100, Unit.PERCENTAGE);

        gSuiteFileDownloader = new FileDownloader(new ExternalResource(""));
        gSuiteFileDownloader.extend(exportGSuiteButton);

        jsonFileDownloader = new FileDownloader(new ExternalResource(""));
        jsonFileDownloader.extend(exportJSONButton);

        TabSheet resultsTabs = new TabSheet();
        tabSheet.setSizeFull();
        resultsTabs.addTab(resultsTable, "Table view");
        resultsTabs.addTab(resultsTree, "Tree view");

        resultsTable.setSizeFull();
        int i = 0;
        for (Map.Entry<String, List<String>> shortcut : SHORTCUTS.entrySet()) {
            resultsTable.addColumn(sr ->
                    {
                        String path = String.join(".", shortcut.getValue());
                        return Dynamic.from(sr.getContent()).dget(path).asString();
                    }
            ).setCaption(shortcut.getKey()).setId(String.valueOf(i++));
        }
        resultsTable.removeColumn("content");

        resultsTree.setSizeFull();
        resultsTree.setItemCaptionGenerator((ItemCaptionGenerator<ResultTreeItemWrapper>) ResultTreeItemWrapper::getValue);
        resultsTree.setStyleGenerator((StyleGenerator<ResultTreeItemWrapper>) item -> item.isLeaf() ? "value-tree-node" : null);

        resultsPanel = new Panel("3. Results", resultsTabs);
        resultsPanel.getContent().setSizeFull();
        resultsPanel.setSizeFull();
        VerticalLayout resultsLayout = new VerticalLayout(resultsPanel, exportGSuiteButton, exportJSONButton);
        resultsLayout.setSizeFull();
        resultsLayout.setExpandRatio(resultsPanel, 1f);
        return resultsLayout;
    }

    private VerticalLayout buildQueryLayout() {
        queryTextArea = new TextArea();
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

        Button clearAllButton = new Button("Clear search query");
        clearAllButton.setSizeFull();
        clearAllButton.addClickListener((Button.ClickListener) event -> queryTextArea.clear());

        Button searchButton = new Button("Search ➚", (Button.ClickListener) clickEvent -> executeQuery(queryTextArea.getValue()));
        queryTextArea.addValueChangeListener((HasValue.ValueChangeListener<String>) event -> searchButton.setEnabled(StringUtils.isNotEmpty(queryTextArea.getValue())));
        queryTextArea.addStyleName("scrollable-text-area");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            DropTargetExtension<TextArea> dropTarget = new DropTargetExtension<>(queryTextArea);
            dropTarget.setDropEffect(DropEffect.COPY);
            TextAreaDropListener textAreaDropListener = new TextAreaDropListener(queryTextArea, separator);
            dropTarget.addDropListener(textAreaDropListener);
        }
        Panel queryPanel = new Panel("2. Search query", queryTextArea);
        queryPanel.setSizeFull();

        searchButton.setWidth(100, Unit.PERCENTAGE);
        searchButton.setEnabled(false);

        limitTextField = new TextField("Max. number of results", "10");
        limitTextField.setWidth(100, Unit.PERCENTAGE);
        limitTextField.setValueChangeMode(ValueChangeMode.EAGER);
        limitTextField.addValueChangeListener((HasValue.ValueChangeListener<String>) valueChangeEvent -> {
            String value = valueChangeEvent.getValue();
            value = StringUtils.isEmpty(value) ? "0" : value;
            try {
                Integer.parseInt(value);
                limitTextField.setComponentError(null);
                queryTextArea.setEnabled(true);
                searchButton.setEnabled(true);
            } catch (NumberFormatException e) {
                limitTextField.setComponentError(new UserError("Should be a valid integer number only!"));
                queryTextArea.setEnabled(false);
                searchButton.setEnabled(false);
            }
        });

        refreshCategoriesCheckList();

        HorizontalLayout searchLayout = new HorizontalLayout(limitTextField, searchButton);
        searchLayout.setWidth("100%");
        searchLayout.setComponentAlignment(searchButton, Alignment.BOTTOM_RIGHT);

        Panel categoriesChecklistPanel = new Panel("Filter categories to search in", new VerticalLayout(categoriesChecklist));
        categoriesChecklistPanel.setSizeFull();
        VerticalLayout queryLayout = new VerticalLayout(queryPanel, clearAllButton, categoriesChecklistPanel, searchLayout);
        queryLayout.setExpandRatio(queryPanel, 0.4f);
        queryLayout.setExpandRatio(clearAllButton, 0.1f);
        queryLayout.setExpandRatio(categoriesChecklistPanel, 0.4f);
        queryLayout.setExpandRatio(searchLayout, 0.1f);
        queryLayout.setSizeFull();
        queryLayout.setExpandRatio(queryPanel, 1f);
        return queryLayout;
    }

    private void refreshCategoriesCheckList() {
        Collection<TfObjectType> objectTypes = metamodelService.getObjectTypes(getCurrentHub().getRepository(), getCurrentHub().getName());
        categoriesChecklist.setItems(objectTypes.stream().map(TfObjectType::getName));
    }

    private void executeQuery(String query) {
        TfHub hub = getCurrentHub();
        String limit = limitTextField.getValue();
        limit = StringUtils.isEmpty(limit) ? "0" : limit;
        int count = 0;
        try {
            results = searchService.search(hub.getRepository(), hub.getName(), query, categoriesChecklist.getSelectedItems(), Long.parseLong(limit)).getValue();
            count = searchService.count(hub.getRepository(), hub.getName(), query, categoriesChecklist.getSelectedItems());
        } catch (SQLException e) {
            results = Collections.emptyList();
            log.error(e.getMessage(), e);
        }
        if (results.isEmpty()) {
            resultsTable.setItems(Collections.emptyList());
            resultsTree.setItems(Collections.emptyList());
            resultsPanel.setCaption("3. Results");
            exportGSuiteButton.setEnabled(false);
            exportGSuiteButton.setCaption("Export as GSuite file");
            exportJSONButton.setEnabled(false);
            exportJSONButton.setCaption("Export as JSON file");
            Notification.show("Nothing found for such request");
            return;
        }
        resultsPanel.setCaption(String.format("3. Results: %s out of %s", results.size(), count));
        resultsTable.setItems(results);
        resultsTree.setDataProvider(new TreeDataProvider<>(getResultsTreeData()));
        resultsTree.getDataProvider().refreshAll();
        int numberOfResults = results.size();
        try {
            jsonResult = mapper.writeValueAsString(results);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        exportGSuiteButton.setEnabled(true);
        exportGSuiteButton.setCaption("Export results (" + numberOfResults + " items) as GSuite file");
        exportJSONButton.setEnabled(true);
        exportJSONButton.setCaption("Export results (" + numberOfResults + " items) as JSON file");

        gSuiteFileDownloader.setFileDownloadResource(getStreamResource(null, "gsuite"));
        jsonFileDownloader.setFileDownloadResource(getStreamResource(jsonResult, "json"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private TreeData<ResultTreeItemWrapper> getResultsTreeData() {
        TreeData<ResultTreeItemWrapper> treeData = new TreeData<>();
        int i = 0;
        for (SearchResult result : results) {
            ResultTreeItemWrapper topLevelEntry = new ResultTreeItemWrapper(String.valueOf(++i), false);
            treeData.addRootItems(topLevelEntry);
            Map<String, Object> content = (Map) result.getContent();
            fillResultTree(treeData, topLevelEntry, content);
        }
        return treeData;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void fillResultTree(TreeData<ResultTreeItemWrapper> treeData, ResultTreeItemWrapper parent, Object content) {
        if (content instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) content;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                ResultTreeItemWrapper child = new ResultTreeItemWrapper(entry.getKey(), false);
                treeData.addItem(parent, child);
                Object value = entry.getValue();
                fillResultTree(treeData, child, value);
            }
        } else if (content instanceof Collection) {
            Collection collection = (Collection) content;
            int i = 0;
            for (Object object : collection) {
                ResultTreeItemWrapper arrayEntry = new ResultTreeItemWrapper(String.valueOf(++i), false);
                treeData.addItem(parent, arrayEntry);
                fillResultTree(treeData, arrayEntry, object);
            }
        } else {
            treeData.addItem(parent, new ResultTreeItemWrapper(String.valueOf(content), true));
        }
    }

    private Resource getStreamResource(String content, String extension) {
        return new StreamResource(null, null) {
            @Override
            public StreamSource getStreamSource() {
                if ("json".equalsIgnoreCase(extension)) {
                    return (StreamResource.StreamSource) () -> new ByteArrayInputStream(content.getBytes(Charset.defaultCharset()));
                } else {
                    String gSuiteResult = gSuiteService.apply(results, "");
                    return (StreamResource.StreamSource) () -> new ByteArrayInputStream(gSuiteResult.getBytes(Charset.defaultCharset()));
                }
            }

            @Override
            public String getFilename() {
                TfHub currentHub = getCurrentHub();
                String displayName = currentHub.getDisplayName();
                if (StringUtils.isEmpty(displayName)) {
                    displayName = currentHub.getName();
                }
                String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX")
                        .withZone(ZoneOffset.UTC)
                        .format(Instant.now());
                String count = String.valueOf(results.size());
                return String.format("TF-%s-%s-%sx.%s", displayName, timestamp, count, extension);
            }

            @Override
            public String getMIMEType() {
                return FileTypeResolver.getMIMEType(getFilename());
            }
        };
    }

    public TextArea getQueryTextArea() {
        return queryTextArea;
    }

    @Autowired
    public void setMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Autowired
    public void setMetamodelService(MetamodelService metamodelService) {
        this.metamodelService = metamodelService;
    }

    @Autowired
    public void setGSuiteService(GSuiteService gSuiteService) {
        this.gSuiteService = gSuiteService;
    }

    @Autowired
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

}
