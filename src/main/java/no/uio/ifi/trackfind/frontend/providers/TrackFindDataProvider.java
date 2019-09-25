package no.uio.ifi.trackfind.frontend.providers;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.vaadin.data.provider.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.server.SerializablePredicate;
import no.uio.ifi.trackfind.backend.data.TreeNode;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import no.uio.ifi.trackfind.backend.services.impl.MetamodelService;
import no.uio.ifi.trackfind.frontend.filters.TreeFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TrackFindDataProvider extends AbstractBackEndHierarchicalDataProvider<TreeNode, SerializablePredicate<TreeNode>> {

    @Value("${trackfind.separator}")
    protected String separator;

    private MetamodelService metamodelService;

    /**
     * {@inheritDoc}
     */
    @HystrixCommand(fallbackMethod = "fallbackFetchChildrenFromBackEnd", commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "10000")
    })
    @Override
    protected Stream<TreeNode> fetchChildrenFromBackEnd(HierarchicalQuery<TreeNode, SerializablePredicate<TreeNode>> query) {
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
                treeNode.setSeparator(separator);
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
            String prefix = category + separator;
            return children.stream().map(c -> {
                TreeNode treeNode = new TreeNode();
                treeNode.setTreeFilter(treeFilter);
                treeNode.setCategory(category);
                treeNode.setValue(c);
                treeNode.setParent(parent);
                treeNode.setSeparator(separator);
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

    protected Stream<TreeNode> fallbackFetchChildrenFromBackEnd(HierarchicalQuery<TreeNode, SerializablePredicate<TreeNode>> query) {
        return Stream.empty();
    }

    @HystrixCommand(fallbackMethod = "fallbackGetChildCount", commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "10000")
    })
    @Override
    public int getChildCount(HierarchicalQuery<TreeNode, SerializablePredicate<TreeNode>> query) {
        return (int) fetchChildrenFromBackEnd(query).count();
    }

    public int fallbackGetChildCount(HierarchicalQuery<TreeNode, SerializablePredicate<TreeNode>> query) {
        return 0;
    }

    @HystrixCommand(fallbackMethod = "fallbackHasChildren", commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "10000")
    })
    @Override
    public boolean hasChildren(TreeNode item) {
        return getChildCount(new HierarchicalQuery<>(item.getTreeFilter(), item)) != 0;
    }

    public boolean fallbackHasChildren(TreeNode item) {
        return false;
    }

    @Autowired
    public void setMetamodelService(MetamodelService metamodelService) {
        this.metamodelService = metamodelService;
    }

}
