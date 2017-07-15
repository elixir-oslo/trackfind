package no.uio.ifi.trackfind.frontend.listeners;

import com.vaadin.ui.Tree;
import no.uio.ifi.trackfind.frontend.data.TreeNode;

public class TreeItemClickListener implements Tree.ItemClickListener<TreeNode> {

    private Tree<TreeNode> tree;

    public TreeItemClickListener(Tree<TreeNode> tree) {
        this.tree = tree;
    }

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
