package no.uio.ifi.trackfind.frontend.listeners;

import com.vaadin.shared.MouseEventDetails;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.dnd.event.DropEvent;
import com.vaadin.ui.dnd.event.DropListener;
import no.uio.ifi.trackfind.frontend.data.TreeNode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class TextAreaDropListener implements DropListener<TextArea> {

    private static Map<Boolean, String> OPERATORS = new HashMap<Boolean, String>() {{
        put(true, " AND ");
        put(false, " OR ");
    }};

    private static String INVERSION = "NOT ";

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
        boolean inversion = mouseEventDetails.isShiftKey();
        if (CollectionUtils.size(selectedItems) == 1) {
            processDragAndDropSingle(logicalOperation, inversion, selectedItems.iterator().next());
        } else {
            processDragAndDropMultiple(logicalOperation, inversion, selectedItems);
        }
    }

    private void processDragAndDropMultiple(boolean logicalOperation, boolean inversion, Set<TreeNode> items) {
        String operator = OPERATORS.get(logicalOperation);
        String value = queryTextArea.getValue();
        if (StringUtils.isNoneEmpty(value)) {
            value += operator;
        }
        if (inversion) {
            value += INVERSION;
        }
        TreeNode firstItem = items.iterator().next();
        String attribute = firstItem.getPath().split(":")[0];
        value += attribute + ": (";
        for (TreeNode item : items) {
            value += item.toString() + " OR ";
        }
        value = value.subSequence(0, value.length() - 4) + ")";
        queryTextArea.setValue(value);
    }

    private void processDragAndDropSingle(boolean logicalOperation, boolean inversion, TreeNode item) {
        String operator = OPERATORS.get(logicalOperation);
        String value = queryTextArea.getValue();
        if (StringUtils.isNoneEmpty(value)) {
            value += operator;
        }
        if (inversion) {
            value += INVERSION;
        }
        value += item.getPath();
        queryTextArea.setValue(value);
    }

}
