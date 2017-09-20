package no.uio.ifi.trackfind.frontend.providers;

import com.vaadin.data.provider.AbstractHierarchicalDataProvider;
import no.uio.ifi.trackfind.frontend.data.TreeNode;

import java.util.function.Predicate;

/**
 * Abstract front-end DataProvider for Vaadin Tree (not to be confused with back-end DataProvider).
 *
 * @author Dmytro Titov
 */
// TODO: Migrate to TreeDataProvider.
public abstract class TrackDataProvider extends AbstractHierarchicalDataProvider<TreeNode, Predicate<? super TreeNode>> {

    protected String attributesFilter = "";
    protected String valuesFilter = "";

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

}
