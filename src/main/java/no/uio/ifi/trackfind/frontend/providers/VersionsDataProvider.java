package no.uio.ifi.trackfind.frontend.providers;

import com.vaadin.data.provider.AbstractBackEndDataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.TabSheet;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import no.uio.ifi.trackfind.backend.pojo.TfVersion;
import no.uio.ifi.trackfind.frontend.components.TrackFindTree;

import java.util.stream.Stream;

/**
 * Vaadin data provider for selected hub's versions.
 */
public class VersionsDataProvider extends AbstractBackEndDataProvider<TfVersion, String> {

    private TabSheet tabSheet;

    public VersionsDataProvider(TabSheet tabSheet) {
        this.tabSheet = tabSheet;
    }

    @Override
    protected Stream<TfVersion> fetchFromBackEnd(Query<TfVersion, String> query) {
        TfHub hub;
        Component selectedTab = tabSheet.getSelectedTab();
        if (selectedTab instanceof Grid) {
            Grid grid = (Grid) selectedTab;
            hub = (TfHub) grid.getData();
        } else if (selectedTab instanceof TrackFindTree) {
            TrackFindTree trackFindTree = (TrackFindTree) selectedTab;
            hub = trackFindTree.getHub();
        } else {
            return Stream.empty();
        }
        return query
                .getFilter()
                .map(s -> hub.getVersions().stream().filter(v -> v.toString().contains(s)))
                .orElseGet(() -> hub.getVersions().stream());
    }

    @Override
    protected int sizeInBackEnd(Query<TfVersion, String> query) {
        return (int) fetchFromBackEnd(query).count();
    }

}
