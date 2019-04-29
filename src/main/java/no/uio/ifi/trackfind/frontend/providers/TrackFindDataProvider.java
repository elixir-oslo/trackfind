package no.uio.ifi.trackfind.frontend.providers;

import com.vaadin.data.provider.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.server.SerializablePredicate;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import no.uio.ifi.trackfind.backend.data.TreeNode;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import no.uio.ifi.trackfind.backend.services.MetamodelService;
import no.uio.ifi.trackfind.frontend.filters.TreeFilter;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
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
        TreeFilter treeFilter = (TreeFilter) query.getFilter().orElseThrow(RuntimeException::new);
        TfHub hub = treeFilter.getHub();
        String repository = hub.getRepository();
        String hubName = hub.getName();
        Map<String, Map<String, Object>> metamodelTree = metamodelService.getMetamodelTree(repository, hubName);
        Optional<TreeNode> parentOptional = query.getParentOptional();
        if (!parentOptional.isPresent()) {
            Stream<TreeNode> treeNodeStream = metamodelTree.keySet().parallelStream().map(c -> {
                TreeNode treeNode = new TreeNode();
                treeNode.setCategory(c);
                treeNode.setValue(c);
                treeNode.setParent(null);
                treeNode.setSeparator(properties.getLevelsSeparator());
                treeNode.setLevel(0);
                treeNode.setHasValues(false);
                treeNode.setChildren(metamodelTree.get(c).keySet());
                treeNode.setAttribute(true);
                treeNode.setArray(false);
                treeNode.setType("string");
                return treeNode;
            }).sorted();
            return treeNodeStream.filter(treeFilter);
        } else {
            TreeNode parent = parentOptional.get();
            if (!parent.isAttribute()) {
                return Stream.empty();
            }
            String category = parent.getCategory();
            boolean isParentObjectType = parent.getParent() == null;
            Collection<String> children = parent.getChildren();
            Stream<TreeNode> treeNodeStream = children.parallelStream().map(c -> {
                TreeNode treeNode = new TreeNode();
                treeNode.setCategory(category);
                treeNode.setValue(c);
                treeNode.setParent(parent);
                treeNode.setSeparator(properties.getLevelsSeparator());
                treeNode.setLevel(parent.getLevel() + 1);
                if (isParentObjectType) {
                    treeNode.setHasValues(false);
                    treeNode.setChildren(metamodelService.getAttributes(repository, hubName, category, treeNode.getPath()));
                } else {
                    Collection<String> grandChildren = metamodelService.getValues(repository, hubName, category, treeNode.getPath());
                    if (CollectionUtils.isEmpty(grandChildren)) {
                        treeNode.setHasValues(false);
                        treeNode.setChildren(metamodelService.getAttributes(repository, hubName, category, treeNode.getPath()));
                    } else {
                        treeNode.setHasValues(true);
                        treeNode.setChildren(grandChildren);
                    }
                }
                treeNode.setAttribute(CollectionUtils.isNotEmpty(treeNode.getChildren()));
                treeNode.setArray(metamodelService.getArrayOfObjectsAttributes(repository, hubName, category).contains(treeNode.getPath()));
                treeNode.setType(metamodelService.getAttributeTypes(repository, hubName, category).get(treeNode.getPath()));
                return treeNode;
            }).sorted();
            return treeNodeStream.filter(treeFilter);
        }
    }

    @Override
    public int getChildCount(HierarchicalQuery<TreeNode, SerializablePredicate<TreeNode>> query) {
        return (int) fetchChildrenFromBackEnd(query).count();
    }

    @Override
    public boolean hasChildren(TreeNode item) {
        return item.isAttribute();
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
