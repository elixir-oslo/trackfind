package no.uio.ifi.trackfind.frontend;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.pojo.TfUser;
import no.uio.ifi.trackfind.backend.security.TrackFindUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Users Vaadin UI of the application.
 * Uses custom theme (VAADIN/themes/trackfind/trackfind.scss).
 * Uses custom WidgetSet (TrackFindWidgetSet.gwt.xml).
 *
 * @author Dmytro Titov
 */
@SpringUI(path = "/users")
@Widgetset("TrackFindWidgetSet")
@Title("Users")
@Theme("trackfind")
@Slf4j
public class TrackFindUsersUI extends AbstractUI {

    private TrackFindUserDetailsService userDetailsService;

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        HorizontalLayout headerLayout = buildHeaderLayout();
        VerticalLayout referencesLayout = buildUsersLayout();
        HorizontalLayout mainLayout = buildMainLayout(referencesLayout);
        HorizontalLayout footerLayout = buildFooterLayout();
        VerticalLayout outerLayout = buildOuterLayout(headerLayout, mainLayout, footerLayout);
        setContent(outerLayout);
    }

    private VerticalLayout buildUsersLayout() {
        VerticalLayout usersLayout = new VerticalLayout();
        usersLayout.setSizeFull();
        Grid<TfUser> grid = new Grid<>(TfUser.class);
        grid.setSizeFull();
        grid.removeColumn("id");
        grid.removeColumn("credentialsNonExpired");
        grid.removeColumn("authorities");
        grid.removeColumn("password");
        grid.removeColumn("versions");
        grid.removeColumn("accountNonExpired");
        grid.removeColumn("accountNonLocked");
        grid.removeColumn("enabled");
        grid.setItems(userDetailsService.getAllUsers());
        Button activateDeactivateButton = new Button("Activate/deactivate", (Button.ClickListener) event -> {
            try {
                TfUser user = grid.getSelectedItems().iterator().next();
                if (user.isActive()) {
                    userDetailsService.deactivateUser(user);
                } else {
                    userDetailsService.activateUser(user);
                }
                grid.setItems(userDetailsService.getAllUsers());
            } catch (DataIntegrityViolationException e) {
                Notification.show("There should be at least one active admin user", Notification.Type.ERROR_MESSAGE);
            }
        });
        activateDeactivateButton.setEnabled(false);
        Button grantRevokeAdminAuthorityButton = new Button("Grant/revoke admin authority", (Button.ClickListener) event -> {
            try {
                TfUser user = grid.getSelectedItems().iterator().next();
                if (user.isAdmin()) {
                    userDetailsService.revokeAdminAuthority(user);
                } else {
                    userDetailsService.grantAdminAuthority(user);
                }
                grid.setItems(userDetailsService.getAllUsers());
            } catch (DataIntegrityViolationException e) {
                Notification.show("There should be at least one active admin user", Notification.Type.ERROR_MESSAGE);
            }
        });
        grantRevokeAdminAuthorityButton.setEnabled(false);
        grid.addSelectionListener((SelectionListener<TfUser>) event -> {
            activateDeactivateButton.setEnabled(event.getFirstSelectedItem().isPresent());
            grantRevokeAdminAuthorityButton.setEnabled(event.getFirstSelectedItem().isPresent());
        });
        Panel usersPanel = new Panel("Users", grid);
        usersPanel.setSizeFull();
        HorizontalLayout controlsLayout = new HorizontalLayout(grantRevokeAdminAuthorityButton, activateDeactivateButton);
        controlsLayout.setComponentAlignment(activateDeactivateButton, Alignment.BOTTOM_RIGHT);
        controlsLayout.setComponentAlignment(grantRevokeAdminAuthorityButton, Alignment.BOTTOM_RIGHT);
        usersLayout.addComponents(usersPanel, controlsLayout);
        usersLayout.setExpandRatio(usersPanel, 1f);
        return usersLayout;
    }

    private HorizontalLayout buildMainLayout(VerticalLayout leftLayout) {
        HorizontalLayout mainLayout = new HorizontalLayout(leftLayout);
        mainLayout.setExpandRatio(leftLayout, 0.66f);
        mainLayout.setSizeFull();
        return mainLayout;
    }

    @Autowired
    public void setUserDetailsService(TrackFindUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

}
