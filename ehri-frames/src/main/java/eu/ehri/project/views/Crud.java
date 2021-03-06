package eu.ehri.project.views;

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Mutation;

/**
 * Interface for CRUD operations on types that derive
 * from {@link AccessibleEntity}.
 *
 * @param <E>
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface Crud<E extends AccessibleEntity> {
    public Crud<E> setScope(PermissionScope scope);
    
    public E detail(String id, Accessor user) throws ItemNotFound;

    public Mutation<E> update(Bundle bundle, Accessor user)
            throws PermissionDenied, ValidationError, DeserializationError, ItemNotFound;

    public E create(Bundle bundle, Accessor user)
            throws PermissionDenied, ValidationError, DeserializationError;

    public Mutation<E> createOrUpdate(Bundle bundle, Accessor user)
            throws PermissionDenied, ValidationError, DeserializationError;

    public Integer delete(String id, Accessor user) throws PermissionDenied,
            ValidationError, SerializationError, ItemNotFound;
}
