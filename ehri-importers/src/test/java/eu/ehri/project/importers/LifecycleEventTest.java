package eu.ehri.project.importers;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tinkerpop.blueprints.Vertex;

import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.events.SystemEvent;
import java.util.ArrayList;

public class LifecycleEventTest extends AbstractImporterTest {

    private static final Logger logger = LoggerFactory.getLogger(LifecycleEventTest.class);
    protected final String EAD_EN = "exptestEsterwegen_en.xml";
    protected final String EAD_DE = "exptestEsterwegen_de.xml";
    protected final String IMPORTED_ITEM_ID = "DE ITS [OuS 1.1.7]";
    // Depends on fixtures
    protected final String TEST_REPO = "r1";

    @Test
    public void testItsLifeCycleEvents() throws Exception {
        Repository agent = manager.getFrame(TEST_REPO, Repository.class);
        final String logMessage = "Importing a single EAD by ItsTest";

        InputStream ios = ClassLoader.getSystemResourceAsStream(EAD_EN);
        InputStream ios2 = ClassLoader.getSystemResourceAsStream(EAD_DE);

        XmlImportManager sim = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("its.properties")).setTolerant(Boolean.TRUE);
        List<String> paths = new ArrayList<String>();
        paths.add("target/test-classes/"+EAD_EN);
        paths.add("target/test-classes/"+EAD_DE);
        
        ImportLog log_en = sim.importFiles(paths, logMessage);

        DocumentaryUnit unit = graph.frame(
                getVertexByIdentifier(graph, IMPORTED_ITEM_ID),
                DocumentaryUnit.class);

        //there should be only one direct lifecycleEvent
        int lifecycleEventCount = 0;
        for (Edge eventEdge : unit.asVertex().getEdges(Direction.OUT, "lifecycleEvent")) {
            lifecycleEventCount++;
            //the lifecycleEvent should point to the SystemEvent
            Vertex lifecycleEvent = eventEdge.getVertex(Direction.IN);
            assertEquals(1, toList(lifecycleEvent.getEdges(Direction.OUT, "hasEvent")).size());
            //the lifecycleEvent should NOT have a parent lifeCycleEvent
            assertEquals(0, toList(lifecycleEvent.getEdges(Direction.OUT, "lifecycleEvent")).size());
        }
        assertEquals(1, lifecycleEventCount);

        for (Edge e : unit.asVertex().getEdges(Direction.OUT)) {
            logger.debug("out " + e.getLabel() + " " + e.getVertex(Direction.IN).getPropertyKeys());
            if (e.getLabel().equals("lifecycleEvent")) {
                for (Edge eventedge : e.getVertex(Direction.IN).getEdges(Direction.OUT)) {
                    logger.debug("lifecycleEvent out " + eventedge.getLabel() + " " + eventedge.getVertex(Direction.IN).getPropertyKeys());
                }
                for (Edge eventedge : e.getVertex(Direction.IN).getEdges(Direction.IN)) {
                    logger.debug("lifecycleEvent in " + eventedge.getLabel() + " " + eventedge.getVertex(Direction.OUT).getPropertyKeys());
                }
            }
        }
        for (Edge e : unit.asVertex().getEdges(Direction.IN)) {
            logger.debug("in " + e.getLabel() + " " + e.getVertex(Direction.OUT).getPropertyKeys());
        }

        List<SystemEvent> actions = toList(unit.getHistory());
        // we imported two eads, but in one transaction, so we expect only one Event
        assertEquals(1, actions.size());
        assertEquals(logMessage, actions.get(0).getLogMessage());

        // Check scope is correct...
        assertEquals(agent, unit.getPermissionScope());
    }
}
