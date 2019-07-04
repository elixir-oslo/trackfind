package no.uio.ifi.trackfind.frontend;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.BrowserFrame;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Hystrix Vaadin UI of the application.
 * Uses custom theme (VAADIN/themes/trackfind/trackfind.scss).
 * Uses custom WidgetSet (TrackFindWidgetSet.gwt.xml).
 *
 * @author Dmytro Titov
 */
@SpringUI(path = "/monitor")
@Widgetset("TrackFindWidgetSet")
@Title("Hystryx Dashboard")
@Theme("trackfind")
@Slf4j
public class TrackFindMonitorUI extends AbstractUI {

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        HorizontalLayout headerLayout = buildHeaderLayout();
        VerticalLayout referencesLayout = buildHystrixLayout();
        HorizontalLayout mainLayout = buildMainLayout(referencesLayout);
        HorizontalLayout footerLayout = buildFooterLayout();
        VerticalLayout outerLayout = buildOuterLayout(headerLayout, mainLayout, footerLayout);
        setContent(outerLayout);
    }

    private VerticalLayout buildHystrixLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL("http://checkip.amazonaws.com").openStream()))) {
            String host = "localhost";
            String implementationVersion = getClass().getPackage().getImplementationVersion();
            if (implementationVersion != null) {
                host = in.readLine();
            }
            BrowserFrame browser = new BrowserFrame("Browser",
                    new ExternalResource("http://" + host + "/hystrix/monitor?stream=http%3A%2F%2Flocalhost%2Factuator%2Fhystrix.stream&title=TrackFind"));
            browser.setSizeFull();
            Panel panel = new Panel("Circuits status", browser);
            panel.setSizeFull();
            layout.addComponents(panel);
            layout.setExpandRatio(panel, 1f);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return layout;
    }


    private HorizontalLayout buildMainLayout(VerticalLayout leftLayout) {
        HorizontalLayout mainLayout = new HorizontalLayout(leftLayout);
        mainLayout.setExpandRatio(leftLayout, 0.66f);
        mainLayout.setSizeFull();
        return mainLayout;
    }

}