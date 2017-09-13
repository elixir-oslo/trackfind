package no.uio.ifi.trackfind.frontend.components;

import com.vaadin.ui.Component;
import com.vaadin.ui.Tree;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;

/**
 * Vaadin Tree extension to expose it's CompositionRoot (which is protected out-of-the-box) and to inject back-end DataProvider.
 *
 * @param <T> Type of Vaadin Tree entries.
 * @author Dmytro Titov
 */
public class TrackFindTree<T> extends Tree<T> { // TODO: Fix issues with large tree.

    private DataProvider trackDataProvider;

    /**
     * Constructor injecting back-end DataProvider th the Tree instance.
     *
     * @param trackDataProvider Back-end DataProvider.
     */
    public TrackFindTree(DataProvider trackDataProvider) {
        this.trackDataProvider = trackDataProvider;
    }

    /**
     * Gets back-end DataProvider injected to this Tree.
     *
     * @return Back-end DataProvider.
     */
    public DataProvider getBackEndDataProvider() {
        return trackDataProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getCompositionRoot() {
        return super.getCompositionRoot();
    }

}
