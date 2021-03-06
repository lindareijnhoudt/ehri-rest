package eu.ehri.project.importers;

import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class VC_ARAImporterTest extends AbstractImporterTest{
    
       protected final String SINGLE_EAD = "araEad.xml";
       protected final String VC_EAD = "araVcEad.xml";

       // Depends on fixtures
    protected final String TEST_REPO ="r1";

    @Test
    public void testImportItemsT() throws Exception {

        final String logMessage = "Importing a single EAD";

        int origCount = getNodeCount(graph);
        System.out.println(origCount);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        importManager = new SaxImportManager(graph, repository, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("ara.properties"))
                .setTolerant(Boolean.TRUE);
        
                 // Before...
       List<VertexProxy> graphState1 = getGraphState(graph);
        ImportLog log = importManager.importFile(ios, logMessage);
        printGraph(graph);
        
 // After...
       List<VertexProxy> graphState2 = getGraphState(graph);
       GraphDiff diff = diffGraph(graphState1, graphState2);
       diff.printDebug(System.out);

        
        printGraph(graph);
       
        InputStream ios_vc = ClassLoader.getSystemResourceAsStream(VC_EAD);
        importManager = new SaxImportManager(graph, repository, validUser, VirtualEadImporter.class, VirtualEadHandler.class, new XmlImportProperties("vc_ara.properties"))
                .setTolerant(Boolean.TRUE);

ImportLog log_vc = importManager.importFile(ios_vc, logMessage);
        
    }

}
