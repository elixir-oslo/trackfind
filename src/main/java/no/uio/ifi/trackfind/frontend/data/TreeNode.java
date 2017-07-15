package no.uio.ifi.trackfind.frontend.data;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

public class TreeNode implements Comparable<TreeNode> {

    private int level;
    private TreeNode parent;
    private Map.Entry<String, Object> node;

    public TreeNode(Map<String, Object> root) {
        this.level = 0;
        this.parent = null;
        this.node = new AbstractMap.SimpleEntry<>(null, root);
    }

    private TreeNode(TreeNode parent, Map.Entry<String, Object> node) {
        this.parent = parent;
        this.node = node;
        this.level = parent.getLevel() + 1;
    }

    public int getLevel() {
        return level;
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

    public boolean isLeaf() {
        return node.getValue() == null;
    }

    public boolean isFinalAttribute() {
        return fetchChildren().anyMatch(TreeNode::isLeaf);
    }

    @Override
    public int compareTo(TreeNode that) {
        return String.valueOf(this).compareTo(String.valueOf(that));
    }

    @Override
    public String toString() {
        return node.getKey();
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TreeNode && toString().equals(obj.toString());
    }

}