package eu.ehri.project.acl;

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.EmptyIterable;

import java.util.Collection;

/**
 * Singleton class representing the system scope for
 * permissions and ID namespaces.
 * 
 * @author Mike Bryant (http://github.com/mikesname)
 */
public enum SystemScope implements PermissionScope {
    
    INSTANCE;

    /**
     * Obtain the shared instance of SystemScope.
     * @return The global SystemScope instance
     */
    public static PermissionScope getInstance() {
        return INSTANCE;
    }
    
    public String getId() {
        return Entities.SYSTEM;
    }

    public String getType() {
        return Entities.SYSTEM;
    }

    public String getIdentifier() {
        return Entities.SYSTEM;
    }

    public Vertex asVertex() {
        // TODO: Determine if there's a better approach to this.
        // Since PermissionScope can be implemented by several
        // types of node, comparing them by vertex is the only
        // reliable approach. ReRally, this operation should
        // throw an UnsupportedOperationException().
        return null;
    }

    public Iterable<PermissionGrant> getPermissionGrants() {
        return new EmptyIterable<PermissionGrant>();
    }

    public Iterable<PermissionScope> getPermissionScopes() {
        return new EmptyIterable<PermissionScope>();
    }

    @Override
    public Iterable<Frame> getContainedItems() {
        return new EmptyIterable<Frame>();
    }

    @Override
    public Iterable<Frame> getAllContainedItems() {
        return new EmptyIterable<Frame>();
    }

    @Override
    public Collection<String> idPath() {
        return Lists.newArrayList();
    }
}
