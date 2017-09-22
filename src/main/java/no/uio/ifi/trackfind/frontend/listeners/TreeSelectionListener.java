package no.uio.ifi.trackfind.frontend.listeners;

import com.vaadin.event.selection.MultiSelectionEvent;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.event.selection.SingleSelectionEvent;
import com.vaadin.ui.Tree;
import no.uio.ifi.trackfind.frontend.components.KeyboardInterceptorExtension;
import no.uio.ifi.trackfind.frontend.data.TreeNode;
import org.apache.commons.collections4.MapUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Vaadin Tree click listener for implementing multiple selection and some other rules.
 *
 * @author Dmytro Titov
 */
// TODO: Doesn't work, fix it.
public class TreeSelectionListener implements SelectionListener<TreeNode> {

    private Tree<TreeNode> tree;
    private KeyboardInterceptorExtension keyboardInterceptorExtension;

    /**
     * Constructor that injects Tree and KeyboardInterceptorExtension to the instance.
     *
     * @param tree                         Vaadin Tree.
     * @param keyboardInterceptorExtension KeyboardInterceptorExtension.
     */
    public TreeSelectionListener(Tree<TreeNode> tree, KeyboardInterceptorExtension keyboardInterceptorExtension) {
        this.tree = tree;
        this.keyboardInterceptorExtension = keyboardInterceptorExtension;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked", "PMD.NPathComplexity"})
    @Override
    public void selectionChange(SelectionEvent<TreeNode> event) {
        Set<TreeNode> selectedItems = event.getAllSelectedItems();
        if (event instanceof SingleSelectionEvent) {
            return;
        }
        MultiSelectionEvent<TreeNode> multiSelectionEvent = (MultiSelectionEvent<TreeNode>) event;
        if (!multiSelectionEvent.isUserOriginated()) {
            return;
        }
        Set<TreeNode> addedSelection = multiSelectionEvent.getAddedSelection();
        if (CollectionUtils.isEmpty(addedSelection)) {
            return;
        }
        TreeNode current = addedSelection.iterator().next();
        if (!current.isFinalAttribute() && !current.isValue()) {
            tree.deselect(current);
            return;
        }
        if (current.isFinalAttribute()) {
            selectedItems.stream().filter(tn -> !tn.equals(current)).forEach(tree::deselect);
            return;
        }
        selectedItems.stream().filter(TreeNode::isFinalAttribute).forEach(tree::deselect);
        selectedItems.stream().filter(tn -> tn.getLevel() != current.getLevel() || tn.getParent() != current.getParent()).forEach(tree::deselect);
        if (!keyboardInterceptorExtension.isControlKeyDown() &&
                !keyboardInterceptorExtension.isMetaKeyDown() &&
                !keyboardInterceptorExtension.isShiftKeyDown()) {
            selectedItems.stream().filter(tn -> !tn.equals(current)).forEach(tree::deselect);
        } else if (keyboardInterceptorExtension.isShiftKeyDown()) {
            List<TreeNode> oldSelection = multiSelectionEvent.getOldSelection().stream().sorted().collect(Collectors.toList());
            if (oldSelection.stream().anyMatch(tn -> tn.getParent() != current.getParent())) {
                return;
            }
            TreeNode first = oldSelection.get(0);
            TreeNode last = oldSelection.get(oldSelection.size() - 1);
            final int[] index = {0};
            Map<TreeNode, Integer> indexedSiblings =
                    current.getParent().getChildren().stream().sorted().collect(Collectors.toMap(Function.identity(), tn -> index[0]++));
            int firstIndex = indexedSiblings.get(first);
            int lastIndex = indexedSiblings.get(last);
            int currentIndex = indexedSiblings.get(current);
            List<Integer> indices = Arrays.asList(firstIndex, lastIndex, currentIndex);
            Optional<Integer> optionalMin = indices.stream().min(Integer::compareTo);
            Optional<Integer> optionalMax = indices.stream().max(Integer::compareTo);
            if (!optionalMin.isPresent() || !optionalMax.isPresent()) {
                return;
            }
            int min = optionalMin.get();
            int max = optionalMax.get();
            Map<Integer, TreeNode> invertedMap = MapUtils.invertMap(indexedSiblings);
            for (int i = min; i <= max; i++) {
                tree.select(invertedMap.get(i));
            }
        }
    }

}
