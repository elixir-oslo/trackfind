package no.uio.ifi.trackfind.frontend.listeners;

import com.vaadin.ui.Button;
import no.uio.ifi.trackfind.frontend.TrackFindMainUI;

/**
 * Listener for "Add to query" Button click.
 *
 * @author Dmytro Titov
 */
public class AddToQueryButtonClickListener extends MoveAttributeValueHandler implements Button.ClickListener {

    private TrackFindMainUI ui;

    /**
     * Constructor with binding to TrackFindMainUI.
     *
     * @param ui TrackFind UI.
     */
    public AddToQueryButtonClickListener(TrackFindMainUI ui) {
        this.ui = ui;
    }

    /**
     * Processes click event.
     *
     * @param event Click event.
     */
    @Override
    public void buttonClick(Button.ClickEvent event) {
        processDragAndDrop(ui.getQueryTextArea(), null, ui.getCurrentTree().getSelectedItems());
    }

}
