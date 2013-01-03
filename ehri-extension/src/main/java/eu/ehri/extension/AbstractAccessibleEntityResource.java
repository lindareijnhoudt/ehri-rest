package eu.ehri.extension;

import java.net.URI;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Response.Status;

import org.neo4j.graphdb.GraphDatabaseService;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.utils.ClassUtils;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.views.Crud;
import eu.ehri.project.views.impl.LoggingCrudViews;
import eu.ehri.project.views.impl.Query;

import static eu.ehri.extension.RestHelpers.*;

/**
 * Handle CRUD operations on AccessibleEntity's by using the
 * eu.ehri.project.views.Views class generic code. Resources for specific
 * entities can extend this class.
 * 
 * @param <E>
 *            The specific AccesibleEntity derived class
 */
public class AbstractAccessibleEntityResource<E extends AccessibleEntity> extends
        AbstractRestResource {

    protected final Crud<E> views;
    protected final Query<E> querier;
    protected final Class<E> cls;

    /**
     * Constructor
     * 
     * @param database
     *            Injected neo4j database
     * @param cls
     *            The 'entity' class
     */
    public AbstractAccessibleEntityResource(@Context GraphDatabaseService database,
            Class<E> cls) {
        super(database);
        this.cls = cls;
        views = new LoggingCrudViews<E>(graph, cls);
        querier = new Query<E>(graph, cls);

    }

    /**
     * List all instances of the 'entity' accessible to the given user.
     * 
     * @return
     * @throws ItemNotFound
     * @throws BadRequester
     * @throws PermissionDenied
     */
    public StreamingOutput page(Integer offset, Integer limit)
            throws ItemNotFound, BadRequester {
        final Query.Page<E> page = querier.setOffset(offset).setLimit(limit)
                .page(getRequesterUserProfile());

        return streamingPage(page);
    }

    /**
     * List all instances of the 'entity' accessible to the given user.
     * 
     * @return
     * @throws ItemNotFound
     * @throws BadRequester
     * @throws PermissionDenied
     */
    public StreamingOutput list(Integer offset, Integer limit)
            throws ItemNotFound, BadRequester {
        final Iterable<E> list = querier.setOffset(offset).setLimit(limit)
                .list(getRequesterUserProfile());
        return streamingList(list);
    }

    /**
     * List all instances of the 'entity' accessible to the given user.
     * 
     * @return
     * @throws ItemNotFound
     * @throws BadRequester
     * @throws PermissionDenied
     */
    public <T extends VertexFrame> StreamingOutput list(Iterable<T> iter,
            Integer offset, Integer limit) throws ItemNotFound, BadRequester {
        return streamingList(iter);
    }

    /**
     * Create an instance of the 'entity' in the database
     * 
     * @param json
     *            The json representation of the entity to create (no vertex
     *            'id' fields)
     * @return The response of the create request, the 'location' will contain
     *         the url of the newly created instance.
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     */
    public Response create(String json) throws PermissionDenied,
            ValidationError, IntegrityError, DeserializationError,
            ItemNotFound, BadRequester {

        try {
            Bundle entityBundle = converter.jsonToBundle(json);
            E entity = views.create(converter.bundleToData(entityBundle),
                    getRequesterUserProfile());
            String jsonStr = converter.vertexFrameToJson(entity);
            UriBuilder ub = uriInfo.getAbsolutePathBuilder();
            // FIXME: Hide the details of building this path
            URI docUri = ub.path(
                    (String) entity.asVertex().getProperty(EntityType.ID_KEY))
                    .build();

            return Response.status(Status.CREATED).location(docUri)
                    .entity((jsonStr).getBytes()).build();

        } catch (SerializationError e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity((produceErrorMessageJson(e)).getBytes()).build();
        }
    }

    /**
     * Retieve (get) an instance of the 'entity' in the database
     * 
     * @param id
     *            The vertex id
     * @return The response of the request, which contains the json
     *         representation
     * @throws PermissionDenied
     * @throws BadRequester
     */
    public Response retrieve(long id) throws PermissionDenied, BadRequester {
        try {
            E entity = views.detail(graph.getVertex(id, cls),
                    getRequesterUserProfile());
            String jsonStr = converter.vertexFrameToJson(entity);

            return Response.status(Status.OK).entity((jsonStr).getBytes())
                    .build();
        } catch (SerializationError e) {
            // Most likely there was no such item (wrong id)
            // BETTER get a different Exception for that?
            //
            // so we would need to return a BADREQUEST, or NOTFOUND

            return Response.status(Status.NOT_FOUND)
                    .entity((produceErrorMessageJson(e)).getBytes()).build();
        }
    }

    /**
     * Retieve (get) an instance of the 'entity' in the database
     * 
     * @param id
     *            The Entities identifier string
     * @return The response of the request, which contains the json
     *         representation
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws BadRequester
     */
    public Response retrieve(String id) throws PermissionDenied, ItemNotFound,
            BadRequester {
        try {
            E entity = views.detail(manager.getFrame(id, getEntityType(), cls),
                    getRequesterUserProfile());
            String jsonStr = converter.vertexFrameToJson(entity);
            return Response.status(Status.OK).entity((jsonStr).getBytes())
                    .build();
        } catch (SerializationError e) {
            throw new WebApplicationException(e);
        }
    }

    /**
     * Retieve (get) an instance of the 'entity' in the database
     * 
     * @param key
     *            The key to search
     * @param value
     *            The key's value
     * @return The response of the request, which contains the json
     *         representation
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws BadRequester
     */
    public Response retrieve(String key, String value) throws ItemNotFound,
            PermissionDenied, BadRequester {
        try {
            E entity = querier.get(key, value, getRequesterUserProfile());
            String jsonStr = converter.vertexFrameToJson(entity);
            return Response.status(Status.OK).entity((jsonStr).getBytes())
                    .build();
        } catch (SerializationError e) {
            throw new WebApplicationException(e);
        }
    }

    /**
     * Update (change) an instance of the 'entity' in the database
     * 
     * @param json
     *            The json
     * @return The response of the update request
     * @throws PermissionDenied
     * @throws IntegrityError
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     */
    public Response update(String json) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {

        try {
            Bundle entityBundle = converter.jsonToBundle(json);
            E update = views.update(converter.bundleToData(entityBundle),
                    getRequesterUserProfile());
            String jsonStr = converter.vertexFrameToJson(update);

            return Response.status(Status.OK).entity((jsonStr).getBytes())
                    .build();
        } catch (SerializationError e) {
            throw new WebApplicationException(e);
        }
    }

    /**
     * Update (change) an instance of the 'entity' in the database
     * 
     * @param id
     *            The items identifier property
     * @param json
     *            The json
     * @return The response of the update request
     * @throws PermissionDenied
     * @throws IntegrityError
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     */
    public Response update(String id, String json) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        // FIXME: This is nasty because it searches for an item with the
        // specified key/value and constructs a new bundle containing the
        // item's graph id, which requires an extra
        // serialization/deserialization.
        E entity = views.detail(manager.getFrame(id, getEntityType(), cls),
                getRequesterUserProfile());
        Bundle rawBundle = converter.jsonToBundle(json);
        Bundle entityBundle = new Bundle(manager.getId(entity),
                getEntityType(), rawBundle.getData(), rawBundle.getRelations());
        return update(entityBundle.toString());
    }

    /**
     * Delete (remove) an instance of the 'entity' in the database
     * 
     * @param id
     *            The vertex id
     * @return The response of the delete request
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws ItemNotFound
     * @throws BadRequester
     */
    protected Response delete(Long id) throws PermissionDenied,
            ValidationError, ItemNotFound, BadRequester {
        try {
            views.delete(graph.getVertex(id, cls), getRequesterUserProfile());
            return Response.status(Status.OK).build();
        } catch (SerializationError e) {
            throw new WebApplicationException(e);
        }
    }

    /**
     * Delete (remove) an instance of the 'entity' in the database
     * 
     * @param id
     *            The vertex id
     * @return The response of the delete request
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws ValidationError
     * @throws BadRequester
     * @throws SerializationError
     */
    protected Response delete(String id) throws PermissionDenied, ItemNotFound,
            ValidationError, BadRequester {
        try {
            E entity = views.detail(manager.getFrame(id, getEntityType(), cls),
                    getRequesterUserProfile());
            views.delete(entity, getRequesterUserProfile());
            return Response.status(Status.OK).build();
        } catch (SerializationError e) {
            throw new WebApplicationException(e);
        }
    }

    // Helpers

    private EntityClass getEntityType() {
        return ClassUtils.getEntityType(cls);
    }

}