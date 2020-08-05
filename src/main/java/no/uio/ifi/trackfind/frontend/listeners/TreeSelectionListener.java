package no.uio.ifi.trackfind.frontend.listeners;

import com.vaadin.data.provider.HierarchicalDataProvider;
import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.event.selection.MultiSelectionEvent;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.event.selection.SingleSelectionEvent;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.ui.Button;
import no.uio.ifi.trackfind.backend.data.TreeNode;
import no.uio.ifi.trackfind.frontend.components.KeyboardInterceptorExtension;
import no.uio.ifi.trackfind.frontend.components.TrackFindTree;
import no.uio.ifi.trackfind.frontend.filters.TreeFilter;
import org.springframework.util.CollectionUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Vaadin Tree click listener for implementing multiple selection and some other rules.
 *
 * @author Dmytro Titov
 */
public class TreeSelectionListener implements SelectionListener<TreeNode> {

    private TrackFindTree<TreeNode> tree;
    private TreeFilter filter;
    private Button copyButton;
    private Button visitButton;
    private Button addToQueryButton;
    private KeyboardInterceptorExtension keyboardInterceptorExtension;

    /**
     * Constructor that injects Tree and KeyboardInterceptorExtension to the instance.
     *
     * @param tree                         Vaadin Tree.
     * @param copyButton                   Copies selected tree element.
     * @param visitButton                  Visits the link in the selected tree element.
     * @param addToQueryButton             Adds selected tree element to the query.
     * @param keyboardInterceptorExtension KeyboardInterceptorExtension.
     */
    public TreeSelectionListener(TrackFindTree<TreeNode> tree,
                                 TreeFilter filter,
                                 Button copyButton,
                                 Button visitButton,
                                 Button addToQueryButton,
                                 KeyboardInterceptorExtension keyboardInterceptorExtension) {
        this.tree = tree;
        this.filter = filter;
        this.copyButton = copyButton;
        this.visitButton = visitButton;
        this.addToQueryButton = addToQueryButton;
        this.keyboardInterceptorExtension = keyboardInterceptorExtension;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked", "PMD.NPathComplexity"})
    @Override
    public void selectionChange(SelectionEvent<TreeNode> event) {
        Set<TreeNode> selectedItems = event.getAllSelectedItems();
        boolean valueSelected = event.getAllSelectedItems().stream().anyMatch(n -> !n.isAttribute());
        boolean hasIcon = event.getAllSelectedItems().stream().anyMatch(n -> n.getIcon() != null);
        copyButton.setEnabled(valueSelected);
        visitButton.setEnabled(valueSelected && hasIcon);
        addToQueryButton.setEnabled(valueSelected);
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
        if (current.isAttribute()) {
            selectedItems.stream().filter(tn -> !tn.equals(current)).forEach(tree::deselect);
            return;
        }
        selectedItems.stream().filter(TreeNode::isAttribute).forEach(tree::deselect);
        selectedItems.stream().filter(tn -> tn.getLevel() != current.getLevel() || tn.getParent() != current.getParent()).forEach(tree::deselect);
        if (!keyboardInterceptorExtension.isControlKeyDown() &&
                !keyboardInterceptorExtension.isMetaKeyDown() &&
                !keyboardInterceptorExtension.isShiftKeyDown()) {
            selectedItems.stream().filter(tn -> !tn.equals(current)).forEach(tree::deselect);
        } else if (keyboardInterceptorExtension.isShiftKeyDown()) {
            TreeNode first = selectedItems.stream().sorted().findFirst().orElseThrow(RuntimeException::new);
            TreeNode last = selectedItems.stream().sorted().reduce((f, s) -> s).get();
            if (first.getLevel() != last.getLevel()) {
                return;
            }
            HierarchicalQuery<TreeNode, SerializablePredicate<TreeNode>> treeNodeFHierarchicalQuery = new HierarchicalQuery<>(filter, current.getParent());
            HierarchicalDataProvider<TreeNode, SerializablePredicate<TreeNode>> dataProvider = (HierarchicalDataProvider<TreeNode, SerializablePredicate<TreeNode>>) tree.getDataProvider();
            List<TreeNode> siblings = dataProvider.fetchChildren(treeNodeFHierarchicalQuery).sorted().collect(Collectors.toList());
            int firstIndex = Math.min(siblings.indexOf(first), siblings.indexOf(last));
            int lastIndex = Math.max(siblings.indexOf(first), siblings.indexOf(last));
            for (int i = 0; i < siblings.size(); i++) {
                if (i >= firstIndex && i <= lastIndex) {
                    tree.select(siblings.get(i));
                }
            }
        }
    }

}
