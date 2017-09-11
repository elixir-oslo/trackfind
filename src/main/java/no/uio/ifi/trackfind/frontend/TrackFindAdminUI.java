package no.uio.ifi.trackfind.frontend;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin Vaadin UI of the application.
 * Uses custom theme (VAADIN/themes/trackfind/trackfind.scss).
 * Uses custom WidgetSet (TrackFindWidgetSet.gwt.xml).
 *
 * @author Dmytro Titov
 */
@SpringUI(path = "/admin")
@Widgetset("TrackFindWidgetSet")
@Title("Dashboard")
@Theme("trackfind")
@Slf4j
public class TrackFindAdminUI extends AbstractUI {

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        HorizontalLayout headerLayout = buildHeaderLayout();
        VerticalLayout treeLayout = buildTreeLayout();
        VerticalLayout fieldsLayout = buildFieldsLayout();
        VerticalLayout searchExportLayout = buildSearchExportLayout();
        HorizontalLayout mainLayout = buildMainLayout(treeLayout, fieldsLayout, searchExportLayout);
        HorizontalLayout footerLayout = buildFooterLayout();
        VerticalLayout outerLayout = buildOuterLayout(headerLayout, mainLayout, footerLayout);
        setContent(outerLayout);
    }

    private HorizontalLayout buildMainLayout(VerticalLayout treeLayout, VerticalLayout queryLayout, VerticalLayout resultsLayout) {
        HorizontalLayout mainLayout = new HorizontalLayout(treeLayout, queryLayout, resultsLayout);
        mainLayout.setSizeFull();
        return mainLayout;
    }

    private VerticalLayout buildSearchExportLayout() {
        ListSelect<String> searchList = new ListSelect<>("The List 1");
        searchList.setItems("Mercury", "Venus", "Earth");
        searchList.setRows(5);
        ListSelect<String> exportList = new ListSelect<>("The List 2");
        exportList.setItems("Mercury", "Venus", "Earth");
        exportList.setRows(5);
        Button saveButton = new Button("Save");
        VerticalLayout searchExportLayout = new VerticalLayout(searchList, exportList, saveButton);
        searchExportLayout.setSizeFull();
        return searchExportLayout;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private VerticalLayout buildFieldsLayout() {
        TextField testTextField = new TextField("Test");
        VerticalLayout queryLayout = new VerticalLayout(testTextField);
        queryLayout.setSizeFull();
        return queryLayout;
    }

}
