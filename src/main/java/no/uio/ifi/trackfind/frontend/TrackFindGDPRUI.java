package no.uio.ifi.trackfind.frontend;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * GDPR Vaadin UI of the application.
 * Uses custom theme (VAADIN/themes/trackfind/trackfind.scss).
 * Uses custom WidgetSet (TrackFindWidgetSet.gwt.xml).
 *
 * @author Dmytro Titov
 */
@SpringUI(path = "/GDPR")
@Widgetset("TrackFindWidgetSet")
@Title("GDPR")
@Theme("trackfind")
@Slf4j
public class TrackFindGDPRUI extends AbstractUI {

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        HorizontalLayout headerLayout = buildHeaderLayout();
        HorizontalLayout mainLayout = buildMainLayout();
        HorizontalLayout footerLayout = buildFooterLayout();
        VerticalLayout outerLayout = buildOuterLayout(headerLayout, mainLayout, footerLayout);
        setContent(outerLayout);
    }

    private HorizontalLayout buildMainLayout() {
        Label content = new Label();
        content.setSizeFull();
        try {
            String html = IOUtils.resourceToString("/GDPR.html", Charset.defaultCharset());
            content.setValue(html);
            content.setContentMode(ContentMode.HTML);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        Panel panel = new Panel("User Rights under the GDPR and " +
                "how to exercise them in the context of the TrackFind Service provided by the University of Oslo",
                new VerticalLayout(content));
        panel.setSizeFull();
        HorizontalLayout mainLayout = new HorizontalLayout(panel);
        mainLayout.setSizeFull();
        mainLayout.setMargin(new MarginInfo(true, true, false, true));
        return mainLayout;
    }

}
