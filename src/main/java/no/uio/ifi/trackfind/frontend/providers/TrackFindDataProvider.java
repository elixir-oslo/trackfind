package no.uio.ifi.trackfind.frontend.providers;

import com.vaadin.data.provider.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.server.SerializablePredicate;
import no.uio.ifi.trackfind.backend.data.TreeNode;
import no.uio.ifi.trackfind.backend.pojo.TfHub;
import no.uio.ifi.trackfind.backend.services.impl.MetamodelService;
import no.uio.ifi.trackfind.backend.services.impl.SchemaService;
import no.uio.ifi.trackfind.frontend.filters.TreeFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class TrackFindDataProvider extends AbstractBackEndHierarchicalDataProvider<TreeNode, SerializablePredicate<TreeNode>> {

    @Value("${trackfind.separator}")
    protected String separator;

    private MetamodelService metamodelService;
    private SchemaService schemaService;

    public Stream<TreeNode> fetch(HierarchicalQuery<TreeNode, SerializablePredicate<TreeNode>> query) {
        return fetchChildrenFromBackEnd(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Stream<TreeNode> fetchChildrenFromBackEnd(HierarchicalQuery<TreeNode, SerializablePredicate<TreeNode>> query) {
        TreeFilter treeFilter = (TreeFilter) query.getFilter().orElseThrow(RuntimeException::new);
        TfHub hub = treeFilter.getHub();
        String repository = hub.getRepository();
        String hubName = hub.getName();
        Map<String, Map<String, Object>> metamodelTree;
        metamodelTree = metamodelService.getMetamodelTree(repository, hubName);
        Map<String, Collection<SchemaService.Attribute>> schemaAttributes = schemaService.getAttributes();
        Optional<TreeNode> parentOptional = query.getParentOptional();
        if (parentOptional.isEmpty()) {
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
                treeNode.setStandard(schemaAttributes.containsKey(c));
                treeNode.setArray(false);
                treeNode.setType("string");
                return treeNode;
            }).filter(treeFilter).skip(query.getOffset()).limit(query.getLimit()).sorted();
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
            Collection<SchemaService.Attribute> standardAttributes = schemaService.getAttributes().get(category);
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
                String parentPath = parent.getPath().replace(prefix, "");
                if (treeNode.isAttribute()) {
                    if (parent.isStandard()) {
                        Optional<SchemaService.Attribute> standardAttribute = standardAttributes.parallelStream().filter(a -> a.getPath().replace("'", "").equals(path)).findAny();
                        if (standardAttribute.isPresent() && standardAttribute.get().getIcon() != null) {
                            treeNode.setIcon(standardAttribute.get().getIcon());
                        }
                        standardAttribute = standardAttributes.parallelStream().filter(a -> a.getPath().replace("'", "").equals(parentPath)).findAny();
                        if (standardAttribute.isPresent() && standardAttribute.get().getIcon() != null) {
                            treeNode.setIcon(standardAttribute.get().getIcon());
                        }
                        treeNode.setStandard(standardAttributes.parallelStream().map(a -> a.getPath().replace("'", "")).anyMatch(a -> a.startsWith(path)));
                    }
                    if (attributes.contains(path)) {
                        treeNode.setChildren(metamodelService.getValues(repository, hubName, category, path, null));
                    } else {
                        Collection<String> subAttributes = metamodelService.getAttributes(repository, hubName, category, path);
                        treeNode.setChildren(subAttributes);
                    }
                } else {
                    if (parent.isStandard()) {
                        Optional<SchemaService.Attribute> standardAttribute = standardAttributes.parallelStream().filter(a -> {
                            String substitutedPath = a.getPath().replace("'", "");
                            return substitutedPath.equals(parentPath);
                        }).findAny();
                        if (standardAttribute.isPresent() && standardAttribute.get().getIcon() != null) {
                            treeNode.setIcon(standardAttribute.get().getIcon());
                        }
                        String grandParentPath = parent.getParent().getPath().replace(prefix, "");
                        standardAttribute = standardAttributes.parallelStream().filter(a -> {
                            String substitutedPath = a.getPath().replace("'", "");
                            return substitutedPath.equals(grandParentPath);
                        }).findAny();
                        if (standardAttribute.isPresent() && standardAttribute.get().getIcon() != null) {
                            treeNode.setIcon(standardAttribute.get().getIcon());
                        }
                        treeNode.setStandard(true);
                    }
                }
                return treeNode;
            }).filter(treeFilter).skip(query.getOffset()).limit(query.getLimit()).sorted();
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
    public void setMetamodelService(MetamodelService metamodelService) {
        this.metamodelService = metamodelService;
    }

    @Autowired
    public void setSchemaService(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

}
