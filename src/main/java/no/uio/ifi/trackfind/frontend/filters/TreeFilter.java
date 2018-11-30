package no.uio.ifi.trackfind.frontend.filters;

import com.vaadin.server.SerializablePredicate;
import lombok.AllArgsConstructor;
import lombok.Data;
import no.uio.ifi.trackfind.backend.data.TreeNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

/**
 * Filter for the Vaadin tree.
 */
@AllArgsConstructor
@Data
public class TreeFilter implements SerializablePredicate<TreeNode> {

    private boolean raw;
    private String valuesFilter;
    private String attributesFilter;

    @Override
    public boolean test(TreeNode treeNode) {
        String attributeOrValue = treeNode.toString().toLowerCase();
        if (!treeNode.isAttribute() && !StringUtils.containsIgnoreCase(attributeOrValue, valuesFilter)) {
            return false;
        } else if (treeNode.isFin() && !StringUtils.containsIgnoreCase(attributeOrValue, attributesFilter)) {
            return false;
        } else if (treeNode.isAttribute() && CollectionUtils.isEmpty(treeNode.getChildren())) {
            return false;
        }
        return true;
    }

}
