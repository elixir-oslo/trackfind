package no.uio.ifi.trackfind.frontend.providers;

import com.vaadin.data.provider.AbstractBackEndDataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.TabSheet;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import no.uio.ifi.trackfind.backend.pojo.TfObjectType;
import no.uio.ifi.trackfind.backend.services.impl.MetamodelService;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

public class AttributesDataProvider extends AbstractBackEndDataProvider<String, String> {

    private TabSheet tabSheet;
    private ComboBox<TfObjectType> categoryComboBox;
    private String separator;
    private MetamodelService metamodelService;

    public AttributesDataProvider(TabSheet tabSheet, ComboBox<TfObjectType> categoryComboBox, String separator, MetamodelService metamodelService) {
        this.tabSheet = tabSheet;
        this.categoryComboBox = categoryComboBox;
        this.separator = separator;
        this.metamodelService = metamodelService;
    }

    @Override
    protected Stream<String> fetchFromBackEnd(Query<String, String> query) {
        Grid selectedTab = (Grid) tabSheet.getSelectedTab();
        TfHub hub = (TfHub) selectedTab.getData();
        Optional<TfObjectType> selectedItemOptional = categoryComboBox.getSelectedItem();
        if (!selectedItemOptional.isPresent()) {
            return Stream.empty();
        }
        Collection<String> attributesFlat = metamodelService.getAttributesFlat(hub.getRepository(), hub.getName(), selectedItemOptional.get().getName(), null);
        return query
                .getFilter()
                .map(s -> attributesFlat.stream().filter(a -> a.contains(s)))
                .orElseGet(attributesFlat::stream)
                .map(a -> {
                    String[] split = a.split(separator);
                    for (int i = 0; i < split.length; i++) {
                        split[i] = "'" + split[i] + "'";
                    }
                    return String.join(separator, split);
                });
    }

    @Override
    protected int sizeInBackEnd(Query<String, String> query) {
        return (int) fetchFromBackEnd(query).count();
    }

}
