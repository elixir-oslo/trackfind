package no.uio.ifi.trackfind.frontend.listeners;

import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.dnd.event.DropEvent;
import com.vaadin.ui.dnd.event.DropListener;
import no.uio.ifi.trackfind.backend.data.TreeNode;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Optional;
import java.util.Set;

/**
 * Listener for Drop event bind to Vaadin TextField.
 *
 * @author Dmytro Titov
 */
@SuppressWarnings("PMD.NonStaticInitializer")
public class TextFieldDropListener implements DropListener<TextField> {

    private TextField textField;

    /**
     * Constructor with binding to TextField.
     *
     * @param textField TextField to handle drops for.
     */
    public TextFieldDropListener(TextField textField) {
        this.textField = textField;
    }

    /**
     * Processes drop event.
     *
     * @param event Drop event.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void drop(DropEvent<TextField> event) {
        Optional<AbstractComponent> componentOptional = event.getDragSourceComponent();
        if (componentOptional.isPresent() && componentOptional.get() instanceof TreeGrid) {
            TreeGrid<TreeNode> treeGrid = (TreeGrid<TreeNode>) componentOptional.get();
            Set<TreeNode> selectedItems = treeGrid.getSelectedItems();
            int size = CollectionUtils.size(selectedItems);
            if (size == 1) {
                TreeNode item = selectedItems.iterator().next();
                textField.setValue(item.getSQLPath());
            } else if (size > 1) {
                Notification.show("Please, drag only one attribute in there.");
            }
        }
    }

}
