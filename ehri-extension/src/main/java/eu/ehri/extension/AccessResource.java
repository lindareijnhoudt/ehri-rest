package eu.ehri.extension;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;

/**
 * Provides a RESTful(ish) interface for setting PermissionTarget perms.
 */
@Path(AccessResource.ENDPOINT)
public class AccessResource extends
        AbstractAccessibleEntityResource<AccessibleEntity> {

    public static final String ENDPOINT = "access";

    public AccessResource(@Context GraphDatabaseService database) {
        super(database, AccessibleEntity.class);
    }

    /**
     * Set the accessors who are able to view an item. If no accessors
     * are set, the item is globally readable.
     *
     * @param id          The ID of the item
     * @param accessorIds The IDs of the users who can access this item.
     * @return the updated object
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @POST
    @Path("/{id:[^/]+}")
    public Response setVisibility(@PathParam("id") String id,
            @QueryParam(ACCESSOR_PARAM) List<String> accessorIds)
            throws PermissionDenied, ItemNotFound, BadRequester, SerializationError {
        graph.getBaseGraph().checkNotInTransaction();
        try {
            AccessibleEntity item = manager.getFrame(id, AccessibleEntity.class);
            Accessor current = getRequesterUserProfile();
            Set<Accessor> accessors = getAccessors(accessorIds, current);
            aclViews.setAccessors(item, accessors, current);
            graph.getBaseGraph().commit();
            return single(item);
        } finally {
            cleanupTransaction();
        }
    }
}
