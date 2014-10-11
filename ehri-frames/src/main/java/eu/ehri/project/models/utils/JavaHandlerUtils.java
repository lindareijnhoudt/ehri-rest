package eu.ehri.project.models.utils;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: mike
 * <p/>
 * Utilities for dealing with Gremlin pipelines.
 */
public class JavaHandlerUtils {

    public static final Logger logger = LoggerFactory.getLogger(JavaHandlerUtils.class);

    public static final int LOOP_MAX = 20;

    /**
     * Pipe function that quits after a certain number of loops
     */
    public static <S> PipeFunction<LoopPipe.LoopBundle<S>, Boolean> maxLoopFuncFactory(final int maxLoops) {
        return new PipeFunction<LoopPipe.LoopBundle<S>,
                Boolean>() {
            @Override
            public Boolean compute(LoopPipe.LoopBundle<S> vertexLoopBundle) {
                return vertexLoopBundle.getLoops() < maxLoops;
            }
        };
    }

    /**
     * Pipe function with a max loop clause with the default of 20.
     */
    public static PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean> defaultMaxLoops
            = maxLoopFuncFactory(LOOP_MAX);

    /**
     * Pipe function that always allows looping to continue.
     */
    public static PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean> noopLoopFunc
            = new PipeFunction<com.tinkerpop.pipes.branch.LoopPipe.LoopBundle<Vertex>, Boolean>() {
        @Override
        public Boolean compute(com.tinkerpop.pipes.branch.LoopPipe.LoopBundle<Vertex> vertexLoopBundle) {
            return true;
        }
    };

    /**
     * Given a pipeline
     *
     * @param element  The element
     * @param propName The cache property name
     * @param pipe     A pipeline to count
     */
    public static <S, E> void cacheCount(Element element, GremlinPipeline<S, E> pipe, String propName) {
        element.setProperty(propName, pipe.count());
    }

    /**
     * Add a relationship in such a way that it is the only one of its type
     * from the source to the target.
     *
     * @param from  The source vertex
     * @param to    The target vertex
     * @param label The relationship label name
     * @return Whether or not changes were made to the graph.
     */
    public static boolean addSingleRelationship(Vertex from, Vertex to, String label) {
        if (!from.equals(to)) {
            for (Edge edge : from.getEdges(Direction.OUT, label)) {
                if (edge.getVertex(Direction.IN).equals(to)) {
                    logger.warn("Attempting to add relationship '{}' that already exists: {}", label, to);
                    return false;
                } else {
                    edge.remove();
                    logger.warn("Removed prior '{}' relationship added in single mode: {}",
                            label, from);
                }
            }
            from.addEdge(label, to);
            return true;
        } else {
            logger.warn("Attempt to add self-referential '{}' relationship " +
                    "where single relationship specified: {}",
                    label, to);
            return false;
        }
    }

    /**
     * Add a relationship that can only exist once between source and target.
     *
     * @param from  The source vertex
     * @param to    The target vertex
     * @param label The relationship label name
     * @return Whether or not changes were made to the graph.
     */
    public static boolean addUniqueRelationship(Vertex from, Vertex to, String label) {
        for (Edge edge : from.getEdges(Direction.OUT, label)) {
            if (edge.getVertex(Direction.IN).equals(to)) {
                return false;
            }
        }
        from.addEdge(label, to);
        return true;
    }

    /**
     * Determine if a relationship with the given label(s) exists
     * between source and target vertex.
     *
     * @param from   The source vertex
     * @param to     The target vertex
     * @param labels The set of labels
     * @return If a matching relationship is found
     */
    public static boolean hasRelationship(Vertex from, Vertex to, String... labels) {
        for (Edge edge : from.getEdges(Direction.OUT, labels)) {
            if (edge.getVertex(Direction.IN).equals(to)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove all relationships with the given labels between
     * source and target vertex.
     *
     * @param from   The source vertex
     * @param to     The target vertex
     * @param labels The set of labels
     */
    public static boolean removeAllRelationships(Vertex from, Vertex to, String... labels) {
        int removed = 0;
        for (Edge edge : from.getEdges(Direction.OUT, labels)) {
            if (edge.getVertex(Direction.IN).equals(to)) {
                edge.remove();
                removed++;
            }
        }
        return removed > 0;
    }

    /**
     * Check if a vertex has edges with the given direct/label.
     *
     * @param vertex    The source vertex
     * @param direction The edge direction
     * @param labels    The set of labels
     * @return Whether any edges exist
     */
    public static boolean hasEdge(Vertex vertex, Direction direction, String... labels) {
        return vertex.getEdges(direction, labels).iterator().hasNext();
    }
}
