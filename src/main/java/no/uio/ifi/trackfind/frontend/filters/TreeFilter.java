package no.uio.ifi.trackfind.frontend.filters;

import com.vaadin.server.SerializablePredicate;
import lombok.AllArgsConstructor;
import lombok.Data;
import no.uio.ifi.trackfind.backend.data.TreeNode;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import org.apache.commons.collections.CollectionUtils;

/**
 * Filter for the Vaadin tree.
 */
@AllArgsConstructor
@Data
public class TreeFilter implements SerializablePredicate<TreeNode> {

    private TfHub hub;
    private String valuesFilter;
    private String attributesFilter;

    @Override
    public boolean test(TreeNode treeNode) {
        return treeNode.isAttribute() || treeNode.getValue().toLowerCase().contains(valuesFilter.toLowerCase());
    }

}
