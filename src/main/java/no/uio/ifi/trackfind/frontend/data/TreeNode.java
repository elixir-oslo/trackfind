package no.uio.ifi.trackfind.frontend.data;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

public class TreeNode implements Comparable<TreeNode> {

    private Map.Entry<String, Object> node;

    public TreeNode(Map<String, Object> root) {
        this.node = new AbstractMap.SimpleEntry<>(null, root);
    }

    private TreeNode(Map.Entry<String, Object> node) {
        this.node = node;
    }

    @SuppressWarnings("unchecked")
    public Stream<TreeNode> fetchChildren() {
        Object value = node.getValue();
        if (value instanceof Map) {
            return ((Map<String, Object>) value).entrySet().stream().map(TreeNode::new);
        } else if (value instanceof Collection) {
            return ((Collection<String>) value).stream().map(s -> new AbstractMap.SimpleEntry<>(s, null)).map(TreeNode::new);
        } else {
            return Stream.empty();
        }
    }

    public boolean hasChildren() {
        return node.getValue() != null;
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
