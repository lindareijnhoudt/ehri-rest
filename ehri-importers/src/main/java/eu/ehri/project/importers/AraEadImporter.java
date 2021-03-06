package eu.ehri.project.importers;

import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.AbstractUnit;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.persistence.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Import EAD for a given repository into the database. Due to the laxness of the EAD standard this is a fairly complex
 * procedure. An EAD a single entity at the highest level of description or multiple top-level entities, with or without
 * a hierarchical structure describing their child items. This means that we need to recursively descend through the
 * archdesc and c,c01-12 levels.
 * 
 * will preserve existing 'otherIdentifiers' on the DocumentaryUnit.
 *
 * Furthermore, it will always try to resolve the UndeterminedRelationships, not just on creation.
 * This is not standard behaviour, so use with caution.
 *
 * TODO: Extensive cleanups, optimisation, and rationalisation.
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 *
 */
public class AraEadImporter extends EadImporter {

    private static final Logger logger = LoggerFactory.getLogger(AraEadImporter.class);
    //the EadImporter can import ead as DocumentaryUnits, the default, or overwrite those and create VirtualUnits instead.
    private EntityClass unitEntity = EntityClass.DOCUMENTARY_UNIT;
    private Serializer mergeSerializer;

    /**
     * Construct an EadImporter object.
     *
     * @param framedGraph
     * @param permissionScope
     * @param log
     */
    public AraEadImporter(FramedGraph<?> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);
        mergeSerializer = new Serializer.Builder(framedGraph).dependentOnly().build();
    }

    /**
     * Import a single archdesc or c01-12 item, keeping a reference to the hierarchical depth.
     *
     * @param itemData The data map
     * @param idPath The identifiers of parent documents, not including those of the overall permission scope
     * @throws ValidationError when the itemData does not contain an identifier for the unit or...
     */
    @Override
    public AbstractUnit importItem(Map<String, Object> itemData, List<String> idPath)
            throws ValidationError {

        BundleDAO persister = getPersister(idPath);

        List<Map<String, Object>> extractedDates = extractDates(itemData);
        replaceDates(itemData, extractedDates);

        Bundle descBundle = new Bundle(EntityClass.DOCUMENT_DESCRIPTION, extractUnitDescription(itemData, EntityClass.DOCUMENT_DESCRIPTION));
        // Add dates and descriptions to the bundle since they're @Dependent
        // relations.
        for (Map<String, Object> dpb : extractedDates) {
            descBundle = descBundle.withRelation(Ontology.ENTITY_HAS_DATE, new Bundle(EntityClass.DATE_PERIOD, dpb));
        }
        for (Map<String, Object> rel : extractRelations(itemData)) {//, (String) unit.getErrors().get(IdentifiableEntity.IDENTIFIER_KEY)
            logger.debug("relation found: " + rel.get(Ontology.NAME_KEY));
            for (String s : rel.keySet()) {
                logger.debug(s);
            }
            descBundle = descBundle.withRelation(Ontology.HAS_ACCESS_POINT, new Bundle(EntityClass.UNDETERMINED_RELATIONSHIP, rel));
        }
        Map<String, Object> unknowns = extractUnknownProperties(itemData);
        if (!unknowns.isEmpty()) {
            StringBuilder unknownProperties = new StringBuilder();
            for (String u : unknowns.keySet()) {
                unknownProperties.append(u);
            }
            logger.info("Unknown Properties found: " + unknownProperties.toString());
            descBundle = descBundle.withRelation(Ontology.HAS_UNKNOWN_PROPERTY, new Bundle(EntityClass.UNKNOWN_PROPERTY, unknowns));
        }
        // extractDocumentaryUnit does not throw ValidationError on missing ID
        Bundle unit = new Bundle(unitEntity, extractDocumentaryUnit(itemData));


        // Check for missing identifier, throw an exception when there is no ID.
        if (unit.getDataValue(Ontology.IDENTIFIER_KEY) == null) {
            throw new ValidationError(unit, Ontology.IDENTIFIER_KEY,
                    "Missing identifier " + Ontology.IDENTIFIER_KEY);
        }
        logger.debug("Imported item: " + itemData.get("name"));

        Mutation<DocumentaryUnit> mutation =
                persister.createOrUpdate(mergeWithPreviousAndSave(unit, descBundle, idPath), DocumentaryUnit.class);
        DocumentaryUnit frame = mutation.getNode();

        // Set the repository/item relationship
        if (idPath.isEmpty() && mutation.created()) {
            EntityClass scopeType = manager.getEntityClass(permissionScope);
            if (scopeType.equals(EntityClass.REPOSITORY)) {
                Repository repository = framedGraph.frame(permissionScope.asVertex(), Repository.class);
                frame.setRepository(repository);
                frame.setPermissionScope(repository);
            } else if (scopeType.equals(unitEntity)) {
                DocumentaryUnit parent = framedGraph.frame(permissionScope.asVertex(), DocumentaryUnit.class);
                parent.addChild(frame);
                frame.setPermissionScope(parent);
            } else {
                logger.error("Unknown scope type for documentary unit: {}", scopeType);
            }
        }
        handleCallbacks(mutation);
        logger.debug("============== " + frame.getIdentifier() + " created:" + mutation.created());
        
        //BEWARE: it will always try to solve the UndeterminedRelationships, not only on creation!
//        if (mutation.created()) {
            solveUndeterminedRelationships(frame, descBundle);
//        }
        return frame;


    }

    /**
     * finds any bundle in the graph with the same ObjectIdentifier. if it exists it replaces the Description in the
     * given language, else it just saves it
     *
     * @param unit - the DocumentaryUnit to be saved
     * @param descBundle - the documentsDescription to replace any previous ones with this language
     * @return A bundle with description relationships merged.
     * @throws ValidationError
     */
    protected Bundle mergeWithPreviousAndSave(Bundle unit, Bundle descBundle, List<String> idPath) throws ValidationError {
        final String languageOfDesc = descBundle.getDataValue(Ontology.LANGUAGE_OF_DESCRIPTION);
        final String thisSourceFileId = descBundle.getDataValue(Ontology.SOURCEFILE_KEY);
        /*
         * for some reason, the idpath from the permissionscope does not contain the parent documentary unit.
         * TODO: so for now, it is added manually
         */
        List<String> lpath = new ArrayList<String>();
        for (String p : getPermissionScope().idPath()) {
            lpath.add(p);
        }
        for (String p : idPath) {
            lpath.add(p);
        }
        Bundle withIds = unit.generateIds(lpath);

        if (manager.exists(withIds.getId())) {
            try {
                //read the current item’s bundle
                Bundle oldBundle = mergeSerializer
                        .vertexFrameToBundle(manager.getVertex(withIds.getId()));

                //determine if previous existing DocUnit had 'otherIdentifiers', if so, add to existing withIds
                if (oldBundle.getData().keySet().contains(Ontology.OTHER_IDENTIFIERS)) {
                    Object otherIdentifiers = oldBundle.getData().get(Ontology.OTHER_IDENTIFIERS);
                    if(unit.getData().keySet().contains(Ontology.OTHER_IDENTIFIERS)){
                        if(otherIdentifiers instanceof List){
                            ((List<String>)otherIdentifiers).add(unit.getDataValue(Ontology.OTHER_IDENTIFIERS).toString());
                        }else if (otherIdentifiers instanceof String){
                            List<String> allOtherIdentifiers = new ArrayList<String>();
                            allOtherIdentifiers.add(otherIdentifiers.toString());
                            allOtherIdentifiers.add(unit.getDataValue(Ontology.OTHER_IDENTIFIERS).toString());
                            otherIdentifiers = allOtherIdentifiers;
                        }
                    }
                    withIds = withIds.withDataValue(Ontology.OTHER_IDENTIFIERS, otherIdentifiers);
                }

                

                //if the unit exists, with a desc with the same sourcefileid, overwrite, else create new desc
                //filter out dependents that a) are descriptions, b) have the same language/code
                Bundle.Filter filter = new Bundle.Filter() {
                    @Override
                    public boolean remove(String relationLabel, Bundle bundle) {
                        String lang = bundle.getDataValue(Ontology.LANGUAGE);
                        String oldSourceFileId = bundle.getDataValue(Ontology.SOURCEFILE_KEY);
                        return bundle.getType().equals(EntityClass.DOCUMENT_DESCRIPTION)
                                && (lang != null
                                && lang.equals(languageOfDesc)
                                && (oldSourceFileId != null && oldSourceFileId.equals(thisSourceFileId)));
                    }
                };
                Bundle filtered = oldBundle.filterRelations(filter);

                //if this desc-id already exists, but with a different sourceFileId, 
                //change the desc-id
                String defaultDescIdentifier = withIds.getId() + "-" + languageOfDesc.toLowerCase();
                String newDescIdentifier = withIds.getId() + "-" + thisSourceFileId.toLowerCase().replace("#", "-");
                if (manager.exists(newDescIdentifier)) {
                    descBundle = descBundle.withDataValue(Ontology.IDENTIFIER_KEY, newDescIdentifier);
                } else if (manager.exists(defaultDescIdentifier)) {
                    Bundle oldDescBundle = mergeSerializer
                            .vertexFrameToBundle(manager.getVertex(defaultDescIdentifier));
                    //if the previous had NO sourcefile_key OR it was different:
                    if (oldDescBundle.getDataValue(Ontology.SOURCEFILE_KEY) == null
                            || !thisSourceFileId.equals(oldDescBundle.getDataValue(Ontology.SOURCEFILE_KEY).toString())) {
                        descBundle = descBundle.withDataValue(Ontology.IDENTIFIER_KEY, newDescIdentifier);
                        logger.info("other description found (" + defaultDescIdentifier + "), creating new description id: " + descBundle.getDataValue(Ontology.IDENTIFIER_KEY).toString());
                    }
                }

                return withIds.withRelations(filtered.getRelations())
                        .withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);

            } catch (SerializationError ex) {
                throw new ValidationError(unit, "serialization error", ex.getMessage());
            } catch (ItemNotFound ex) {
                throw new ValidationError(unit, "item not found exception", ex.getMessage());
            }
        } else {
            return unit.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);
        }
    }

    
}
