package no.uio.ifi.trackfind.frontend.data;

import com.vaadin.data.provider.HierarchicalQuery;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Data type for representing metadata element (attribute or value) on front-end.
 *
 * @author Dmytro Titov
 */
public class TreeNode implements Comparable<TreeNode> {

    private int level;
    private TreeNode parent;
    private Map.Entry<String, Object> node;
    private HierarchicalQuery<TreeNode, Predicate<? super TreeNode>> query;
    private Collection<TreeNode> children;

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
     * Gets query for Vaadin DataProvider.
     *
     * @return Vaadin's HierarchicalQuery.
     */
    public HierarchicalQuery<TreeNode, Predicate<? super TreeNode>> getQuery() {
        if (query == null) { // double checked synchronization
            synchronized (this) {
                if (query == null) {
                    query = getQueryInternally();
                }
            }
        }
        return query;
    }

    /**
     * Gets query for Vaadin DataProvider.
     *
     * @return Vaadin's HierarchicalQuery.
     */
    private HierarchicalQuery<TreeNode, Predicate<? super TreeNode>> getQueryInternally() {
        return new HierarchicalQuery<>(null, this);
    }

    /**
     * Fetches children of current node.
     *
     * @return Current node's children (attributes or values).
     */
    public Collection<TreeNode> fetchChildren() {
        if (children == null) { // double checked synchronization
            synchronized (this) {
                if (children == null) {
                    children = Collections.unmodifiableCollection(fetchChildrenInternally());
                }
            }
        }
        return children;
    }

    /**
     * Fetches children of current node.
     *
     * @return Current node's children (attributes or values).
     */
    @SuppressWarnings("unchecked")
    private Collection<TreeNode> fetchChildrenInternally() {
        Object value = node.getValue();
        if (value instanceof Map) {
            return ((Map<String, Object>) value).entrySet().parallelStream().map(e -> new TreeNode(this, e)).collect(Collectors.toSet());
        } else if (value instanceof Collection) {
            return ((Collection<String>) value).parallelStream().map(s -> new AbstractMap.SimpleEntry<>(s, null)).map(e -> new TreeNode(this, e)).collect(Collectors.toSet());
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * Gets path from the root node to current node.
     *
     * @return Sequence of attributes separated by some delimiter ("&gt;" by default).
     */
    public String getPath() {
        StringBuilder path = new StringBuilder();
        TreeNode parent = getParent();
        while (parent != null) {
            path.insert(0, ">" + parent.toString());
            parent = parent.getParent();
        }
        if (isLeaf()) {
            return path.toString().replace(">null>", "") + ": " + toString();
        } else {
            return (path.toString().replace(">null", "") + ">" + toString() + ": ").substring(1);
        }
    }

    /**
     * Checks if it's a leaf-node (value of the metamodel).
     *
     * @return <code>true</code> if it's a leaf-node, <code>false</code> otherwise.
     */
    public boolean isLeaf() {
        return node.getValue() == null;
    }

    /**
     * Checks if it's a final attribute (containing no children attributes, but values only).
     *
     * @return <code>true</code> if it's a final attribute, <code>false</code> otherwise.
     */
    public boolean isFinalAttribute() {
        return fetchChildren().parallelStream().anyMatch(TreeNode::isLeaf);
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
