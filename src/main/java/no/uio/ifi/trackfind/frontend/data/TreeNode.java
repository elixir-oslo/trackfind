package no.uio.ifi.trackfind.frontend.data;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

public class TreeNode implements Comparable<TreeNode> {

    private TreeNode parent;
    private Map.Entry<String, Object> node;

    public TreeNode(Map<String, Object> root) {
        this.parent = null;
        this.node = new AbstractMap.SimpleEntry<>(null, root);
    }

    private TreeNode(TreeNode parent, Map.Entry<String, Object> node) {
        this.parent = parent;
        this.node = node;
    }

    public TreeNode getParent() {
        return parent;
    }

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

    public boolean isLeaf() {
        return node.getValue() == null;
    }

    @Override
    public int compareTo(TreeNode that) {
        return String.valueOf(this).compareTo(String.valueOf(that));
    }

    @Override
    public String toString() {
        return node.getKey();
    }

}
