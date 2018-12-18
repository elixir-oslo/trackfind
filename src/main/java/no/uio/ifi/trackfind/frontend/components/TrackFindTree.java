package no.uio.ifi.trackfind.frontend.components;

import com.vaadin.ui.Component;
import com.vaadin.ui.Tree;
import lombok.AllArgsConstructor;
import lombok.Data;
import no.uio.ifi.trackfind.backend.dao.Hub;

/**
 * Vaadin Tree extension to expose it's CompositionRoot (which is protected out-of-the-box) and to inject back-end DataProvider.
 *
 * @param <T> Type of Vaadin Tree entries.
 * @author Dmytro Titov
 */
@Data
@AllArgsConstructor
public class TrackFindTree<T> extends Tree<T> {

    private Hub hub;

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getCompositionRoot() {
        return super.getCompositionRoot();
    }

}
