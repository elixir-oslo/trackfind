package no.uio.ifi.trackfind.backend.data.filters;

import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.server.SerializablePredicate;
import lombok.AllArgsConstructor;
import lombok.Data;
import no.uio.ifi.trackfind.backend.data.TreeNode;
import org.apache.commons.lang3.StringUtils;

@AllArgsConstructor
@Data
public class TreeFilter implements SerializablePredicate<TreeNode> {

    private TreeDataProvider<TreeNode> dataProvider;
    private String valuesFilter;
    private String attributesFilter;

    @Override
    public boolean test(TreeNode treeNode) {
        String attributeOrValue = treeNode.toString().toLowerCase();
        if (!treeNode.isAttribute() && !StringUtils.containsIgnoreCase(attributeOrValue, valuesFilter)) {
            return false;
        } else if (treeNode.isFin() && !StringUtils.containsIgnoreCase(attributeOrValue, attributesFilter)) {
            return false;
        } else if (treeNode.isAttribute() && dataProvider.getChildCount(new HierarchicalQuery<>(null, treeNode)) == 0) {
            return false;
        }
        return true;
    }

}