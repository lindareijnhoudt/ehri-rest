package eu.ehri.project.models.idgen;

import com.google.common.collect.ListMultimap;
import eu.ehri.project.persistence.Bundle;

import java.util.Collection;
import java.util.List;

/**
 * Generate an ID given an entity type, a set of scopes, and some data.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface IdGenerator {

    /**
     * @param scopeIds  The entity's parent scope identifiers
     * @param bundle The entity's bundle data
     * @return A set of errors
     */
    public ListMultimap<String, String> handleIdCollision(Collection<String> scopeIds, Bundle bundle);

    /**
     * Generate an ID given an array of scope IDs. This can be used
     * where the scope might not yet exist.
     *
     * @param scopeIds An array of scope ids, ordered parent-to-child.
     * @param bundle   The entity's bundle data
     * @return A generated ID string
     */
    public String generateId(Collection<String> scopeIds, Bundle bundle);

    /**
     * Return the base data for the id, sans scoping.
     * @param bundle The entity's bundle.
     * @return The base id string.
     */
    public String getIdBase(Bundle bundle);
}
