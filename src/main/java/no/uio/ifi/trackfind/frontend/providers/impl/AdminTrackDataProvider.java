package no.uio.ifi.trackfind.frontend.providers.impl;

import com.vaadin.data.provider.HierarchicalQuery;
import no.uio.ifi.trackfind.frontend.data.TreeNode;
import no.uio.ifi.trackfind.frontend.providers.TrackDataProvider;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Front-end DataProvider for Vaadin Tree (not to be confused with back-end DataProvider) on admin UI.
 *
 * @author Dmytro Titov
 */
public class AdminTrackDataProvider extends TrackDataProvider {

    private TreeNode root;
    private Map<String, Integer> childCountMap = new HashMap<>();

    /**
     * Constructor that accepts tree root.
     *
     * @param root root node of TreeNodes hierarchy.
     */
    public AdminTrackDataProvider(TreeNode root) {
        this.root = root;
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
        if (StringUtils.isEmpty(attributesFilter)) {
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
            String attributeOrValue = child.toString().toLowerCase();
            if (child.isValue()) {
                iterator.remove();
            } else if (getChildCount(child) == 0 && !child.isFinalAttribute()) {
                iterator.remove();
            } else if (child.isFinalAttribute() && !attributeOrValue.contains(attributesFilter)) {
                iterator.remove();
            }
        }
        return children.parallelStream().
                filter(query.getFilter().orElse(tr -> true)).
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
