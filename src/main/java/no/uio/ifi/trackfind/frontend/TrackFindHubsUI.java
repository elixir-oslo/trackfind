package no.uio.ifi.trackfind.frontend;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.HasValue;
import com.vaadin.data.provider.AbstractBackEndDataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.event.selection.MultiSelectionListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import no.uio.ifi.trackfind.backend.pojo.TfVersion;
import no.uio.ifi.trackfind.backend.repositories.HubRepository;
import no.uio.ifi.trackfind.backend.services.ValidationService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.dialogs.ConfirmDialog;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Hubs Vaadin UI of the application.
 * Uses custom theme (VAADIN/themes/trackfind/trackfind.scss).
 * Uses custom WidgetSet (TrackFindWidgetSet.gwt.xml).
 *
 * @author Dmytro Titov
 */
@SpringUI(path = "/hubs")
@Widgetset("TrackFindWidgetSet")
@Title("Hubs")
@Theme("trackfind")
@Slf4j
public class TrackFindHubsUI extends AbstractUI {

    private HubRepository hubRepository;
    private ValidationService validationService;

    private ComboBox<TfHub> comboBox = new ComboBox<>("Available hubs");
    private ListSelect<TfHub> listSelect = new ListSelect<>();
    private TextField displayNameTextField = new TextField("Hub display name (optional)");
    private Button saveDisplayNameButton = new Button("Save");
    private Button add = new Button("Activate →");
    private Button remove = new Button("Deactivate ←");
    private Button crawl = new Button("Crawl");
    private Button validate = new Button("Validate");

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        HorizontalLayout headerLayout = buildHeaderLayout();
        VerticalLayout availableHubsLayout = buildAvailableHubsLayout();
        VerticalLayout activeHubsLayout = buildActiveHubsLayout();
        HorizontalLayout mainLayout = buildMainLayout(availableHubsLayout, buildButtonsLayout(), activeHubsLayout);
        HorizontalLayout footerLayout = buildFooterLayout();
        VerticalLayout outerLayout = buildOuterLayout(headerLayout, mainLayout, footerLayout);
        setContent(outerLayout);
    }

    private VerticalLayout buildAvailableHubsLayout() {
        VerticalLayout hubsLayout = new VerticalLayout();
        hubsLayout.setSizeFull();
        comboBox.setWidth(100, Unit.PERCENTAGE);
        comboBox.setDataProvider(new AbstractBackEndDataProvider<TfHub, String>() {
            @Override
            protected Stream<TfHub> fetchFromBackEnd(Query<TfHub, String> query) {
                return query
                        .getFilter()
                        .map(s -> trackFindService.getAvailableTrackHubs().stream().filter(th -> th.getName().toLowerCase().contains(s.toLowerCase())))
                        .orElseGet(() -> trackFindService.getAvailableTrackHubs().stream());
            }

            @Override
            protected int sizeInBackEnd(Query<TfHub, String> query) {
                return (int) fetchFromBackEnd(query).count();
            }
        });
        comboBox.setItemCaptionGenerator(h -> h.getRepository() + ": " + h.getName());
        comboBox.addValueChangeListener((HasValue.ValueChangeListener<TfHub>) event -> add.setEnabled(comboBox.getSelectedItem().isPresent()));
        Panel panel = new Panel("Hub selection", comboBox);
        panel.setSizeFull();
        hubsLayout.addComponents(panel);
        hubsLayout.setExpandRatio(panel, 1f);
        return hubsLayout;
    }

    private VerticalLayout buildButtonsLayout() {
        VerticalLayout verticalLayout = new VerticalLayout();
        add.setEnabled(false);
        add.setWidth(100, Unit.PERCENTAGE);
        add.addClickListener((Button.ClickListener) event -> comboBox.getSelectedItem().ifPresent(hub -> {
            trackFindService.activateHubs(Collections.singleton(hub));
            listSelect.getDataProvider().refreshAll();
            Set<TfHub> availableHubs = comboBox.getDataProvider().fetch(new Query<>()).collect(Collectors.toSet());
            availableHubs.remove(hub);
            comboBox.clear();
            comboBox.getDataProvider().refreshAll();
        }));
        remove.setEnabled(false);
        remove.setWidth(100, Unit.PERCENTAGE);
        remove.addClickListener((Button.ClickListener) event -> {
            Set<TfHub> activeHubs = listSelect.getSelectedItems();
            try {
                trackFindService.deactivateHubs(activeHubs);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                Notification.show("Error: " + e.getMessage(), Notification.Type.ERROR_MESSAGE);
            }
            listSelect.getDataProvider().refreshAll();
            Set<TfHub> availableHubs = comboBox.getDataProvider().fetch(new Query<>()).collect(Collectors.toSet());
            availableHubs.addAll(activeHubs);
            comboBox.getDataProvider().refreshAll();
        });
        crawl.setEnabled(false);
        crawl.setWidth(100, Unit.PERCENTAGE);
        UI ui = getUI();
        crawl.addClickListener((Button.ClickListener) event -> ConfirmDialog.show(ui,
                "Are you sure? " +
                        "Crawling is time-consuming process and will lead to changing the data in the database.",
                (ConfirmDialog.Listener) dialog -> {
                    if (dialog.isConfirmed()) {
                        try {
                            Set<TfHub> activeHubs = listSelect.getSelectedItems();
                            for (TfHub hub : activeHubs) {
                                DataProvider dataProvider = trackFindService.getDataProvider(hub.getRepository());
                                dataProvider.crawlRemoteRepository(hub.getName());
                            }
                            Notification.show("Success!");
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                            Notification.show("Error: " + e.getMessage(), Notification.Type.ERROR_MESSAGE);
                        }
                        listSelect.getDataProvider().refreshAll();
                    }
                }));
        validate.setEnabled(false);
        validate.setWidth(100, Unit.PERCENTAGE);
        validate.addClickListener((Button.ClickListener) event -> ConfirmDialog.show(ui,
                "Are you sure? " +
                        "Validation is time-consuming process.",
                (ConfirmDialog.Listener) dialog -> {
                    if (dialog.isConfirmed()) {
                        try {
                            Set<TfHub> activeHubs = listSelect.getSelectedItems();
                            for (TfHub hub : activeHubs) {
                                String result = validationService.validate(hub.getRepository(), hub.getName());
                                ConfirmDialog.show(ui, result, (ConfirmDialog.Listener) d -> listSelect.getDataProvider().refreshAll());
                            }
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                            Notification.show("Error: " + e.getMessage(), Notification.Type.ERROR_MESSAGE);
                        }
                    }
                }));
        verticalLayout.addComponents(add, remove, crawl, validate);
        return verticalLayout;
    }

    private VerticalLayout buildActiveHubsLayout() {
        VerticalLayout hubsLayout = new VerticalLayout();
        listSelect.setSizeFull();
        listSelect.setDataProvider(new AbstractBackEndDataProvider<TfHub, String>() {
            @Override
            protected Stream<TfHub> fetchFromBackEnd(Query<TfHub, String> query) {
                return trackFindService.getTrackHubs(true).stream();
            }

            @Override
            protected int sizeInBackEnd(Query<TfHub, String> query) {
                return (int) fetchFromBackEnd(query).count();
            }
        });
        listSelect.setItemCaptionGenerator(hub -> {
            String caption = hub.getRepository() + ": " + hub.getName();
            Optional<TfVersion> currentVersionOptional = hub.getCurrentVersion();
            if (currentVersionOptional.isPresent()) {
                caption += " ⬇️";
                Boolean validation = currentVersionOptional.get().getValidation();
                if (validation != null) {
                    caption += (validation ? " ✅" : " ❌");
                }
            }
            return caption;
        });
        listSelect.addSelectionListener((MultiSelectionListener<TfHub>) event -> {
            Set<TfHub> selectedItems = listSelect.getSelectedItems();
            remove.setEnabled(CollectionUtils.isNotEmpty(selectedItems));
            crawl.setEnabled(CollectionUtils.isNotEmpty(selectedItems));
            validate.setEnabled(CollectionUtils.isNotEmpty(selectedItems));
            saveDisplayNameButton.setEnabled(CollectionUtils.isNotEmpty(selectedItems));
            displayNameTextField.setEnabled(CollectionUtils.isNotEmpty(selectedItems));
            if (CollectionUtils.isNotEmpty(selectedItems)) {
                String displayName = selectedItems.iterator().next().getDisplayName();
                displayNameTextField.setValue(displayName != null ? displayName : "");
            }
        });
        Panel panel = new Panel("Hub selection", listSelect);
        panel.setSizeFull();
        displayNameTextField.setEnabled(false);
        displayNameTextField.setHeightUndefined();
        displayNameTextField.setWidth(100, Unit.PERCENTAGE);
        saveDisplayNameButton.setEnabled(false);
        saveDisplayNameButton.setHeightUndefined();
        saveDisplayNameButton.setWidth(100, Unit.PERCENTAGE);
        saveDisplayNameButton.addClickListener((Button.ClickListener) event -> {
            TfHub hub = listSelect.getSelectedItems().iterator().next();
            String value = displayNameTextField.getValue();
            hub.setDisplayName(StringUtils.isEmpty(value) ? null : value);
            hubRepository.save(hub);
        });
        hubsLayout.addComponentsAndExpand(panel, displayNameTextField, saveDisplayNameButton);
        hubsLayout.setExpandRatio(panel, 0.80f);
        hubsLayout.setExpandRatio(displayNameTextField, 0.13f);
        hubsLayout.setExpandRatio(saveDisplayNameButton, 0.07f);
        return hubsLayout;
    }

    private HorizontalLayout buildMainLayout(VerticalLayout leftLayout, VerticalLayout middleLayout, VerticalLayout rightLayout) {
        HorizontalLayout mainLayout = new HorizontalLayout(leftLayout, middleLayout, rightLayout);
        mainLayout.setExpandRatio(leftLayout, 0.4f);
        mainLayout.setExpandRatio(middleLayout, 0.2f);
        mainLayout.setExpandRatio(rightLayout, 0.4f);
        mainLayout.setSizeFull();
        return mainLayout;
    }

    @Autowired
    public void setHubRepository(HubRepository hubRepository) {
        this.hubRepository = hubRepository;
    }

    @Autowired
    public void setValidationService(ValidationService validationService) {
        this.validationService = validationService;
    }

}
