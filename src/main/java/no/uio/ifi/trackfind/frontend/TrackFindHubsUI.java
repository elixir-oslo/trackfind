package no.uio.ifi.trackfind.frontend;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.HasValue;
import com.vaadin.data.provider.Query;
import com.vaadin.event.selection.MultiSelectionListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import org.vaadin.dialogs.ConfirmDialog;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

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

    private ComboBox<TfHub> comboBox;
    private ListSelect<TfHub> listSelect;
    private Button add;
    private Button remove;
    private Button crawl;

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
        comboBox = new ComboBox<>("Available hubs");
        comboBox.setWidth(100, Unit.PERCENTAGE);
        Collection<TfHub> allTrackHubs = trackFindService.getTrackHubs(false);
        Collection<TfHub> activeTrackHubs = trackFindService.getTrackHubs(true);
        allTrackHubs.removeAll(activeTrackHubs);
        comboBox.setItems(allTrackHubs);
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
        add = new Button("Activate →");
        add.setEnabled(false);
        add.setWidth(100, Unit.PERCENTAGE);
        add.addClickListener((Button.ClickListener) event -> comboBox.getSelectedItem().ifPresent(hub -> {
            trackFindService.activateHubs(Collections.singleton(hub));
            listSelect.setItems(trackFindService.getTrackHubs(true));
            listSelect.getDataProvider().refreshAll();
            Set<TfHub> availableHubs = comboBox.getDataProvider().fetch(new Query<>()).collect(Collectors.toSet());
            availableHubs.remove(hub);
            comboBox.clear();
            comboBox.setItems(availableHubs);
            comboBox.getDataProvider().refreshAll();
        }));
        remove = new Button("Deactivate ←");
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
            listSelect.setItems(trackFindService.getTrackHubs(true));
            listSelect.getDataProvider().refreshAll();
            Set<TfHub> availableHubs = comboBox.getDataProvider().fetch(new Query<>()).collect(Collectors.toSet());
            availableHubs.addAll(activeHubs);
            comboBox.setItems(availableHubs);
            comboBox.getDataProvider().refreshAll();
        });
        crawl = new Button("Crawl");
        crawl.setEnabled(false);
        crawl.setWidth(100, Unit.PERCENTAGE);
        crawl.addClickListener((Button.ClickListener) event -> ConfirmDialog.show(getUI(),
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
                    }
                }));
        crawl.setWidth(100, Unit.PERCENTAGE);
        verticalLayout.addComponents(add, remove, crawl);
        return verticalLayout;
    }

    private VerticalLayout buildActiveHubsLayout() {
        VerticalLayout hubsLayout = new VerticalLayout();
        listSelect = new ListSelect<>();
        listSelect.setWidth(100, Unit.PERCENTAGE);
        listSelect.setHeight(100, Unit.PERCENTAGE);
        listSelect.setItems(trackFindService.getTrackHubs(true));
        listSelect.setItemCaptionGenerator(h -> h.getRepository() + ": " + h.getName());
        listSelect.addSelectionListener((MultiSelectionListener<TfHub>) event -> remove.setEnabled(!listSelect.getSelectedItems().isEmpty()));
        listSelect.addSelectionListener((MultiSelectionListener<TfHub>) event -> crawl.setEnabled(!listSelect.getSelectedItems().isEmpty()));
        Panel panel = new Panel("Hub selection", listSelect);
        hubsLayout.addComponentsAndExpand(panel);
        return hubsLayout;
    }

    private HorizontalLayout buildMainLayout(VerticalLayout leftLayout, VerticalLayout middleLayout, VerticalLayout rightLayout) {
        HorizontalLayout mainLayout = new HorizontalLayout(leftLayout, middleLayout, rightLayout);
        mainLayout.setExpandRatio(leftLayout, 0.33f);
        mainLayout.setExpandRatio(middleLayout, 0.33f);
        mainLayout.setExpandRatio(rightLayout, 0.33f);
        mainLayout.setSizeFull();
        return mainLayout;
    }

}
