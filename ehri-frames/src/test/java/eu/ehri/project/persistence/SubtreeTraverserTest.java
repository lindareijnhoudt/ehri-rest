package eu.ehri.project.persistence;

import eu.ehri.project.models.base.Frame;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test the subtree traverser actually works.
 * 
 * @author Mike Bryant (http://github.com/mikesname)
 *
 */
public class SubtreeTraverserTest extends AbstractFixtureTest {

    private static class Counter {
        public int count = 0;
    }

    @Test
    public void testSubtreeSerializationTouchesAllNodes() throws Exception {
        // Doc c1 has six nodes in its subtree
        final Counter counter = new Counter();
        new Serializer.Builder(graph).dependentOnly().build()
                .traverseSubtree(item, new TraversalCallback() {
            public void process(Frame vertexFrame, int depth, String rname, int rnum) {
                counter.count++;
            }
        });
        assertEquals(6, counter.count);
    }
}
