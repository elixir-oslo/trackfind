package no.uio.ifi.trackfind.frontend.listeners;

import com.vaadin.ui.Button;
import no.uio.ifi.trackfind.frontend.TrackFindMainUI;
import no.uio.ifi.trackfind.frontend.components.KeyboardInterceptorExtension;

/**
 * Listener for "Add to query" Button click.
 *
 * @author Dmytro Titov
 */
public class AddToQueryButtonClickListener extends MoveAttributeValueHandler implements Button.ClickListener {

    private TrackFindMainUI ui;
    private KeyboardInterceptorExtension keyboardInterceptorExtension;

    /**
     * Constructor with binding to TrackFindMainUI.
     *
     * @param ui              TrackFind UI.
     * @param levelsSeparator Levels separator.
     */
    public AddToQueryButtonClickListener(TrackFindMainUI ui, KeyboardInterceptorExtension keyboardInterceptorExtension, String levelsSeparator) {
        super(levelsSeparator);
        this.ui = ui;
        this.keyboardInterceptorExtension = keyboardInterceptorExtension;
    }

    /**
     * Processes click event.
     *
     * @param event Click event.
     */
    @Override
    public void buttonClick(Button.ClickEvent event) {
        boolean logicalOperation = !keyboardInterceptorExtension.isAltKeyDown();
        boolean inversion = keyboardInterceptorExtension.isShiftKeyDown();
        processDragAndDrop(ui.getQueryTextArea(), logicalOperation, inversion, ui.getCurrentTree().getSelectedItems());
    }

}
