package eu.ehri.project.importers;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.base.PermissionScope;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test the import of a Cegesoma AA EAD file. This file was based on BundesarchiveTest.java.
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 * @author Ben Companjen (http://github.com/bencomp)
 */
public class CegesomaAraTest extends AbstractImporterTest {

    private static final Logger logger = LoggerFactory.getLogger(CegesomaAraTest.class);
    protected final String TEST_REPO = "r1";
    protected final String XMLFILE = "CegesomaAA.pxml";
    protected final String ARA_XMLFILE = "araEad.xml";
    protected final String ARCHDESC = "AA 1134",
            C01 = "1234",
            C02_01 = "AA 1134 / 32",
            C02_02 = "AA 1134 / 34";
    DocumentaryUnit archdesc, c1, c2_1, c2_2;
    int origCount = 0;

    @Test
    public void cegesomaTest() throws ItemNotFound, IOException, ValidationError, InputParseError {

        PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing an example Cegesoma EAD";

        origCount = getNodeCount(graph);

        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);

        InputStream ios = ClassLoader.getSystemResourceAsStream(XMLFILE);
        ImportLog log = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("cegesomaAA.properties")).importFile(ios, logMessage);
        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
//       diff.printDebug(System.out);

        printGraph(graph);
        // How many new nodes will have been created? We should have
        /**
         * event links: 6 relationship: 34 documentaryUnit: 5 documentDescription: 5 systemEvent: 1 datePeriod: 4
         * maintenanceEvent: 1
         */
        int newCount = origCount + 56;
        assertEquals(newCount, getNodeCount(graph));

        archdesc = graph.frame(
                getVertexByIdentifier(graph, ARCHDESC),
                DocumentaryUnit.class);
        c1 = graph.frame(
                getVertexByIdentifier(graph, C01),
                DocumentaryUnit.class);
        c2_1 = graph.frame(
                getVertexByIdentifier(graph, C02_01),
                DocumentaryUnit.class);
        c2_2 = graph.frame(
                getVertexByIdentifier(graph, C02_02),
                DocumentaryUnit.class);

        // Test ID generation is correct
        assertEquals("nl-r1-aa-1134-1234", c1.getId());
        assertEquals(c1.getId() + "-aa-1134-32", c2_1.getId());
        assertEquals(c1.getId() + "-aa-1134-34", c2_2.getId());

        for (String key : archdesc.asVertex().getPropertyKeys()) {
            logger.debug(key + " " + archdesc.asVertex().getProperty(key));
        }
        assertTrue(((List<String>) archdesc.asVertex().getProperty(Ontology.OTHER_IDENTIFIERS)).contains("AA 627"));

        InputStream ios_ara = ClassLoader.getSystemResourceAsStream(ARA_XMLFILE);
        importManager = new SaxImportManager(graph, repository, validUser, AraEadImporter.class, EadHandler.class, new XmlImportProperties("ara.properties"))
                .setTolerant(Boolean.TRUE);

        ImportLog log_ara = importManager.importFile(ios_ara, logMessage);
        for (String key : archdesc.asVertex().getPropertyKeys()) {
            logger.debug(key + " " + archdesc.asVertex().getProperty(key));
        }
        assertTrue(archdesc.asVertex().getPropertyKeys().contains(Ontology.OTHER_IDENTIFIERS));
        assertTrue(((List<String>) archdesc.asVertex().getProperty(Ontology.OTHER_IDENTIFIERS)).contains("AA 627"));
        assertTrue(((List<String>) archdesc.asVertex().getProperty(Ontology.OTHER_IDENTIFIERS)).contains("AC559"));
        

    }
}
