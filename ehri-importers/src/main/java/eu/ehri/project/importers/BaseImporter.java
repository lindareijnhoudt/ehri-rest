package eu.ehri.project.importers;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.Authority;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.BundleFactory;
import eu.ehri.project.persistance.EntityBundle;

public abstract class BaseImporter<T> implements Importer<T> {
    private Agent repository;
    private FramedGraph<Neo4jGraph> framedGraph;

    public BaseImporter(FramedGraph<Neo4jGraph> framedGraph, Agent repository) {
        this.repository = repository;
        this.framedGraph = framedGraph;
    }

    public void importDocumentaryUnit(T data) throws Exception {
        // TODO Auto-generated method stub
    }

    public List<T> extractChildData(T data) {
        return new LinkedList<T>();
    }

    public EntityBundle<DocumentaryUnit> extractDocumentaryUnit(T data)
            throws Exception {

        EntityBundle<DocumentaryUnit> bundle = new BundleFactory<DocumentaryUnit>()
                .buildBundle(new HashMap<String, Object>(),
                        DocumentaryUnit.class);

        // Extract details for the logical item here

        return bundle;
    }

    public abstract List<EntityBundle<DocumentDescription>> extractDocumentDescriptions(
            T data) throws Exception;

    public List<EntityBundle<DocumentaryUnit>> extractParent(T data)
            throws Exception {
        return new LinkedList<EntityBundle<DocumentaryUnit>>();
    }

    public List<EntityBundle<Authority>> extractAuthorities(T data)
            throws Exception {
        return new LinkedList<EntityBundle<Authority>>();
    }

    public List<EntityBundle<DatePeriod>> extractDates(T data) throws Exception {
        return new LinkedList<EntityBundle<DatePeriod>>();
    }

    public void importItem(T data, DocumentaryUnit parent) throws Exception {
        EntityBundle<DocumentaryUnit> unit = extractDocumentaryUnit(data);
        BundleDAO<DocumentaryUnit> persister = new BundleDAO<DocumentaryUnit>(
                framedGraph);
        DocumentaryUnit frame = persister.insert(unit);

        // Set the parent child relationship
        if (parent != null)
            parent.addChild(frame);

        // Set the repository/item relationship
        repository.addCollection(frame);

        // Save DatePeriods... this is not an idempotent step, and will need
        // to be radically altered when updating existing items...
        {
            BundleDAO<DatePeriod> datePersister = new BundleDAO<DatePeriod>(
                    framedGraph);
            for (EntityBundle<DatePeriod> dpb : extractDates(data)) {
                frame.addDatePeriod(datePersister.insert(dpb));
            }
        }

        // Save Descriptions
        {
            BundleDAO<DocumentDescription> descPersister = new BundleDAO<DocumentDescription>(
                    framedGraph);
            for (EntityBundle<DocumentDescription> dpb : extractDocumentDescriptions(data)) {
                frame.addDescription(descPersister.insert(dpb));
            }
        }

        // Search through child parts and add them recursively...
        for (T child : extractChildData(data)) {
            System.out.println("Importing child item...");
            importItems(child, frame);
        }
    }

    
    /**
     * Entry point for a top-level DocumentaryUnit item.
     */
    public void importItem(T data) throws Exception {
        importItem(data, null);
    }

    /**
     * Import (multiple) items from the top-level data.
     * 
     * @param data
     * @param agent
     * @throws Exception
     */
    public void importItems(T data, DocumentaryUnit parent) throws Exception {
        importItem(data, parent);
    }

    public void importItems(T data) throws Exception {
        importItems(data, null);
    }
}
