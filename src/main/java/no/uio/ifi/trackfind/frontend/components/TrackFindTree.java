package no.uio.ifi.trackfind.frontend.components;

import com.vaadin.ui.Component;
import com.vaadin.ui.Tree;
import lombok.AllArgsConstructor;
import no.uio.ifi.trackfind.backend.pojo.TfHub;

/**
 * Vaadin Tree extension to expose it's CompositionRoot (which is protected out-of-the-box) and to inject back-end DataProvider.
 *
 * @param <T> Type of Vaadin Tree entries.
 * @author Dmytro Titov
 */
@AllArgsConstructor
public class TrackFindTree<T> extends Tree<T> {

    private TfHub hub;

    public TfHub getHub() {
        return hub;
    }

    public void setHub(TfHub hub) {
        this.hub = hub;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getCompositionRoot() {
        return super.getCompositionRoot();
    }

}
