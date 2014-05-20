/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.VirtualUnit;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.impl.CrudViews;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;

/**
 *
 * @author linda
 */
public class Wp2BtEadTest extends AbstractImporterTest {

    private static final Logger logger = LoggerFactory.getLogger(Wp2BtEadTest.class);
    protected final String SINGLE_EAD = "wp2_bt_ead_small.xml";
    // Depends on fixtures
    protected final String TEST_REPO = "r1";
    // Depends on hierarchical-ead.xml
    protected final String C1_A = "000.001.0";
    protected final String C1_B = "000.002.0";
    protected final String C1_A_C2 = "000.001.1";
    protected final String C1_B_C2_A = "000.002.1";
    protected final String C1_B_C2_B = "000.002.2";
    protected final String FONDS = "wp2bt";

    @Test
    public void testImportItemsT() throws Exception {

        Repository agent = manager.getFrame(TEST_REPO, Repository.class);
        Bundle vocabularyBundle = new Bundle(EntityClass.CVOC_VOCABULARY)
                                .withDataValue(Ontology.IDENTIFIER_KEY, "WP2_keywords");
        Bundle conceptBundle = new Bundle(EntityClass.CVOC_CONCEPT)
                                .withDataValue(Ontology.IDENTIFIER_KEY, "KEYWORD.JMP.716");
        Vocabulary vocabulary = new CrudViews<Vocabulary>(graph, Vocabulary.class).create(vocabularyBundle, validUser);
        logger.debug(vocabulary.getId());
        Concept concept_716 = new CrudViews<Concept>(graph, Concept.class).create(conceptBundle, validUser); 
        vocabulary.addConcept(concept_716);
        
        
        Vocabulary vocabularyTest = manager.getFrame("wp2-keywords", Vocabulary.class);
        assertNotNull(vocabularyTest);
        
        final String logMessage = "Importing Beit Terezin EAD";

        int count = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        SaxImportManager importManager = new SaxImportManager(graph, agent, validUser, EadIntoVirtualCollectionImporter.class, EadIntoVirtualCollectionHandler.class);
        
        importManager.setTolerant(Boolean.TRUE);
        VirtualUnit virtualcollection = graph.frame(getVertexByIdentifier(graph, "vc1"), VirtualUnit.class);
        importManager.setVirtualCollection(virtualcollection);
        
        ImportLog log = importManager.importFile(ios, logMessage);

//        printGraph(graph);
        // How many new nodes will have been created? We should have
        // - 6 more DocumentaryUnits fonds 2C1 3C2
        // - 6 more VirtualUnits
        // - 6 more DocumentDescription
        // - 1 more DatePeriod 0 0 1 
        // - 17 UndeterminedRelationship, 0 2 2 4 4 5
        // - 7 more import Event links (6 for every Unit, 1 for the User)
        // - 1 more import Event
        // - 1 Annotation as resolved relationship 
        int newCount = count + 39+6;
        assertEquals(newCount, getNodeCount(graph));

        Iterable<Vertex> docs = graph.getVertices(Ontology.IDENTIFIER_KEY, FONDS);
        assertTrue(docs.iterator().hasNext());
        
        
        DocumentaryUnit fonds = graph.frame(getVertexByIdentifier(graph, FONDS), DocumentaryUnit.class);
        Iterator<DocumentDescription> i = fonds.getDocumentDescriptions().iterator();
        int countDocDesc = 0;
        while(i.hasNext()){
            DocumentDescription desc  = i.next();
            countDocDesc++;
        }        
        assertEquals(1, countDocDesc);
        
//the virtual fonds should have no Descriptions 'describing' the virtual fonds        
        VirtualUnit virtual_fonds = graph.frame(getVertexByIdentifier(graph, EadIntoVirtualCollectionImporter.VIRTUAL_PREFIX+FONDS), VirtualUnit.class);
        Iterator<Description> v_i = virtual_fonds.getDescriptions().iterator();
        countDocDesc = 0;
        while(v_i.hasNext()){
            Description desc  = v_i.next();
            countDocDesc++;
        }
        assertEquals(0, countDocDesc);
        
//the virtual fonds should have 1 referencedDescription, the DocumentsDescription of the actual fonds.
        Iterator<DocumentDescription> v_i_ref = virtual_fonds.getReferencedDescriptions().iterator();
        countDocDesc = 0;
        while(v_i_ref.hasNext()){
            DocumentDescription desc  = v_i_ref.next();
            countDocDesc++;
        }
        assertEquals(1, countDocDesc);

        // check the child items
        DocumentaryUnit c1_a = graph.frame(getVertexByIdentifier(graph, C1_A), DocumentaryUnit.class);
        DocumentaryUnit c1_b = graph.frame(getVertexByIdentifier(graph, C1_B), DocumentaryUnit.class);
        VirtualUnit v_c1_b =graph.frame(getVertexByIdentifier(graph, EadIntoVirtualCollectionImporter.VIRTUAL_PREFIX+C1_B), VirtualUnit.class);
        DocumentaryUnit c1_a_c2 = graph.frame(getVertexByIdentifier(graph, C1_A_C2), DocumentaryUnit.class);
        DocumentaryUnit c1_b_c2_a = graph.frame(getVertexByIdentifier(graph, C1_B_C2_A), DocumentaryUnit.class);
        DocumentaryUnit c1_b_c2_b = graph.frame(getVertexByIdentifier(graph, C1_B_C2_B), DocumentaryUnit.class);

        assertEquals(fonds, c1_a.getParent());
        assertEquals(fonds, c1_b.getParent());
        
        assertEquals(c1_a, c1_a_c2.getParent());

        assertEquals(c1_b, c1_b_c2_a.getParent());
        assertEquals(c1_b, c1_b_c2_b.getParent());

        // Ensure the import action has the right number of subjects.
        //        Iterable<Action> actions = unit.getHistory();
        // Check we've created 6 items
        assertEquals(6, log.getCreated());
        assertTrue(log.getAction() instanceof SystemEvent);
        assertEquals(logMessage, log.getAction().getLogMessage());

        //assert keywords are matched to cvocs
        assertTrue(toList(v_c1_b.getLinks()).size() > 0);
        for(Link a : v_c1_b.getLinks()){
            logger.debug(a.getLinkType());
        }

        List<AccessibleEntity> subjects = toList(log.getAction().getSubjects());
        for (AccessibleEntity subject : subjects) {
            logger.info("identifier: " + subject.getId());
        }

        assertEquals(6, subjects.size());
        assertEquals(log.getChanged(), subjects.size());

        //check permissionscope of VU's
        System.out.println("virtual_fonds.getPermissionScope().idPath() : "+ virtual_fonds.getPermissionScope().getIdentifier());
        assertEquals(virtualcollection, virtual_fonds.getPermissionScope());
        assertNotNull(v_c1_b);
        assertNotNull(v_c1_b.getPermissionScope());
        System.out.println("v_c1_b.getPermissionScope().idPath() : "+ v_c1_b.getPermissionScope().idPath());
        assertEquals(virtual_fonds, v_c1_b.getPermissionScope());

        //check parents of VU's
        assertEquals(virtualcollection, virtual_fonds.getParent());
        assertEquals(virtual_fonds, v_c1_b.getParent());

        
        // Check permission scopes
        assertEquals(agent, fonds.getPermissionScope());
        assertEquals(fonds, c1_a.getPermissionScope());
        assertEquals(fonds, c1_b.getPermissionScope());
        assertEquals(c1_a, c1_a_c2.getPermissionScope());
        assertEquals(c1_b, c1_b_c2_a.getPermissionScope());
        assertEquals(c1_b, c1_b_c2_b.getPermissionScope());
        
        // Check the author of the description
        for (DocumentDescription d : c1_a.getDocumentDescriptions()){
            assertEquals(EadIntoVirtualCollectionImporter.WP2AUTHOR, d.asVertex().getProperty(EadIntoVirtualCollectionImporter.PROPERTY_AUTHOR));
        }

        // Check the importer is Idempotent
        ImportLog log2 = importManager.importFile(ClassLoader.getSystemResourceAsStream(SINGLE_EAD), logMessage);
        assertEquals(6, log2.getUnchanged());
        //assertEquals(0, log2.getChanged());
        assertEquals(newCount, getNodeCount(graph));
    }
}
