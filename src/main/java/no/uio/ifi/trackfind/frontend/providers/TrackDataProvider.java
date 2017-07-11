package no.uio.ifi.trackfind.frontend.providers;

import com.vaadin.data.provider.AbstractHierarchicalDataProvider;
import com.vaadin.data.provider.HierarchicalQuery;
import no.uio.ifi.trackfind.frontend.data.TreeNode;

import java.util.function.Predicate;
import java.util.stream.Stream;

public class TrackDataProvider extends AbstractHierarchicalDataProvider<TreeNode, Predicate<? super TreeNode>> {

    private TreeNode root;
    private String valuesFilter = "";

    public TrackDataProvider(TreeNode root) {
        this.root = root;
    }

    @Override
    public int getChildCount(HierarchicalQuery<TreeNode, Predicate<? super TreeNode>> query) {
        return (int) fetchChildren(query).count();
    }

    @Override
    public Stream<TreeNode> fetchChildren(HierarchicalQuery<TreeNode, Predicate<? super TreeNode>> query) {
        TreeNode parent = query.getParentOptional().orElse(root);
        return parent.fetchChildren()
                .filter(query.getFilter().orElse(tr -> true))
                .filter(tn -> (getChildCount(getQuery(tn)) != 0) || tn.toString().toLowerCase().contains(valuesFilter))
                .sorted()
                .skip(query.getOffset())
                .limit(query.getLimit());
    }

    @Override
    public boolean hasChildren(TreeNode item) {
        return getChildCount(getQuery(item)) != 0;
    }

    private HierarchicalQuery<TreeNode, Predicate<? super TreeNode>> getQuery(TreeNode item) {
        return new HierarchicalQuery<>(null, item);
    }

    @Override
    public boolean isInMemory() {
        return true;
    }

    public void setValuesFilter(String valuesFilter) {
        this.valuesFilter = valuesFilter.toLowerCase();
    }

}
