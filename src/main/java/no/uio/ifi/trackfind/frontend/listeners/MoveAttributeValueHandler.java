package no.uio.ifi.trackfind.frontend.listeners;

import com.vaadin.shared.MouseEventDetails;
import com.vaadin.ui.TextArea;
import no.uio.ifi.trackfind.frontend.data.TreeNode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Moves attribute or value(s) from Tree to TextArea.
 *
 * @author Dmytro Titov
 */
@SuppressWarnings("PMD.NonStaticInitializer")
public abstract class MoveAttributeValueHandler {

    private static final Map<Boolean, String> CONDITIONS = new HashMap<Boolean, String>() {{
        put(true, "AND ");
        put(false, "OR ");
    }};

    private static final String EQUALITY_OPERATOR = " ? ";

    private String datasetPrefix;
    private String levelsSeparator;

    public MoveAttributeValueHandler(String levelsSeparator) {
        this.levelsSeparator = levelsSeparator;
    }

    /**
     * Fetches mouse click details and processes single or multiple drop.
     *
     * @param textArea          Target: TextArea to move to.
     * @param mouseEventDetails Vaadin MouseEventDetails (with states of keyboard keys as well).
     * @param selectedItems     Set of items selected (being dropped).
     */
    protected void processDragAndDrop(TextArea textArea, MouseEventDetails mouseEventDetails, Set<TreeNode> selectedItems) {
        boolean logicalOperation = mouseEventDetails == null || !mouseEventDetails.isAltKey();
        boolean inversion = mouseEventDetails != null && mouseEventDetails.isShiftKey();
        int size = CollectionUtils.size(selectedItems);
        if (size == 1) {
            processDragAndDropSingle(textArea, logicalOperation, inversion, selectedItems.iterator().next());
        } else if (size > 1) {
            processDragAndDropMultiple(textArea, logicalOperation, inversion, selectedItems);
        }
    }

    /**
     * Processes drop of multiple items.
     *
     * @param textArea         Target: TextArea to move to.
     * @param logicalOperation true for AND, false for OR.
     * @param inversion        true for NOT
     * @param items            Items to drop (always values).
     */
    private void processDragAndDropMultiple(TextArea textArea, boolean logicalOperation, boolean inversion, Set<TreeNode> items) {
        String condition = CONDITIONS.get(logicalOperation);
        StringBuilder query = new StringBuilder(textArea.getValue());
        if (StringUtils.isNoneEmpty(query.toString())) {
            query.append(condition);
        }
        query.append(datasetPrefix).append(levelsSeparator);
        TreeNode firstItem = items.iterator().next();
        String path = firstItem.getPath();
        String queryTerm = path.substring(0, path.lastIndexOf(levelsSeparator));
        query.append(queryTerm).append(inversion ? " NOT IN (" : " IN (");
        for (TreeNode item : items) {
            String value = getValue(item);
            query.append(value).append(", ");
        }
        query = new StringBuilder(query.subSequence(0, query.length() - 2) + ")\n");
        textArea.setValue(query.toString());
    }

    /**
     * Processes drop of single item.
     *
     * @param textArea         Target: TextArea to move to.
     * @param logicalOperation true for AND, false for OR.
     * @param inversion        true for NOT
     * @param item             Item to drop (either attribute or value).
     */
    private void processDragAndDropSingle(TextArea textArea, boolean logicalOperation, boolean inversion, TreeNode item) {
        String condition = CONDITIONS.get(logicalOperation);
        String query = textArea.getValue();
        if (StringUtils.isNoneEmpty(query)) {
            query += condition;
        }
        if (inversion) {
            query += "NOT ";
        }
        query += datasetPrefix + levelsSeparator;
        String path = item.getPath();
        if (item.isValue()) {
            String value = getValue(item);
            query += path.substring(0, path.lastIndexOf(levelsSeparator)) + EQUALITY_OPERATOR + value;
        } else {
            query += path + EQUALITY_OPERATOR;
        }
        query += "\n";
        textArea.setValue(query);
    }

    protected String getValue(TreeNode item) {
        return "'" + item.toString() + "'";
    }

    public void setDatasetPrefix(String datasetPrefix) {
        this.datasetPrefix = datasetPrefix;
    }

}
