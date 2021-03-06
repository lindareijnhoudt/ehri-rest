package eu.ehri.project.importers;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.PermissionScope;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class BundesarchiveSplitTest extends AbstractImporterTest{
    
    protected final String TEST_REPO = "r1";
    protected final String XMLFILE = "BA_split.xml";
    protected final String ARCHDESC = "NS 1";
    int origCount=0;
            
    @Test
    public void bundesarchiveTest() throws ItemNotFound, IOException, ValidationError, InputParseError {
        
        PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing a part of the Split Bundesarchive EAD";

        origCount = getNodeCount(graph);
        
         // Before...
       List<VertexProxy> graphState1 = getGraphState(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(XMLFILE);
        ImportLog log = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("bundesarchive.properties")).importFile(ios, logMessage);
        
 // After...
       List<VertexProxy> graphState2 = getGraphState(graph);
       GraphDiff diff = diffGraph(graphState1, graphState2);
       diff.printDebug(System.out);

        // How many new nodes will have been created? We should have
        // - 1 more DocumentaryUnits (archdesc)
       	// - 1 more DocumentDescription
        // - 1 more DatePeriod
        // - 1 more UnknownProperties
        // - 3 more Relationships
        // - 2 more import Event links (1 for every Unit, 1 for the User)
        // - 1 more import Event
        // - 5 more MaintenanceEvents (4 revised, 1 created)
        int newCount = origCount + 9+1+4+1;
        printGraph(graph);

        assertEquals(newCount, getNodeCount(graph));
        
        DocumentaryUnit archUnit = graph.frame(
                getVertexByIdentifier(graph,ARCHDESC),
                DocumentaryUnit.class);

        // Test ID generation and hierarchy
        assertEquals("nl-r1-ns-1", archUnit.getId());
        assertTrue(archUnit.asVertex().getPropertyKeys().contains(Ontology.OTHER_IDENTIFIERS));

        assertNull(archUnit.getParent());
        assertEquals(agent, archUnit.getRepository());
        assertEquals(agent, archUnit.getPermissionScope());


    //test titles
        for(DocumentDescription d : archUnit.getDocumentDescriptions()){
            assertEquals("Reichsschatzmeister der NSDAP", d.getName());
        }
    //test dates
        for(DocumentDescription d : archUnit.getDocumentDescriptions()){
        	// Single date is just a string
        	assertFalse(d.asVertex().getPropertyKeys().contains("unitDates"));
        	for (DatePeriod dp : d.getDatePeriods()){
        		assertEquals("1906-01-01", dp.getStartDate());
        		assertEquals("1919-12-31", dp.getEndDate());
                        break;
        	}
        }
        
//        // Second fonds has two dates with different types -> list
//        for(DocumentDescription d : c7_2.getDocumentDescriptions()){
//        	// unitDates still around?
//        	assertEquals("1943-1944", d.asVertex().getProperty("unitDates"));
//        	// start and end dates correctly parsed and setup
//        	for(DatePeriod dp : d.getDatePeriods()){
//        		assertEquals("1943-01-01", dp.getStartDate());
//        		assertEquals("1944-12-31", dp.getEndDate());
//        	}
//        	
//        	// Since there was a list of unitDateTypes, it should now be deleted
//        	assertNull(d.asVertex().getProperty("unitDatesTypes"));
//        }
        
    }
}
