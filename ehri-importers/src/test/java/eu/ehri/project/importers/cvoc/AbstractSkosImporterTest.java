package eu.ehri.project.importers.cvoc;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public abstract class AbstractSkosImporterTest extends AbstractFixtureTest {
    private static final Logger logger = LoggerFactory.getLogger(AbstractSkosImporterTest.class);
    public static String FILE1 = "cvoc/simple.xml";
    public static String FILE2 = "cvoc/simple.n3";
    public static String FILE3 = "cvoc/repository-types.xml";
    public static String FILE4 = "cvoc/camps.rdf";
    public static String FILE5 = "cvoc/ghettos.rdf";

    protected Actioner actioner;
    protected Vocabulary vocabulary;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        actioner = manager.cast(validUser, Actioner.class);
        vocabulary = manager.getFrame("cvoc2", Vocabulary.class);
    }
    
     protected void printGraph(FramedGraph<?> graph) {
        int vcount = 0;
        for (Vertex v : graph.getVertices()) {
            logger.debug(++vcount + " -------------------------");
            for (String key : v.getPropertyKeys()) {
                String value = "";
                if (v.getProperty(key) instanceof String[]) {
                    String[] list = (String[]) v.getProperty(key);
                    for (String o : list) {
                        value += "[" + o + "] ";
                    }
                } else {
                    value = v.getProperty(key).toString();
                }
                logger.debug(key + ": " + value);
            }

            for (Edge e : v.getEdges(Direction.OUT)) {
                logger.debug(e.getLabel());
            }
        }
    }

}
