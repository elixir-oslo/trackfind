package no.uio.ifi.trackfind.frontend.listeners;

import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.dnd.event.DropEvent;
import com.vaadin.ui.dnd.event.DropListener;
import no.uio.ifi.trackfind.frontend.data.TreeNode;

import java.util.Optional;
import java.util.Set;

/**
 * Listener for Drop event bind to Vaadin TextArea.
 *
 * @author Dmytro Titov
 */
public class TextAreaDropListener extends MoveAttributeValueHandler implements DropListener<TextArea> {

    private TextArea textArea;

    /**
     * Constructor with binding to TextArea.
     *
     * @param textArea        TextArea to handle drops for.
     * @param levelsSeparator Levels separator.
     */
    public TextAreaDropListener(TextArea textArea, String levelsSeparator) {
        super(levelsSeparator);
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
            processDragAndDrop(textArea, event.getMouseEventDetails(), selectedItems);
        }
    }

}
