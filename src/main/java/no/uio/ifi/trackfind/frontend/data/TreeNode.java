package no.uio.ifi.trackfind.frontend.data;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Data type for representing metadata element (attribute or value) on front-end.
 */
public class TreeNode implements Comparable<TreeNode> {

    private int level;
    private TreeNode parent;
    private Map.Entry<String, Object> node;

    /**
     * Constructs TreeNode instance of root element, setting level to zero and parent tu null.
     *
     * @param root Root element of the metadata hierarchy.
     */
    public TreeNode(Map<String, Object> root) {
        this.level = 0;
        this.parent = null;
        this.node = new AbstractMap.SimpleEntry<>(null, root);
    }

    /**
     * Constructs TreeNode for some child element, accepting it and its parent.
     *
     * @param parent Parent TreeNode for this child element.
     * @param node   Child element.
     */
    private TreeNode(TreeNode parent, Map.Entry<String, Object> node) {
        this.parent = parent;
        this.node = node;
        this.level = parent.getLevel() + 1;
    }

    /**
     * Gets level of current node in the metamodel hierarchy.
     *
     * @return Level of current node.
     */
    public int getLevel() {
        return level;
    }

    /**
     * Gets parent node for current node.
     *
     * @return Parent TreeNode.
     */
    public TreeNode getParent() {
        return parent;
    }

    /**
     * Fetches children of current node.
     *
     * @return Current node's children (attributes or values).
     */
    @SuppressWarnings("unchecked")
    public Stream<TreeNode> fetchChildren() {
        Object value = node.getValue();
        if (value instanceof Map) {
            return ((Map<String, Object>) value).entrySet().stream().map(e -> new TreeNode(this, e));
        } else if (value instanceof Collection) {
            return ((Collection<String>) value).stream().map(s -> new AbstractMap.SimpleEntry<>(s, null)).map(e -> new TreeNode(this, e));
        } else {
            return Stream.empty();
        }
    }

    /**
     * Gets path from the root node to current node.
     *
     * @return Sequence of attributes separated by some delimiter ("&gt;" by default).
     */
    public String getPath() {
        String path = "";
        TreeNode parent = getParent();
        while (parent != null) {
            path = ">" + parent.toString() + path;
            parent = parent.getParent();
        }
        if (isLeaf()) {
            return path.replace(">null>", "") + ": " + toString();
        } else {
            return (path.replace(">null", "") + ">" + toString() + ": ").substring(1);
        }
    }

    /**
     * Checks if it's a leaf-node (value of the metamodel).
     *
     * @return true if it's a leaf-node, false otherwise.
     */
    public boolean isLeaf() {
        return node.getValue() == null;
    }

    /**
     * Checks if it's a final attribute (containing no children attributes, but values only).
     *
     * @return true if it's a final attribute, false otherwise.
     */
    public boolean isFinalAttribute() {
        return fetchChildren().anyMatch(TreeNode::isLeaf);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(TreeNode that) {
        return String.valueOf(this).compareTo(String.valueOf(that));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return node.getKey();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof TreeNode && this.toString().equals(obj.toString());
    }

}
