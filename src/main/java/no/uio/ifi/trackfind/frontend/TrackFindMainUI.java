package no.uio.ifi.trackfind.frontend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.HasValue;
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
import no.uio.ifi.trackfind.backend.services.impl.SearchService;
import no.uio.ifi.trackfind.frontend.components.KeyboardInterceptorExtension;
import no.uio.ifi.trackfind.frontend.components.TrackFindTree;
import no.uio.ifi.trackfind.frontend.filters.TreeFilter;
import no.uio.ifi.trackfind.frontend.listeners.AddToQueryButtonClickListener;
import no.uio.ifi.trackfind.frontend.listeners.TextAreaDropListener;
import no.uio.ifi.trackfind.frontend.listeners.TreeItemClickListener;
import no.uio.ifi.trackfind.frontend.listeners.TreeSelectionListener;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;

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

    private ObjectMapper mapper;
    private MetamodelService metamodelService;
    private GSuiteService gSuiteService;
    private SearchService searchService;

    private int numberOfResults;

    private CheckBoxGroup<String> categoriesChecklist = new CheckBoxGroup<>();
    private TextArea queryTextArea;
    private TextField limitTextField;
    private TextArea resultsTextArea;
    private Collection<SearchResult> results;
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
        HorizontalLayout footerLayout = buildFooterLayout();
        VerticalLayout outerLayout = buildOuterLayout(headerLayout, mainLayout, footerLayout);
        setContent(outerLayout);
        String implementationVersion = getClass().getPackage().getImplementationVersion();
        implementationVersion = implementationVersion == null ? "dev" : implementationVersion;
        Page currentPage = Page.getCurrent();
        currentPage.setTitle("TrackFind: " + implementationVersion);
//        GoogleAnalyticsTracker tracker = new GoogleAnalyticsTracker("UA-143208550-1");
//        tracker.trackPageview("/");
//        tracker.extend(getUI());
    }

    protected VerticalLayout buildTreeLayout() {
        tabSheet = new TabSheet();
        tabSheet.setSizeFull();

        for (TfHub hub : trackFindService.getTrackHubs(true)) {
            TrackFindTree<TreeNode> tree = buildTree(hub);
            tabSheet.addTab(tree, hub.getName());
        }

        tabSheet.addSelectedTabChangeListener((TabSheet.SelectedTabChangeListener) event -> refreshCategoriesCheckList());

        Panel treePanel = new Panel("Model browser", tabSheet);
        treePanel.setSizeFull();

//        TextField attributesFilterTextField = createFilter(true);
        TextField valuesFilterTextField = createFilter(false);

        AddToQueryButtonClickListener addToQueryButtonClickListener = new AddToQueryButtonClickListener(this, separator);
        Button addToQueryButton = new Button("Add to query ➚", addToQueryButtonClickListener);
        addToQueryButton.setWidth(100, Unit.PERCENTAGE);

        VerticalLayout treeLayout = new VerticalLayout(treePanel, valuesFilterTextField, addToQueryButton);
        treeLayout.setSizeFull();
        treeLayout.setExpandRatio(treePanel, 1f);
        return treeLayout;
    }

    @SuppressWarnings("unchecked")
    protected TrackFindTree<TreeNode> buildTree(TfHub hub) {
        TrackFindTree<TreeNode> tree = new TrackFindTree<>(hub);
        tree.setDataProvider(trackFindDataProvider);
        tree.setSelectionMode(Grid.SelectionMode.MULTI);
        tree.addItemClickListener(new TreeItemClickListener(tree));
        TreeGrid<TreeNode> treeGrid = (TreeGrid<TreeNode>) tree.getCompositionRoot();
        TreeFilter filter = new TreeFilter(hub, "", "");
        treeGrid.setFilter(filter);
        tree.addSelectionListener(new TreeSelectionListener(tree, filter, new KeyboardInterceptorExtension(tree)));
        tree.setSizeFull();
        tree.setStyleGenerator((StyleGenerator<TreeNode>) item -> item.isAttribute() ? null : "value-tree-node");

        TreeGridDragSource<TreeNode> dragSource = new TreeGridDragSource<>(treeGrid);
        dragSource.setEffectAllowed(EffectAllowed.COPY);

        return tree;
    }

    private HorizontalLayout buildMainLayout(VerticalLayout treeLayout, VerticalLayout queryLayout, VerticalLayout resultsLayout) {
        MarginInfo marginInfo = new MarginInfo(true, true, false, true);
        treeLayout.setMargin(marginInfo);
        resultsLayout.setMargin(marginInfo);
        HorizontalLayout mainLayout = new HorizontalLayout(treeLayout, queryLayout, resultsLayout);
        mainLayout.setSizeFull();
        return mainLayout;
    }

    private VerticalLayout buildResultsLayout() {
        Button exportGSuiteButton = new Button("Export as GSuite file");
        exportGSuiteButton.setEnabled(false);
        exportGSuiteButton.setWidth(100, Unit.PERCENTAGE);
        Button exportJSONButton = new Button("Export as JSON file");
        exportJSONButton.setEnabled(false);
        exportJSONButton.setWidth(100, Unit.PERCENTAGE);
        gSuiteFileDownloader = new FileDownloader(new ExternalResource(""));
        gSuiteFileDownloader.extend(exportGSuiteButton);
        jsonFileDownloader = new FileDownloader(new ExternalResource(""));
        jsonFileDownloader.extend(exportJSONButton);
        resultsTextArea = new TextArea();
        resultsTextArea.setSizeFull();
        resultsTextArea.setReadOnly(true);
        resultsTextArea.addStyleName("scrollable-text-area");
        resultsTextArea.addValueChangeListener((HasValue.ValueChangeListener<String>) event -> {
            if (StringUtils.isEmpty(event.getValue())) {
                exportGSuiteButton.setEnabled(false);
                exportGSuiteButton.setCaption("Export as GSuite file");
                exportJSONButton.setEnabled(false);
                exportJSONButton.setCaption("Export as JSON file");
            } else {
                exportGSuiteButton.setEnabled(true);
                exportGSuiteButton.setCaption("Export (" + numberOfResults + ") entries as GSuite file");
                exportJSONButton.setEnabled(true);
                exportJSONButton.setCaption("Export (" + numberOfResults + ") entries as JSON file");
            }
        });
        Panel resultsPanel = new Panel("Data", resultsTextArea);
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

        Button clearAllButton = new Button("Clear all");
        clearAllButton.setSizeFull();
        clearAllButton.addClickListener((Button.ClickListener) event -> queryTextArea.clear());

        Button searchButton = new Button("Search ➚", (Button.ClickListener) clickEvent -> executeQuery(queryTextArea.getValue()));
        queryTextArea.addValueChangeListener((HasValue.ValueChangeListener<String>) event -> searchButton.setEnabled(StringUtils.isNotEmpty(queryTextArea.getValue())));
        queryTextArea.addStyleName("scrollable-text-area");
        DropTargetExtension<TextArea> dropTarget = new DropTargetExtension<>(queryTextArea);
        dropTarget.setDropEffect(DropEffect.COPY);
        TextAreaDropListener textAreaDropListener = new TextAreaDropListener(queryTextArea, separator);
        dropTarget.addDropListener(textAreaDropListener);
        Panel queryPanel = new Panel("Search query", queryTextArea);
        queryPanel.setSizeFull();

        searchButton.setWidth(100, Unit.PERCENTAGE);
        searchButton.setEnabled(false);

        limitTextField = new TextField("Limit", "10");
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

        Panel categoriesChecklistPanel = new Panel("Categories", new VerticalLayout(categoriesChecklist));
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
        try {
            results = searchService.search(hub.getRepository(), hub.getName(), query, categoriesChecklist.getSelectedItems(), Long.parseLong(limit)).getValue();
        } catch (SQLException e) {
            results = Collections.emptyList();
            log.error(e.getMessage(), e);
        }
        if (results.isEmpty()) {
            resultsTextArea.setValue("");
            Notification.show("Nothing found for such request");
            return;
        }
        numberOfResults = results.size();
        try {
            jsonResult = mapper.writeValueAsString(results);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        resultsTextArea.setValue(jsonResult);

        gSuiteFileDownloader.setFileDownloadResource(getStreamResource(null, ".gsuite"));
        jsonFileDownloader.setFileDownloadResource(getStreamResource(jsonResult, ".json"));
    }

    private Resource getStreamResource(String content, String extension) {
        return new StreamResource(null, null) {
            @Override
            public StreamSource getStreamSource() {
                if (".json".equalsIgnoreCase(extension)) {
                    return (StreamResource.StreamSource) () -> new ByteArrayInputStream(content.getBytes(Charset.defaultCharset()));
                } else {
                    String gSuiteResult = gSuiteService.apply(results);
                    return (StreamResource.StreamSource) () -> new ByteArrayInputStream(gSuiteResult.getBytes(Charset.defaultCharset()));
                }
            }

            @Override
            public String getFilename() {
                return Calendar.getInstance().getTime().toString() + extension;
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
