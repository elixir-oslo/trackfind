package no.uio.ifi.trackfind.frontend.components;

import com.vaadin.ui.Component;
import com.vaadin.ui.Tree;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;

public class TrackFindTree<T> extends Tree<T> {

    private DataProvider trackDataProvider;

    public TrackFindTree(DataProvider trackDataProvider) {
        this.trackDataProvider = trackDataProvider;
    }

    public DataProvider getTrackDataProvider() {
        return trackDataProvider;
    }

    @Override
    public Component getCompositionRoot() {
        return super.getCompositionRoot();
    }

}
