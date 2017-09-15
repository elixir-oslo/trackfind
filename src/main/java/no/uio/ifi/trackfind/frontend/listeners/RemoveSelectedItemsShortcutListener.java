package no.uio.ifi.trackfind.frontend.listeners;

import com.vaadin.data.provider.Query;
import com.vaadin.event.ShortcutListener;
import com.vaadin.ui.ListSelect;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RemoveSelectedItemsShortcutListener<T> extends ShortcutListener {

    private ListSelect<T> listSelect;

    public RemoveSelectedItemsShortcutListener(ListSelect<T> listSelect, int keyCode) {
        super("Delete item(s)", keyCode, null);
        this.listSelect = listSelect;
    }

    @Override
    public void handleAction(Object sender, Object target) {
        Set<T> selectedItems = listSelect.getSelectedItems();
        Set<T> attributesToExport = new HashSet<>(getAllItems());
        attributesToExport.removeAll(selectedItems);
        listSelect.setItems(attributesToExport);
    }

    private Set<T> getAllItems() {
        return listSelect.getDataProvider().fetch(new Query<>()).collect(Collectors.toSet());
    }

}
