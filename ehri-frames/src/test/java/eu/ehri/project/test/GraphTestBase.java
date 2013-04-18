package eu.ehri.project.test;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.test.utils.fixtures.FixtureLoaderFactory;
import org.junit.After;
import org.junit.Before;
import org.neo4j.test.TestGraphDatabaseFactory;

/**
 * User: michaelb
 */
public abstract class GraphTestBase {

    protected FramedGraph<Neo4jGraph> graph;
    protected GraphManager manager;

    @Before
    public void setUp() throws Exception {
        graph = new FramedGraph<Neo4jGraph>(new Neo4jGraph(
                new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder()
                        .newGraphDatabase()));
        manager = GraphManagerFactory.getInstance(graph);
    }

    @After
    public void tearDown() throws Exception {
        graph.shutdown();
    }
}