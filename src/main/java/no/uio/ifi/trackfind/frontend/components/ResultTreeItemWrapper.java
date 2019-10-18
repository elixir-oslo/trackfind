package no.uio.ifi.trackfind.frontend.components;

/**
 * Tree node for a results tree on main UI: holds the value itself and indicates whether it's a leaf node.
 */
public class ResultTreeItemWrapper {

    private String value;
    private boolean leaf;

    public ResultTreeItemWrapper(String value, boolean leaf) {
        this.value = value;
        this.leaf = leaf;
    }

    public String getValue() {
        return value;
    }

    public boolean isLeaf() {
        return leaf;
    }

}
