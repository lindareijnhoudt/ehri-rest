package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.models.base.PermissionScope;

@EntityType(EntityTypes.PERMISSION_GRANT)
public interface PermissionGrant extends AccessibleEntity {
    public static final String HAS_GRANTEE = "hasGrantee";
    public static final String HAS_ACCESSOR = "hasAccessor";
    public static final String HAS_PERMISSION = "hasPermission";
    public static final String HAS_ENTITY = "hasEntity";
    public static final String HAS_CONTENT_TYPE = "hasContentType";
    public static final String HAS_SCOPE = "hasScope";
    public static final String HAS_TARGET = "hasTarget";
    
    
    @Adjacency(label = HAS_CONTENT_TYPE)
    public ContentType getContentType();
    
    @Adjacency(label = HAS_CONTENT_TYPE)
    public void setContentType(final ContentType type);
    
    @Adjacency(label = HAS_ACCESSOR)
    public Accessor getAccessor();

    @Adjacency(label = HAS_GRANTEE)
    public Accessor getGrantee();

    @Adjacency(label = HAS_ENTITY)
    public Iterable<AccessibleEntity> getEntities();

    @Adjacency(label = HAS_TARGET)
    public Iterable<PermissionGrantTarget> getTargets();

    @Adjacency(label = HAS_TARGET)
    public void addTarget(final PermissionGrantTarget target);

    @Adjacency(label = HAS_TARGET)
    public void removeTarget(final PermissionGrantTarget target);

    @Adjacency(label = HAS_PERMISSION)
    public Permission getPermission();

    @Adjacency(label = HAS_PERMISSION)
    public void setPermission(final Permission permission);

    @Adjacency(label = HAS_SCOPE)
    public Iterable<PermissionScope> getScopes();
    
    @Adjacency(label = HAS_SCOPE)
    public void addScope(final PermissionScope scope);
}