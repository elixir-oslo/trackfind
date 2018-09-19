package no.uio.ifi.trackfind.frontend.filters;

import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.server.SerializablePredicate;
import lombok.AllArgsConstructor;
import lombok.Data;
import no.uio.ifi.trackfind.frontend.data.TreeNode;
import org.apache.commons.lang3.StringUtils;

@AllArgsConstructor
@Data
public class AdminTreeFilter implements SerializablePredicate<TreeNode> {

    private TreeDataProvider<TreeNode> dataProvider;
    private String attributesFilter;

    @Override
    public boolean test(TreeNode treeNode) {
        String attributeOrValue = treeNode.toString().toLowerCase();
        if (treeNode.isValue()) {
            return false;
        } else if (treeNode.isFinalAttribute() && !StringUtils.containsIgnoreCase(attributeOrValue, attributesFilter)) {
            return false;
        }
        return true;
    }

}
