package no.uio.ifi.trackfind.backend.data;

import lombok.*;

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
    private boolean array;
    private String type;
    private boolean hasValues;
    private int level;
    private String separator;
    private TreeNode parent;
    private Collection<String> children;

    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private String path;
    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private String sqlPath;

    /**
     * Gets path from the root node to current node.
     *
     * @return Sequence of attributes separated by some delimiter.
     */
    @EqualsAndHashCode.Include
    public String getPath() {
        if (path == null) { // double checked synchronization
            synchronized (this) {
                if (path == null) {
                    path = getPathInternally();
                }
            }
        }
        return path;
    }

    /**
     * Gets SQL path from the root node to current node.
     *
     * @return Sequence of attributes separated by some delimiter.
     */
    public String getSQLPath() {
        if (sqlPath == null) { // double checked synchronization
            synchronized (this) {
                if (sqlPath == null) {
                    sqlPath = getSQLPathInternally();
                }
            }
        }
        return sqlPath;
    }

    /**
     * Gets path from the root node to current node.
     *
     * @return Sequence of attributes separated by some delimiter.
     */
    private String getPathInternally() {
        StringBuilder path = new StringBuilder(toString());
        TreeNode parent = getParent();
        while (parent != null) {
            path.insert(0, parent.toString() + separator);
            parent = parent.getParent();
        }
        return path.toString();
    }

    /**
     * Gets SQL path from the root node to current node.
     *
     * @return Sequence of attributes separated by some delimiter.
     */
    private String getSQLPathInternally() {
        StringBuilder path = new StringBuilder("'" + toString() + "'");
        TreeNode parent = getParent();
        while (parent != null) {
            path.insert(0, "'" + parent.toString() + "'" + separator + (parent.isArray() ? "*->" : ""));
            parent = parent.getParent();
        }
        return path.toString() + (isArray() ? "*->" : "");
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
