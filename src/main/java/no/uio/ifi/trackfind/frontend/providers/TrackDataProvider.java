package no.uio.ifi.trackfind.frontend.providers;

import com.vaadin.data.provider.AbstractHierarchicalDataProvider;
import com.vaadin.data.provider.HierarchicalQuery;
import no.uio.ifi.trackfind.frontend.data.TreeNode;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Front-end DataProvider for Vaadin Tree (not to be confused with back-end DataProvider).
 *
 * @author Dmytro Titov
 */
public class TrackDataProvider extends AbstractHierarchicalDataProvider<TreeNode, Predicate<? super TreeNode>> {

    private TreeNode root;
    private String attributesFilter = "";
    private String valuesFilter = "";
    private Map<String, Integer> childCountMap = new HashMap<>();

    /**
     * Constructor that accepts tree root.
     *
     * @param root root node of TreeNodes hierarchy.
     */
    public TrackDataProvider(TreeNode root) {
        this.root = root;
    }

    /**
     * Sets expression for filtering attributes in tree (last-level).
     *
     * @param attributesFilter String expression to filter final attributes in tree.
     */
    public void setAttributesFilter(String attributesFilter) {
        this.attributesFilter = attributesFilter.toLowerCase();
    }

    /**
     * Sets expression for filtering values in tree.
     *
     * @param valuesFilter String expression to filter values in tree.
     */
    public void setValuesFilter(String valuesFilter) {
        this.valuesFilter = valuesFilter.toLowerCase();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getChildCount(HierarchicalQuery<TreeNode, Predicate<? super TreeNode>> query) {
        return (int) fetchChildren(query).count();
    }

    /**
     * Gets the number of children for specified TreeNode.
     *
     * @param treeNode Specified TreeNode.
     * @return Number of children.
     */
    private int getChildCount(TreeNode treeNode) {
        if (StringUtils.isEmpty(attributesFilter) && StringUtils.isEmpty(valuesFilter)) {
            return childCountMap.computeIfAbsent(treeNode.getPath(), k -> getChildCount(treeNode.getQuery()));
        }
        return getChildCount(treeNode.getQuery());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<TreeNode> fetchChildren(HierarchicalQuery<TreeNode, Predicate<? super TreeNode>> query) {
        TreeNode parent = query.getParentOptional().orElse(root);
        Collection<TreeNode> children = new HashSet<>(parent.fetchChildren());
        Iterator<TreeNode> iterator = children.iterator();
        while (iterator.hasNext()) {
            TreeNode child = iterator.next();
            int childCount = getChildCount(child);
            boolean leaf = child.isLeaf();
            String attributeOrValue = child.toString().toLowerCase();
            if (childCount == 0 && !attributeOrValue.contains(valuesFilter)) {
                iterator.remove();
            } else if (leaf && !attributeOrValue.contains(valuesFilter)) {
                iterator.remove();
            } else if (child.isFinalAttribute() && !attributeOrValue.contains(attributesFilter)) {
                iterator.remove();
            } else if (!leaf && childCount == 0) {
                iterator.remove();
            }
        }
        return children.parallelStream().
                filter(query.getFilter().orElse(tr -> true)).
                filter(tn -> (getChildCount(tn) != 0) || tn.toString().toLowerCase().contains(valuesFilter)).
                sorted().
                skip(query.getOffset()).
                limit(query.getLimit());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasChildren(TreeNode item) {
        return getChildCount(item) != 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInMemory() {
        return true;
    }

}
