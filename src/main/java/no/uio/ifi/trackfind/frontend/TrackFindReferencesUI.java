package no.uio.ifi.trackfind.frontend;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.HasValue;
import com.vaadin.data.provider.AbstractBackEndDataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import com.vaadin.ui.renderers.ButtonRenderer;
import com.vaadin.ui.renderers.ClickableRenderer;
import elemental.json.JsonValue;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.annotations.DoInTransaction;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import no.uio.ifi.trackfind.backend.pojo.TfObjectType;
import no.uio.ifi.trackfind.backend.pojo.TfReference;
import no.uio.ifi.trackfind.backend.services.MetamodelService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.Stream;

/**
 * References Vaadin UI of the application.
 * Uses custom theme (VAADIN/themes/trackfind/trackfind.scss).
 * Uses custom WidgetSet (TrackFindWidgetSet.gwt.xml).
 *
 * @author Dmytro Titov
 */
@SpringUI(path = "/references")
@Widgetset("TrackFindWidgetSet")
@Title("References")
@Theme("trackfind")
@Slf4j
public class TrackFindReferencesUI extends AbstractUI {

    private MetamodelService metamodelService;

    @DoInTransaction
    @Override
    protected void init(VaadinRequest vaadinRequest) {
        HorizontalLayout headerLayout = buildHeaderLayout();
        VerticalLayout referencesLayout = buildReferencesLayout();
        HorizontalLayout mainLayout = buildMainLayout(referencesLayout);
        HorizontalLayout footerLayout = buildFooterLayout();
        VerticalLayout outerLayout = buildOuterLayout(headerLayout, mainLayout, footerLayout);
        setContent(outerLayout);
    }

    @SuppressWarnings("unchecked")
    private VerticalLayout buildReferencesLayout() {
        VerticalLayout referencesLayout = new VerticalLayout();
        referencesLayout.setSizeFull();
        TabSheet referencesTabSheet = new TabSheet();
        referencesTabSheet.setSizeFull();
        Panel referencesPanel = new Panel("References", referencesTabSheet);
        referencesPanel.setSizeFull();
        for (TfHub hub : trackFindService.getTrackHubs(true)) {
            Grid<TfReference> grid = new Grid<>(TfReference.class);
            grid.setSizeFull();
            grid.addColumn(r -> r.getFromObjectType().getName()).setCaption("From category").setId("0");
            grid.addColumn(TfReference::getFromAttribute).setCaption("From attribute").setId("1");
            grid.addColumn(r -> r.getToObjectType().getName()).setCaption("To category").setId("2");
            grid.addColumn(TfReference::getToAttribute).setCaption("To attribute").setId("3");
            grid.getColumn("fromObjectType").setHidden(true);
            grid.getColumn("fromAttribute").setHidden(true);
            grid.getColumn("toObjectType").setHidden(true);
            grid.getColumn("toAttribute").setHidden(true);
            ButtonRenderer buttonRenderer = new ButtonRenderer((ClickableRenderer.RendererClickListener<TfReference>) event -> {
                metamodelService.deleteReference(event.getItem());
                grid.setItems(metamodelService.getReferences(hub.getRepository(), hub.getName()));
            }) {
                @Override
                public JsonValue encode(Object value) {
                    return super.encode("Delete");
                }
            };
            grid.getColumn("id").setRenderer(buttonRenderer).setCaption("Operations");
            grid.setColumnOrder("0", "1", "2", "3", "id");
            grid.setData(hub);
            grid.setItems(metamodelService.getReferences(hub.getRepository(), hub.getName()));
            referencesTabSheet.addTab(grid, hub.getRepository() + ": " + hub.getName()).getComponent().setSizeFull();
        }
        ComboBox<TfObjectType> fromCategoryComboBox = new ComboBox<>("From category");
        fromCategoryComboBox.setItemCaptionGenerator(TfObjectType::getName);
        AbstractBackEndDataProvider<TfObjectType, String> dataProvider = new AbstractBackEndDataProvider<TfObjectType, String>() {
            @Override
            protected Stream<TfObjectType> fetchFromBackEnd(Query<TfObjectType, String> query) {
                Grid selectedTab = (Grid) referencesTabSheet.getSelectedTab();
                TfHub hub = (TfHub) selectedTab.getData();
                return hub.getCurrentVersion().orElseThrow(RuntimeException::new).getObjectTypes().stream();
            }

            @Override
            protected int sizeInBackEnd(Query<TfObjectType, String> query) {
                return (int) fetchFromBackEnd(query).count();
            }
        };
        fromCategoryComboBox.setDataProvider(dataProvider);
        TextField fromAttributeTextField = new TextField("From attribute");
        ComboBox<TfObjectType> toCategoryComboBox = new ComboBox<>("To category");
        toCategoryComboBox.setItemCaptionGenerator(TfObjectType::getName);
        toCategoryComboBox.setDataProvider(dataProvider);
        TextField toAttributeTextField = new TextField("To attribute");
        Button addButton = new Button("Add", (Button.ClickListener) event -> {
            TfReference reference = new TfReference(
                    null,
                    fromCategoryComboBox.getSelectedItem().orElseThrow(RuntimeException::new),
                    fromAttributeTextField.getValue(),
                    toCategoryComboBox.getSelectedItem().orElseThrow(RuntimeException::new),
                    toAttributeTextField.getValue());
            metamodelService.addReference(reference);
            Grid selectedTab = (Grid) referencesTabSheet.getSelectedTab();
            TfHub hub = (TfHub) selectedTab.getData();
            selectedTab.setItems(metamodelService.getReferences(hub.getRepository(), hub.getName()));
        });
        addButton.setEnabled(false);
        fromCategoryComboBox.addValueChangeListener((HasValue.ValueChangeListener<TfObjectType>) event -> {
            changeAddButtonState(fromCategoryComboBox, fromAttributeTextField, toCategoryComboBox, toAttributeTextField, addButton);
        });
        fromAttributeTextField.addValueChangeListener((HasValue.ValueChangeListener<String>) event -> {
            changeAddButtonState(fromCategoryComboBox, fromAttributeTextField, toCategoryComboBox, toAttributeTextField, addButton);
        });
        toCategoryComboBox.addValueChangeListener((HasValue.ValueChangeListener<TfObjectType>) event -> {
            changeAddButtonState(fromCategoryComboBox, fromAttributeTextField, toCategoryComboBox, toAttributeTextField, addButton);
        });
        toAttributeTextField.addValueChangeListener((HasValue.ValueChangeListener<String>) event -> {
            changeAddButtonState(fromCategoryComboBox, fromAttributeTextField, toCategoryComboBox, toAttributeTextField, addButton);
        });
        HorizontalLayout controlsLayout = new HorizontalLayout(fromCategoryComboBox, fromAttributeTextField, toCategoryComboBox, toAttributeTextField, addButton);
        controlsLayout.setComponentAlignment(addButton, Alignment.BOTTOM_RIGHT);
        referencesLayout.addComponents(referencesPanel, controlsLayout);
        referencesLayout.setExpandRatio(referencesPanel, 1f);
        return referencesLayout;
    }

    private void changeAddButtonState(ComboBox<TfObjectType> fromCategoryComboBox, TextField fromAttributeTextField, ComboBox<TfObjectType> toCategoryComboBox, TextField toAttributeTextField, Button addButton) {
        addButton.setEnabled(fromCategoryComboBox.getSelectedItem().isPresent()
                && fromAttributeTextField.getOptionalValue().isPresent()
                && toCategoryComboBox.getSelectedItem().isPresent()
                && toAttributeTextField.getOptionalValue().isPresent());
    }

    private HorizontalLayout buildMainLayout(VerticalLayout leftLayout) {
        HorizontalLayout mainLayout = new HorizontalLayout(leftLayout);
        mainLayout.setExpandRatio(leftLayout, 0.66f);
        mainLayout.setSizeFull();
        return mainLayout;
    }

    @Autowired
    public void setMetamodelService(MetamodelService metamodelService) {
        this.metamodelService = metamodelService;
    }

}
