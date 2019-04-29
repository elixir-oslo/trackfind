package no.uio.ifi.trackfind.frontend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.HasValue;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.*;
import com.vaadin.shared.ui.ContentMode;
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
import no.uio.ifi.trackfind.backend.services.GSuiteService;
import no.uio.ifi.trackfind.backend.services.SearchService;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

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
    private GSuiteService gSuiteService;
    private SearchService searchService;

    private int numberOfResults;

    private TextAreaDropListener textAreaDropListener;
    private AddToQueryButtonClickListener addToQueryButtonClickListener;
    private TextArea queryTextArea;
    private TextField limitTextField;
    private TextArea resultsTextArea;
    private Collection<SearchResult> results;
    private String jsonResult;
    private String gSuiteResult;

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
    }

    protected VerticalLayout buildTreeLayout() {
        tabSheet = new TabSheet();
        tabSheet.setSizeFull();

        for (TfHub hub : trackFindService.getTrackHubs(true)) {
            TrackFindTree<TreeNode> tree = buildTree(hub);
            tabSheet.addTab(tree, hub.getName());
        }

        Panel treePanel = new Panel("Model browser", tabSheet);
        treePanel.setSizeFull();

//        CheckBox checkBox = new CheckBox("Raw metamodel");
//        checkBox.addValueChangeListener((HasValue.ValueChangeListener<Boolean>) event -> {
//            textAreaDropListener.setDatasetPrefix(event.getValue() ? "curated_content" : "standard_content");
//            addToQueryButtonClickListener.setDatasetPrefix(event.getValue() ? "curated_content" : "standard_content");
//            refreshTrees(event.getValue());
//        });

        TextField attributesFilterTextField = createFilter(true);
        TextField valuesFilterTextField = createFilter(false);

        addToQueryButtonClickListener = new AddToQueryButtonClickListener(this, properties.getLevelsSeparator());
        addToQueryButtonClickListener.setDatasetPrefix("fair_content");
        Button addToQueryButton = new Button("Add to query ➚", addToQueryButtonClickListener);
        addToQueryButton.setWidth(100, Unit.PERCENTAGE);

        VerticalLayout treeLayout = new VerticalLayout(treePanel, attributesFilterTextField, valuesFilterTextField, addToQueryButton);
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
        HorizontalLayout mainLayout = new HorizontalLayout(treeLayout, queryLayout, resultsLayout);
        mainLayout.setSizeFull();
        return mainLayout;
    }

    private VerticalLayout buildResultsLayout() {
        Button exportGSuiteButton = new Button("Export as GSuite file", (Button.ClickListener) event -> gSuiteResult = gSuiteService.apply(results));
        exportGSuiteButton.setEnabled(false);
        exportGSuiteButton.setWidth(100, Unit.PERCENTAGE);
        Button exportJSONButton = new Button("Export as JSON file");
        exportJSONButton.setEnabled(false);
        exportJSONButton.setWidth(100, Unit.PERCENTAGE);
        Resource gSuiteResource = new StreamResource(null, null) {
            @Override
            public StreamSource getStreamSource() {
                return (StreamResource.StreamSource) () -> new ByteArrayInputStream(gSuiteResult.getBytes(Charset.defaultCharset()));
            }

            @Override
            public String getFilename() {
                return Calendar.getInstance().getTime().toString() + ".gsuite";
            }

            @Override
            public String getMIMEType() {
                return FileTypeResolver.getMIMEType(getFilename());
            }
        };
        FileDownloader gSuiteFileDownloader = new FileDownloader(gSuiteResource);
        gSuiteFileDownloader.extend(exportGSuiteButton);
        Resource jsonResource = new StreamResource(null, null) {
            @Override
            public StreamSource getStreamSource() {
                return (StreamResource.StreamSource) () -> new ByteArrayInputStream(jsonResult.getBytes(Charset.defaultCharset()));
            }

            @Override
            public String getFilename() {
                return Calendar.getInstance().getTime().toString() + ".json";
            }

            @Override
            public String getMIMEType() {
                return FileTypeResolver.getMIMEType(getFilename());
            }
        };
        FileDownloader jsonFileDownloader = new FileDownloader(jsonResource);
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

    @SuppressWarnings("ResultOfMethodCallIgnored")
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
        Button searchButton = new Button("Search ➚", (Button.ClickListener) clickEvent -> executeQuery(queryTextArea.getValue()));
        queryTextArea.addValueChangeListener((HasValue.ValueChangeListener<String>) event -> searchButton.setEnabled(StringUtils.isNotEmpty(queryTextArea.getValue())));
        queryTextArea.addStyleName("scrollable-text-area");
        DropTargetExtension<TextArea> dropTarget = new DropTargetExtension<>(queryTextArea);
        dropTarget.setDropEffect(DropEffect.COPY);
        textAreaDropListener = new TextAreaDropListener(queryTextArea, properties.getLevelsSeparator());
        textAreaDropListener.setDatasetPrefix("fair_content");
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

        VerticalLayout helpLayout = buildHelpLayout();

        PopupView popup = new PopupView("Help", helpLayout);
        VerticalLayout queryLayout = new VerticalLayout(queryPanel, limitTextField, searchButton, popup);
        queryLayout.setSizeFull();
        queryLayout.setExpandRatio(queryPanel, 1f);
        return queryLayout;
    }

    private VerticalLayout buildHelpLayout() {
        Collection<Component> instructions = new ArrayList<>();
        instructions.add(new Label("<b>How to perform a search:<b> ", ContentMode.HTML));
        instructions.add(new Label("1. Navigate through metamodel tree using browser on the left."));
        instructions.add(new Label("2. Filter attributes or values using text-fields in the bottom if needed."));
        instructions.add(new Label("3. Select attribute or value(s) you want to use in the search query. Multiple values can be selected using <i>Shift</i> or <i>Ctrl</i> / <i>Command</i>.", ContentMode.HTML));
        instructions.add(new Label("4. Drag and drop attribute name or value to the query area or simply press <i>Add to query</i> button."));
        instructions.add(new Label("5. Correct query manually if necessary."));
        instructions.add(new Label("6. Press <i>Ctrl+Shift</i> or <i>Command+Shift</i> or click <i>Search</i> button to execute the query.", ContentMode.HTML));
        instructions.add(new Label("<b>Hotkeys:<b> ", ContentMode.HTML));
        instructions.add(new Label("Use <i>Ctrl</i> or <i>Command</i> to select multiple values in tree.", ContentMode.HTML));
        instructions.add(new Label("Use <i>Shift</i> to select range of values in tree.", ContentMode.HTML));
        instructions.add(new Label("Hold <i>Alt</i> or <i>Option</i> key while dragging to use OR operator instead of AND.", ContentMode.HTML));
        instructions.add(new Label("Hold <i>Shift</i> key while dragging to add NOT operator.", ContentMode.HTML));
        return new VerticalLayout(instructions.toArray(new Component[]{}));
    }

    @SuppressWarnings("unchecked")
    private void executeQuery(String query) {
        TfHub hub = getCurrentHub();
        String limit = limitTextField.getValue();
        limit = StringUtils.isEmpty(limit) ? "0" : limit;
        results = searchService.search(hub.getRepository(), hub.getName(), query, Integer.parseInt(limit));
        if (results.isEmpty()) {
            resultsTextArea.setValue("");
            Notification.show("Nothing found for such request");
            return;
        }
        numberOfResults = results.size();
        try {
            jsonResult = mapper.writeValueAsString(results);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
        resultsTextArea.setValue(jsonResult);
    }

    public TextArea getQueryTextArea() {
        return queryTextArea;
    }

    @Autowired
    public void setMapper(ObjectMapper mapper) {
        this.mapper = mapper;
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
