package eu.ehri.project.importers;

import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.impl.CrudViews;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class Wp2YvEadTest extends AbstractImporterTest {

    private static final Logger logger = LoggerFactory.getLogger(Wp2YvEadTest.class);
    protected final String SINGLE_EAD = "wp2_yv_ead.xml";
    // Depends on fixtures
    protected final String TEST_REPO = "r1";
    // Depends on hierarchical-ead.xml
    protected final String C1 = "O.64.2-A.";
    protected final String C2 = "O.64.2-A.A.";
    protected final String C3 = "3685529";
    protected final String FONDS = "O.64.2";

    @Test
    public void testImportItemsT() throws Exception {

        Repository agent = manager.getFrame(TEST_REPO, Repository.class);
        Bundle vocabularyBundle = new Bundle(EntityClass.CVOC_VOCABULARY)
                                .withDataValue(Ontology.IDENTIFIER_KEY, "WP2_keywords")
                                .withDataValue(Ontology.NAME_KEY, "WP2 Keywords");
        Bundle conceptBundle = new Bundle(EntityClass.CVOC_CONCEPT)
                                .withDataValue(Ontology.IDENTIFIER_KEY, "KEYWORD.JMP.288");
        Vocabulary vocabulary = new CrudViews<Vocabulary>(graph, Vocabulary.class).create(vocabularyBundle,
                validUser);
        logger.debug(vocabulary.getId());
        Concept concept_288 = new CrudViews<Concept>(graph, Concept.class).create(conceptBundle, validUser);
        vocabulary.addItem(concept_288);
        
        
        Vocabulary vocabularyTest = manager.getFrame("wp2-keywords", Vocabulary.class);
        assertNotNull(vocabularyTest);
        
        final String logMessage = "Importing Yad Vashem EAD";

        int count = getNodeCount(graph);
// Before...
       List<VertexProxy> graphState1 = getGraphState(graph);

        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        SaxImportManager importManager = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("wp2ead.properties"));
        
        importManager.setTolerant(Boolean.TRUE);
        
        ImportLog log = importManager.importFile(ios, logMessage);
 // After...
       List<VertexProxy> graphState2 = getGraphState(graph);
       GraphDiff diff = diffGraph(graphState1, graphState2);
//       diff.printDebug(System.out);

        //printGraph(graph);
        // How many new nodes will have been created? We should have
        // - 4 more DocumentaryUnits fonds C1 C2 C3 
        // - 4 more DocumentDescription
        // - 1 more DatePeriod 0 0 1 
        // - 11 UndeterminedRelationship, 0 0 0 11
        // - 5 more import Event links (4 for every Unit, 1 for the User)
        // - 1 more import Event
       
        // - 1 Link as resolved relationship 

//printGraph(graph);
        int newCount = count + 26 + 1;
        assertEquals(newCount, getNodeCount(graph));

        Iterable<Vertex> docs = graph.getVertices(Ontology.IDENTIFIER_KEY, FONDS);
        assertTrue(docs.iterator().hasNext());
        DocumentaryUnit fonds = graph.frame(getVertexByIdentifier(graph, FONDS), DocumentaryUnit.class);

        // check the child items
        DocumentaryUnit c1 = graph.frame(getVertexByIdentifier(graph, C1), DocumentaryUnit.class);
        DocumentaryUnit c2 = graph.frame(getVertexByIdentifier(graph, C2), DocumentaryUnit.class);
        DocumentaryUnit c3 = graph.frame(getVertexByIdentifier(graph, C3), DocumentaryUnit.class);

        assertEquals(fonds, c1.getParent());
        assertEquals(c1, c2.getParent());
        assertEquals(c2, c3.getParent());
        
        // Ensure the import action has the right number of subjects.
        //        Iterable<Action> actions = unit.getHistory();
        // Check we've created 6 items
        assertEquals(4, log.getCreated());
        assertTrue(log.getAction() instanceof SystemEvent);
        assertEquals(logMessage, log.getAction().getLogMessage());

        //assert keywords are matched to cvocs
        assertTrue(toList(c3.getLinks()).size() > 0);
        for(Link a : c3.getLinks()){
            logger.debug(a.getLinkType() + " " + a.getDescription());
            assertEquals("subjectAccess", a.getLinkType());
            int hasBody = 0;
            for(AccessibleEntity body : a.getLinkBodies()){
                hasBody++;
//                logger.debug("body: "+ body.getId() + " " + body.getType());
            }
            assertEquals(1, hasBody);
        }

        List<AccessibleEntity> subjects = toList(log.getAction().getSubjects());
        int countSubject=0;
        for (AccessibleEntity subject : subjects) {
            logger.info("identifier: " + subject.getId());
            countSubject++;
        }
        assertTrue(countSubject > 0);

        assertEquals(4, subjects.size());
        assertEquals(log.getChanged(), subjects.size());

        // Check permission scopes
        assertEquals(agent, fonds.getPermissionScope());
        assertEquals(fonds, c1.getPermissionScope());
        assertEquals(c1, c2.getPermissionScope());
        assertEquals(c2, c3.getPermissionScope());
        
        // Check the author of the top level description
        for (DocumentDescription d : fonds.getDocumentDescriptions()){
            assertEquals("BT", d.asVertex().getProperty("processInfo"));
        }

        // Check the importer is Idempotent
        ImportLog log2 = importManager.importFile(ClassLoader.getSystemResourceAsStream(SINGLE_EAD), logMessage);
        assertEquals(4, log2.getUnchanged());
        //assertEquals(0, log2.getChanged());
        assertEquals(newCount, getNodeCount(graph));
    }
}
