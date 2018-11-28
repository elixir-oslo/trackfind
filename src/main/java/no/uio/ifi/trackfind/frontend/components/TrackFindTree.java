package no.uio.ifi.trackfind.frontend.components;

import com.vaadin.ui.Component;
import com.vaadin.ui.Tree;

/**
 * Vaadin Tree extension to expose it's CompositionRoot (which is protected out-of-the-box) and to inject back-end DataProvider.
 *
 * @param <T> Type of Vaadin Tree entries.
 * @author Dmytro Titov
 */
public class TrackFindTree<T> extends Tree<T> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getCompositionRoot() {
        return super.getCompositionRoot();
    }

}
