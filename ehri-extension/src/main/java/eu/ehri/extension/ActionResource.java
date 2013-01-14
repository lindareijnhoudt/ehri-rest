package eu.ehri.extension;

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.neo4j.graphdb.GraphDatabaseService;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.Action;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.views.impl.LoggingCrudViews;
import eu.ehri.project.views.impl.Query;

/**
 * Provides a RESTfull interface for the Action class. Note: Action instances
 * are created by the system, so we do not have create/update/delete methods
 * here.
 */
@Path(Entities.ACTION)
public class ActionResource extends AbstractAccessibleEntityResource<Action> {

    public ActionResource(@Context GraphDatabaseService database) {
        super(database, Action.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:.+}")
    public Response getAction(@PathParam("id") String id) throws ItemNotFound,
            PermissionDenied, BadRequester {
        return retrieve(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list")
    public StreamingOutput listActions(
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        return list(offset, limit, order, filters);
    }

    /**
     * Lookup and page the history for a given item.
     * 
     * @param id
     * @param offset
     * @param limit
     * @return
     * @throws ItemNotFound
     * @throws BadRequester
     * @throws PermissionDenied
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/for/{id:.+}")
    public StreamingOutput pageActionsForItem(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester, PermissionDenied {
        Accessor user = getRequesterUserProfile();
        AccessibleEntity item = new LoggingCrudViews<AccessibleEntity>(graph,
                AccessibleEntity.class).detail(
                manager.getFrame(id, AccessibleEntity.class), user);
        // FIXME: We ignore the sort param here...
        Query<Action> query = new Query<Action>(graph, Action.class)
                .setOffset(offset).setLimit(limit)
                .orderBy(Action.TIMESTAMP, Query.Sort.DESC).filter(filters);
        return streamingPage(query.page(item.getHistory(), user));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/page")
    public StreamingOutput pageActions(
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        return page(offset, limit, order, filters);
    }
}
