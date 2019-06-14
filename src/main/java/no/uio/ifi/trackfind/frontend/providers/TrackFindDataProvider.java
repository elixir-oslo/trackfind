package no.uio.ifi.trackfind.frontend.providers;

import com.vaadin.data.provider.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.server.SerializablePredicate;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import no.uio.ifi.trackfind.backend.data.TreeNode;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import no.uio.ifi.trackfind.backend.services.MetamodelService;
import no.uio.ifi.trackfind.frontend.filters.TreeFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TrackFindDataProvider extends AbstractBackEndHierarchicalDataProvider<TreeNode, SerializablePredicate<TreeNode>> {

    private TrackFindProperties properties;
    private MetamodelService metamodelService;

    /**
     * {@inheritDoc}
     */
    @Override
    protected Stream<TreeNode> fetchChildrenFromBackEnd(HierarchicalQuery<TreeNode, SerializablePredicate<TreeNode>> query) {
        String levelsSeparator = properties.getLevelsSeparator();
        TreeFilter treeFilter = (TreeFilter) query.getFilter().orElseThrow(RuntimeException::new);
        TfHub hub = treeFilter.getHub();
        String repository = hub.getRepository();
        String hubName = hub.getName();
        Map<String, Map<String, Object>> metamodelTree = metamodelService.getMetamodelTree(repository, hubName);
        Optional<TreeNode> parentOptional = query.getParentOptional();
        if (!parentOptional.isPresent()) {
            return metamodelTree.keySet().stream().map(c -> {
                TreeNode treeNode = new TreeNode();
                treeNode.setTreeFilter(treeFilter);
                treeNode.setCategory(c);
                treeNode.setValue(c);
                treeNode.setParent(null);
                treeNode.setSeparator(levelsSeparator);
                treeNode.setLevel(0);
                treeNode.setChildren(metamodelTree.get(c).keySet());
                treeNode.setAttribute(true);
                treeNode.setArray(false);
                treeNode.setType("string");
                return treeNode;
            }).filter(treeFilter).sorted();
        } else {
            TreeNode parent = parentOptional.get();
            if (!parent.isAttribute()) {
                return Stream.empty();
            }
            String category = parent.getCategory();
            Collection<String> attributes = metamodelService.getAttributesFlat(repository, hubName, category, null);
            Collection<String> arrayOfObjectsAttributes = metamodelService.getArrayOfObjectsAttributes(repository, hubName, category);
            Map<String, String> attributeTypes = metamodelService.getAttributeTypes(repository, hubName, category);
            Collection<String> children = parent.getChildren();
            String prefix = category + levelsSeparator;
            return children.stream().map(c -> {
                TreeNode treeNode = new TreeNode();
                treeNode.setTreeFilter(treeFilter);
                treeNode.setCategory(category);
                treeNode.setValue(c);
                treeNode.setParent(parent);
                treeNode.setSeparator(levelsSeparator);
                treeNode.setLevel(parent.getLevel() + 1);
                String path = treeNode.getPath().replace(prefix, "");
                treeNode.setAttribute(attributes.parallelStream().anyMatch(a -> a.startsWith(path)));
                treeNode.setArray(arrayOfObjectsAttributes.contains(path));
                treeNode.setType(attributeTypes.get(path));
                if (treeNode.isAttribute()) {
                    if (attributes.contains(path)) {
                        Collection<String> values = metamodelService.getValues(repository, hubName, category, path);
                        treeNode.setChildren(values.stream().filter(v -> v.toLowerCase().contains(treeFilter.getValuesFilter().toLowerCase())).collect(Collectors.toSet()));
                    } else {
                        Collection<String> subAttributes = metamodelService.getAttributesFlat(repository, hubName, category, path);
                        treeNode.setChildren(subAttributes);
                    }
                }
                return treeNode;
            }).filter(treeFilter).sorted();
        }
    }

    @Override
    public int getChildCount(HierarchicalQuery<TreeNode, SerializablePredicate<TreeNode>> query) {
        return (int) fetchChildrenFromBackEnd(query).count();
    }

    @Override
    public boolean hasChildren(TreeNode item) {
        return getChildCount(new HierarchicalQuery<>(item.getTreeFilter(), item)) != 0;
    }

    @Autowired
    public void setProperties(TrackFindProperties properties) {
        this.properties = properties;
    }

    @Autowired
    public void setMetamodelService(MetamodelService metamodelService) {
        this.metamodelService = metamodelService;
    }

}
