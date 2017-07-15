package no.uio.ifi.trackfind.frontend.listeners;

import com.vaadin.shared.MouseEventDetails;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.dnd.event.DropEvent;
import com.vaadin.ui.dnd.event.DropListener;
import no.uio.ifi.trackfind.frontend.data.TreeNode;
import org.apache.commons.collections.CollectionUtils;

import java.util.Optional;
import java.util.Set;

public class TextAreaDropListener implements DropListener<TextArea> {

    private TextArea queryTextArea;

    public TextAreaDropListener(TextArea queryTextArea) {
        this.queryTextArea = queryTextArea;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void drop(DropEvent<TextArea> event) {
        Optional<AbstractComponent> componentOptional = event.getDragSourceComponent();
        if (componentOptional.isPresent() && componentOptional.get() instanceof TreeGrid) {
            TreeGrid<TreeNode> treeGrid = (TreeGrid<TreeNode>) componentOptional.get();
            Set<TreeNode> selectedItems = treeGrid.getSelectedItems();
            processDragAndDrop(event.getMouseEventDetails(), selectedItems);
        }
    }

    private void processDragAndDrop(MouseEventDetails mouseEventDetails, Set<TreeNode> selectedItems) {
        boolean logicalOperation = !mouseEventDetails.isAltKey();
        boolean inversion = mouseEventDetails.isCtrlKey() || mouseEventDetails.isMetaKey();
        if (CollectionUtils.size(selectedItems) == 1) {
            processDragAndDropSingle(logicalOperation, inversion, selectedItems.iterator().next());
        } else {
            processDragAndDropMultiple(logicalOperation, inversion, selectedItems);
        }
        for (TreeNode selectedItem : selectedItems) {
            queryTextArea.setValue(queryTextArea.getValue() + " AND " + selectedItem.getPath());
        }
    }

    private void processDragAndDropMultiple(boolean logicalOperation, boolean inversion, Set<TreeNode> items) {

    }

    private void processDragAndDropSingle(boolean logicalOperation, boolean inversion, TreeNode item) {

    }

}
