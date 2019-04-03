package no.uio.ifi.trackfind.frontend.providers;

import com.vaadin.data.provider.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.server.SerializablePredicate;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import no.uio.ifi.trackfind.backend.dao.Hub;
import no.uio.ifi.trackfind.backend.data.TreeNode;
import no.uio.ifi.trackfind.backend.services.MetamodelService;
import no.uio.ifi.trackfind.frontend.filters.TreeFilter;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
        Hub hub = treeFilter.getHub();
        boolean raw = false;
        Map<String, Object> metamodelTree = metamodelService.getMetamodelTree(hub, raw);
        Optional<TreeNode> parentOptional = query.getParentOptional();
        if (!parentOptional.isPresent()) {
            Stream<TreeNode> treeNodeStream = metamodelTree.keySet().parallelStream().map(c -> {
                TreeNode treeNode = new TreeNode();
                treeNode.setValue(c);
                treeNode.setParent(null);
                treeNode.setSeparator(properties.getLevelsSeparator());
                treeNode.setLevel(0);
                Collection<String> values = metamodelService.getValues(hub, treeNode.getPath(), "", raw);
                treeNode.setHasValues(CollectionUtils.isNotEmpty(values));
                Collection<String> grandChildren = new ArrayList<>(values);
                grandChildren.addAll(metamodelService.getSubAttributes(hub, treeNode.getPath(), "", raw));
                treeNode.setChildren(grandChildren);
                treeNode.setAttribute(true);
                treeNode.setArray(metamodelService.getArrayOfObjectsAttributes(hub, raw).contains(treeNode.getPath()));
                treeNode.setType(metamodelService.getAttributeTypes(hub, raw).get(treeNode.getPath()));
                return treeNode;
            }).sorted();
            return treeNodeStream.filter(treeFilter);
        } else {
            TreeNode parent = parentOptional.get();
            if (!parent.isAttribute()) {
                return Stream.empty();
            }
            Collection<String> children = new ArrayList<>();
            children.addAll(metamodelService.getValues(hub, parent.getPath(), "", raw));
            children.addAll(metamodelService.getSubAttributes(hub, parent.getPath(), "", raw));
            Stream<TreeNode> treeNodeStream = children.parallelStream().map(c -> {
                TreeNode treeNode = new TreeNode();
                treeNode.setValue(c);
                treeNode.setParent(parent);
                treeNode.setSeparator(properties.getLevelsSeparator());
                treeNode.setLevel(parent.getLevel() + 1);
                Collection<String> values = metamodelService.getValues(hub, treeNode.getPath(), "", raw);
                treeNode.setHasValues(CollectionUtils.isNotEmpty(values));
                Collection<String> grandChildren = new ArrayList<>(values);
                grandChildren.addAll(metamodelService.getSubAttributes(hub, treeNode.getPath(), "", raw));
                treeNode.setChildren(grandChildren);
                treeNode.setAttribute(CollectionUtils.isNotEmpty(grandChildren));
                treeNode.setArray(metamodelService.getArrayOfObjectsAttributes(hub, raw).contains(treeNode.getPath()));
                treeNode.setType(metamodelService.getAttributeTypes(hub, raw).get(treeNode.getPath()));
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
