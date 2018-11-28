package no.uio.ifi.trackfind.backend.data;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collection;

/**
 * Data type for representing metadata element (attribute or value) on front-end.
 *
 * @author Dmytro Titov
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TreeNode implements Comparable<TreeNode> {

    private String value;
    private boolean attribute;
    private boolean fin;
    private int level;
    private String separator;
    private TreeNode parent;
    private Collection<String> children;

    public String getPath() {
        StringBuilder path = new StringBuilder(toString());
        TreeNode parent = getParent();
        while (parent != null) {
            path.insert(0, parent.toString() + separator);
            parent = parent.getParent();
        }
        return path.toString();
    }

    @EqualsAndHashCode.Include
    public String getSQLPath() {
        StringBuilder path = new StringBuilder("'" + toString() + "'");
        TreeNode parent;
        while ((parent = getParent()) != null) {
            path.insert(0, "'" + parent.toString() + "'" + separator);
        }
        return path.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(TreeNode that) {
        return this.getPath().compareTo(that.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return value;
    }

}
