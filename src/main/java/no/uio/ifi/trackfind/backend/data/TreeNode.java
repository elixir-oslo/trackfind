package no.uio.ifi.trackfind.backend.data;

import lombok.*;
import no.uio.ifi.trackfind.frontend.filters.TreeFilter;

import java.util.Collection;

/**
 * Data type for representing metadata element (attribute or value) on front-end.
 *
 * @author Dmytro Titov
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TreeNode implements Comparable<TreeNode> {

    private TreeFilter treeFilter;
    private String category;
    private String value;
    private boolean attribute;
    private boolean standard;
    private boolean array;
    private String type;
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
        if (parent == null) {
            return toString();
        }
        return parent.getPath() + separator + toString();
    }

    /**
     * Gets SQL path from the root node to current node.
     *
     * @return Sequence of attributes separated by some delimiter.
     */
    public String getSQLPath() {
        if (parent == null) {
            return toString() + ".content";
        }
        return parent.getSQLPath() + (array ? separator + "*" : "") + separator + "'" + toString() + "'";
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
