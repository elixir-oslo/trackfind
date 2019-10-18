package no.uio.ifi.trackfind.frontend.components;

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

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public void setLeaf(boolean leaf) {
        this.leaf = leaf;
    }

}
