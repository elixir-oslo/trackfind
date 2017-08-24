package no.uio.ifi.trackfind.frontend.listeners;

import com.vaadin.shared.MouseEventDetails;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.dnd.event.DropEvent;
import com.vaadin.ui.dnd.event.DropListener;
import no.uio.ifi.trackfind.frontend.data.TreeNode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Listener for Drop event bind to Vaadin TextArea.
 *
 * @author Dmytro Titov
 */
@SuppressWarnings("PMD.NonStaticInitializer")
public class TextAreaDropListener implements DropListener<TextArea> {

    private static Map<Boolean, String> OPERATORS = new HashMap<Boolean, String>() {{
        put(true, " AND ");
        put(false, " OR ");
    }};

    private static String INVERSION = "NOT ";

    private TextArea textArea;

    /**
     * Constructor with binding to TextArea.
     *
     * @param textArea TextArea to handle drops for.
     */
    public TextAreaDropListener(TextArea textArea) {
        this.textArea = textArea;
    }

    /**
     * Processes drop event.
     *
     * @param event Drop event.
     */
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

    /**
     * Fetches mouse click details and processes single or multiple drop.
     *
     * @param mouseEventDetails Vaadin MouseEventDetails (with states of keyboard keys as well).
     * @param selectedItems     Set of items selected (being dropped).
     */
    private void processDragAndDrop(MouseEventDetails mouseEventDetails, Set<TreeNode> selectedItems) {
        boolean logicalOperation = !mouseEventDetails.isAltKey();
        boolean inversion = mouseEventDetails.isShiftKey();
        int size = CollectionUtils.size(selectedItems);
        if (size == 1) {
            processDragAndDropSingle(logicalOperation, inversion, selectedItems.iterator().next());
        } else if (size > 1) {
            processDragAndDropMultiple(logicalOperation, inversion, selectedItems);
        }
    }

    /**
     * Processes drop of multiple items.
     *
     * @param logicalOperation true for AND, false for OR.
     * @param inversion        true for NOT
     * @param items            Items to drop (always values).
     */
    private void processDragAndDropMultiple(boolean logicalOperation, boolean inversion, Set<TreeNode> items) {
        String operator = OPERATORS.get(logicalOperation);
        StringBuilder value = new StringBuilder(textArea.getValue());
        if (StringUtils.isNoneEmpty(value.toString())) {
            value.append(operator);
        }
        if (inversion) {
            value.append(INVERSION);
        }
        TreeNode firstItem = items.iterator().next();
        String attribute = escapeQueryTerm(firstItem.getPath().split(":")[0]);
        value.append(attribute).append(": (");
        for (TreeNode item : items) {
            value.append(escapeQueryTerm(item.toString())).append(" OR ");
        }
        value = new StringBuilder(value.subSequence(0, value.length() - 4) + ")\n\n");
        textArea.setValue(value.toString());
    }

    /**
     * Processes drop of single item.
     *
     * @param logicalOperation true for AND, false for OR.
     * @param inversion        true for NOT
     * @param item             Item to drop (either attribute or value).
     */
    private void processDragAndDropSingle(boolean logicalOperation, boolean inversion, TreeNode item) {
        String operator = OPERATORS.get(logicalOperation);
        String value = textArea.getValue();
        if (StringUtils.isNoneEmpty(value)) {
            value += operator;
        }
        if (inversion) {
            value += INVERSION;
        }
        String attribute = escapeQueryTerm(item.getPath().split(":")[0]);
        value += attribute + ": ";
        value += escapeQueryTerm(item.toString()) + "\n\n";
        textArea.setValue(value);
    }

    /**
     * Sanitizes query for Apache Lucene.
     *
     * @param queryTerm Raw query term (attribute or value).
     * @return Sanitized query term (attribute or value).
     */
    private String escapeQueryTerm(String queryTerm) {
        return queryTerm.replace("/", "\\/").replace(" ", "\\ ").replace(":", "\\:");
    }

}
