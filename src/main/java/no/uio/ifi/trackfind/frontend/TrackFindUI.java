package no.uio.ifi.trackfind.frontend;

import com.google.gson.Gson;
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
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.backend.services.TrackFindService;
import no.uio.ifi.trackfind.frontend.components.KeyboardInterceptorExtension;
import no.uio.ifi.trackfind.frontend.components.TrackFindTree;
import no.uio.ifi.trackfind.frontend.data.TreeNode;
import no.uio.ifi.trackfind.frontend.listeners.TextAreaDropListener;
import no.uio.ifi.trackfind.frontend.listeners.TreeItemClickListener;
import no.uio.ifi.trackfind.frontend.listeners.TreeSelectionListener;
import no.uio.ifi.trackfind.frontend.providers.TrackDataProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;

/**
 * Main Vaadin UI of the application.
 * Capable of displaying metadata for repositories, constructing and executing search queries along with exporting the results.
 * Uses custom theme (VAADIN/themes/trackfind/trackfind.scss).
 * Uses custom WidgetSet (TrackFindWidgetSet.gwt.xml).
 *
 * @author Dmytro Titov
 */
@SpringUI
@Widgetset("TrackFindWidgetSet")
@Title("TrackFind")
@Theme("trackfind")
@Slf4j
public class TrackFindUI extends UI {

    private TrackFindService trackFindService;
    private Gson gson;

    private Collection<Map<String, Object>> lastResults;

    private TabSheet tabSheet;
    private TextArea queryTextArea;
    private TextField limitTextField;
    private TextArea resultsTextArea;
    private FileDownloader fileDownloader;
    private Button exportButton;

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

    private VerticalLayout buildOuterLayout(HorizontalLayout headerLayout, HorizontalLayout mainLayout, HorizontalLayout footerLayout) {
        VerticalLayout outerLayout = new VerticalLayout(headerLayout, mainLayout, footerLayout);
        outerLayout.setSizeFull();
        outerLayout.setExpandRatio(headerLayout, 0.05f);
        outerLayout.setExpandRatio(mainLayout, 0.9f);
        outerLayout.setExpandRatio(footerLayout, 0.05f);
        return outerLayout;
    }

    private HorizontalLayout buildMainLayout(VerticalLayout treeLayout, VerticalLayout queryLayout, VerticalLayout resultsLayout) {
        HorizontalLayout mainLayout = new HorizontalLayout(treeLayout, queryLayout, resultsLayout);
        mainLayout.setSizeFull();
        return mainLayout;
    }

    private VerticalLayout buildResultsLayout() {
        resultsTextArea = new TextArea();
        resultsTextArea.setSizeFull();
        resultsTextArea.setReadOnly(true);
        resultsTextArea.addStyleName("scrollable-text-area");
        resultsTextArea.addValueChangeListener((HasValue.ValueChangeListener<String>) event -> {
            if (StringUtils.isEmpty(event.getValue())) {
                exportButton.setEnabled(false);
                exportButton.setCaption("Export as GSuite file");
            } else {
                exportButton.setEnabled(true);
                exportButton.setCaption("Export (" + lastResults.size() + ") entries as GSuite file");
            }
        });
        Panel resultsPanel = new Panel("Data", resultsTextArea);
        resultsPanel.setSizeFull();
        exportButton = new Button("Export as GSuite file");
        exportButton.setEnabled(false);
        VerticalLayout resultsLayout = new VerticalLayout(resultsPanel, exportButton);
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
        DropTargetExtension<TextArea> dropTarget = new DropTargetExtension<>(queryTextArea);
        dropTarget.setDropEffect(DropEffect.COPY);
        dropTarget.addDropListener(new TextAreaDropListener(queryTextArea));
        Panel queryPanel = new Panel("Search query", queryTextArea);
        queryPanel.setSizeFull();

        Button searchButton = new Button("Search", (Button.ClickListener) clickEvent -> executeQuery(queryTextArea.getValue()));
        searchButton.setWidth(100, Unit.PERCENTAGE);

        limitTextField = new TextField("Limit");
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
        instructions.add(new Label("3. Drag and drop attribute name or value to the query area."));
        instructions.add(new Label("4. Correct query manually if necessary (some special characters should be escaped using backslash)."));
        instructions.add(new Label("5. Press <i>Ctrl+Shift</i> or <i>Command+Shift</i> or click <i>Search</i> button to execute the query.", ContentMode.HTML));
        instructions.add(new Label("<b>Hotkeys:<b> ", ContentMode.HTML));
        instructions.add(new Label("Use <i>Ctrl</i> or <i>Command</i> to select multiple values in tree.", ContentMode.HTML));
        instructions.add(new Label("Use <i>Shift</i> to select range of values in tree.", ContentMode.HTML));
        instructions.add(new Label("Hold <i>Alt</i> or <i>Option</i> key while dragging to use OR operator instead of AND.", ContentMode.HTML));
        instructions.add(new Label("Hold <i>Shift</i> key while dragging to add NOT operator.", ContentMode.HTML));

        VerticalLayout helpLayout = new VerticalLayout();
        instructions.forEach(helpLayout::addComponent);

        TextField sampleQueryTextField = new TextField("Sample query", "sample_id: SRS306625_*_471 OR other_attributes>lab: U??D AND ihec_data_portal>assay: (WGB-Seq OR something)");
        sampleQueryTextField.setEnabled(false);
        sampleQueryTextField.setWidth(100, Unit.PERCENTAGE);
        helpLayout.addComponent(sampleQueryTextField);
        return helpLayout;
    }

    private void executeQuery(String query) {
        DataProvider currentDataProvider = getCurrentDataProvider();

        String limit = limitTextField.getValue();
        limit = StringUtils.isEmpty(limit) ? "0" : limit;
        lastResults = currentDataProvider.search(query, Integer.parseInt(limit));
        String jsonResult = gson.toJson(lastResults);
        if (CollectionUtils.isEmpty(lastResults)) {
            resultsTextArea.setValue("");
            Notification.show("Nothing found for such request");
        } else {
            resultsTextArea.setValue(jsonResult);
        }

        StringBuilder result = new StringBuilder("###uri\trepository");
        for (Map lastResult : lastResults) {
            for (String url : currentDataProvider.getUrlsFromDataset(query, lastResult)) {
                result.append("\n").append(url).append("\t").append(currentDataProvider.getName());
            }
        }

        if (fileDownloader != null) {
            exportButton.removeExtension(fileDownloader);
        }
        String finalResult = result.toString();
        Resource resource = new StreamResource((StreamResource.StreamSource) () -> new ByteArrayInputStream(finalResult.getBytes(Charset.defaultCharset())),
                Calendar.getInstance().getTime().toString() + ".gsuite");
        fileDownloader = new FileDownloader(resource);
        fileDownloader.extend(exportButton);
    }

    @SuppressWarnings("unchecked")
    private DataProvider getCurrentDataProvider() {
        TrackFindTree<TreeNode> tree = (TrackFindTree<TreeNode>) tabSheet.getSelectedTab();
        return tree.getTrackDataProvider();
    }

    @SuppressWarnings("unchecked")
    private VerticalLayout buildTreeLayout() {
        tabSheet = new TabSheet();
        tabSheet.setSizeFull();

        for (DataProvider dataProvider : trackFindService.getDataProviders()) {
            TrackFindTree<TreeNode> tree = buildTree(dataProvider);
            tabSheet.addTab(tree, dataProvider.getName());
        }

        Panel treePanel = new Panel("Model browser", tabSheet);
        treePanel.setSizeFull();

        TextField attributesFilterTextField = new TextField("Filter attributes", (HasValue.ValueChangeListener<String>) event -> {
            TrackFindTree<TreeNode> tree = (TrackFindTree<TreeNode>) tabSheet.getSelectedTab();
            TrackDataProvider dataProvider = (TrackDataProvider) tree.getDataProvider();
            dataProvider.setAttributesFilter(event.getValue());
            dataProvider.refreshAll();
        });
        attributesFilterTextField.setValueChangeMode(ValueChangeMode.EAGER);
        attributesFilterTextField.setWidth(100, Unit.PERCENTAGE);

        TextField valuesFilterTextField = new TextField("Filter values", (HasValue.ValueChangeListener<String>) event -> {
            TrackFindTree<TreeNode> tree = (TrackFindTree<TreeNode>) tabSheet.getSelectedTab();
            TrackDataProvider dataProvider = (TrackDataProvider) tree.getDataProvider();
            dataProvider.setValuesFilter(event.getValue());
            dataProvider.refreshAll();
        });
        valuesFilterTextField.setValueChangeMode(ValueChangeMode.EAGER);
        valuesFilterTextField.setWidth(100, Unit.PERCENTAGE);

        VerticalLayout treeLayout = new VerticalLayout(treePanel, attributesFilterTextField, valuesFilterTextField);
        treeLayout.setSizeFull();
        treeLayout.setExpandRatio(treePanel, 1f);
        return treeLayout;
    }

    @SuppressWarnings("unchecked")
    private TrackFindTree<TreeNode> buildTree(DataProvider dataProvider) {
        TrackFindTree<TreeNode> tree = new TrackFindTree<>(dataProvider);
        tree.setSelectionMode(Grid.SelectionMode.MULTI);
        tree.addItemClickListener(new TreeItemClickListener(tree));
        tree.addSelectionListener(new TreeSelectionListener(tree, new KeyboardInterceptorExtension(tree)));
        TreeGridDragSource<TreeNode> dragSource = new TreeGridDragSource<>((TreeGrid<TreeNode>) tree.getCompositionRoot());
        dragSource.setEffectAllowed(EffectAllowed.COPY);
        TrackDataProvider trackDataProvider = new TrackDataProvider(new TreeNode(dataProvider.getMetamodelTree()));
        tree.setDataProvider(trackDataProvider);
        tree.setSizeFull();
        tree.setStyleGenerator((StyleGenerator<TreeNode>) item -> item.isFinalAttribute() || item.isLeaf() ? null : "disabled-tree-node");
        return tree;
    }

    private HorizontalLayout buildFooterLayout() {
        Label footerLabel = new Label("2017");
        HorizontalLayout footerLayout = new HorizontalLayout(footerLabel);
        footerLayout.setSizeFull();
        footerLayout.setComponentAlignment(footerLabel, Alignment.BOTTOM_CENTER);
        return footerLayout;
    }

    private HorizontalLayout buildHeaderLayout() {
        Label headerLabel = new Label("TrackFind");
        HorizontalLayout headerLayout = new HorizontalLayout(headerLabel);
        headerLayout.setSizeFull();
        headerLayout.setComponentAlignment(headerLabel, Alignment.TOP_CENTER);
        return headerLayout;
    }

    @Autowired
    public void setTrackFindService(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

    @Autowired
    public void setGson(Gson gson) {
        this.gson = gson;
    }

}
