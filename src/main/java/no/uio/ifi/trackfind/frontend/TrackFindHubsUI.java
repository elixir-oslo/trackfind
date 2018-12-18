package no.uio.ifi.trackfind.frontend;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.dao.Hub;

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

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        HorizontalLayout headerLayout = buildHeaderLayout();
        VerticalLayout treeLayout = buildHubsLayout();
        VerticalLayout attributesMappingOuterLayout = new VerticalLayout();
        HorizontalLayout mainLayout = buildMainLayout(treeLayout, attributesMappingOuterLayout);
        HorizontalLayout footerLayout = buildFooterLayout();
        VerticalLayout outerLayout = buildOuterLayout(headerLayout, mainLayout, footerLayout);
        setContent(outerLayout);
    }

    private VerticalLayout buildHubsLayout() {
        VerticalLayout hubsLayout = new VerticalLayout();
        ComboBox<Hub> comboBox = new ComboBox<>("Hubs");
        comboBox.setWidth(100, Unit.PERCENTAGE);
        comboBox.setItems(trackFindService.getAllTrackHubs());
        comboBox.setItemCaptionGenerator(h -> h.getRepository() + ": " + h.getHub());
        Panel panel = new Panel("Hub selection", comboBox);
        hubsLayout.addComponentsAndExpand(panel);
//        comboBox.addValueChangeListener(event -> {
//            if (event.getSource().isEmpty()) {
//                message.setText("No browser selected");
//            } else {
//                message.setText("Selected browser: " + event.getValue());
//            }
//        });
        return hubsLayout;
    }

    private HorizontalLayout buildMainLayout(VerticalLayout leftLayout, VerticalLayout rightLayout) {
        HorizontalLayout mainLayout = new HorizontalLayout(leftLayout, rightLayout);
        mainLayout.setExpandRatio(leftLayout, 0.33f);
        mainLayout.setExpandRatio(rightLayout, 0.66f);
        mainLayout.setSizeFull();
        return mainLayout;
    }

}
