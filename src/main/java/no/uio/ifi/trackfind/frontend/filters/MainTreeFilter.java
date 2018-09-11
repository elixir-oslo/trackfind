package no.uio.ifi.trackfind.frontend.filters;

import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.server.SerializablePredicate;
import lombok.AllArgsConstructor;
import lombok.Data;
import no.uio.ifi.trackfind.frontend.data.TreeNode;
import org.apache.commons.lang3.StringUtils;

@AllArgsConstructor
@Data
public class MainTreeFilter implements SerializablePredicate<TreeNode> {

    private TreeDataProvider<TreeNode> dataProvider;
    private String valuesFilter;
    private String attributesFilter;

    @Override
    public boolean test(TreeNode treeNode) {
        String attributeOrValue = treeNode.toString().toLowerCase();
        if (treeNode.isValue() && !StringUtils.containsIgnoreCase(attributeOrValue, valuesFilter)) {
            return false;
        } else if (treeNode.isFinalAttribute() && !StringUtils.containsIgnoreCase(attributeOrValue, attributesFilter)) {
            return false;
        } else if (!treeNode.isValue() && dataProvider.getChildCount(treeNode.getQuery()) == 0) {
            return false;
        }
        return true;
    }

}
