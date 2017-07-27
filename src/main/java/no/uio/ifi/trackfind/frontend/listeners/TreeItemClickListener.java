package no.uio.ifi.trackfind.frontend.listeners;

import com.vaadin.ui.Tree;
import no.uio.ifi.trackfind.frontend.data.TreeNode;

/**
 * Vaadin Tree click listener for implementing collapse/expand on double-click.
 */
public class TreeItemClickListener implements Tree.ItemClickListener<TreeNode> {

    private Tree<TreeNode> tree;

    /**
     * Constructor binding listener to Tree.
     *
     * @param tree Vaadin Tree.
     */
    public TreeItemClickListener(Tree<TreeNode> tree) {
        this.tree = tree;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void itemClick(Tree.ItemClick<TreeNode> event) {
        TreeNode item = event.getItem();
        if (event.getMouseEventDetails().isDoubleClick()) {
            if (tree.isExpanded(item)) {
                tree.collapse(item);
            } else {
                tree.expand(item);
            }
        }
    }

}
