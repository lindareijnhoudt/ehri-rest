package eu.ehri.project.importers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.tinkerpop.blueprints.TransactionalGraph.Conclusion;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ead.EadLanguageExtractor;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.BundleFactory;
import eu.ehri.project.persistance.EntityBundle;

/**
 * Import EAD for a given repository into the database. Due to the laxness of
 * the EAD standard this is a fairly complex procedure. An EAD a single entity
 * at the highest level of description or multiple top-level entities, with or
 * without a heirarchical structure describing their child items. This means
 * that we need to recursively descend through the archdesc and c01-12 levels.
 * 
 * TODO: Extensive cleanups, optimisation, and rationalisation.
 * 
 * @author michaelb
 * 
 */
public class EadImporter extends BaseImporter<Node> {

    private static final Logger logger = LoggerFactory
            .getLogger(EadImporter.class);

    private final XPath xpath;
    private EadLanguageExtractor langHelper;

    // An integer that represents how far down the
    // EAD heirarchy tree the current document is.
    public final String DEPTH_ATTR = "depthOfDescription";

    // A (possibly arbitrary) string denoting what the
    // describing body saw fit to name a documentary unit's
    // level of description.
    public final String LEVEL_ATTR = "levelOfDescription";

    // List of XPaths against the final attribute names. This should probably be
    // moved
    // to external configuration...
    @SuppressWarnings("serial")
    public final Map<String, String> eadAttributeMap = new HashMap<String, String>() {
        {
            put("accruals", "accurals/p");
            put("acquisition", "acqinfo/p");
            put("appraisal", "appraisal/p");
            put("archivalHistory", "custodhist/p");
            put("conditionsOfAccess", "accessrestrict/p");
            put("conditionsOfReproduction", "userestrict/p");
            put("extentAndMedium", "did/physdesc/extent"); // Should be more
                                                           // nuanced!
            put("identifier", "did/unitid");
            put("locationOfCopies", "altformavail/p");
            put("locationOfOriginals", "originalsloc/p");
            put("title", "did/unittitle");
            put("physicalCharacteristics", "phystech/p");
            put("scopeAndContent", "scopecontent/p");
            put("systemOfArrangement", "arrangement/p");
        }
    };

    @SuppressWarnings("serial")
    public final Map<String, String> eadControlaccessMap = new HashMap<String, String>() {
        {
            put("subjects", "controlaccess/subjects");
            put("creators", "did/origination/persname");
            put("creators", "did/origination/corpname");
            put("nameAccess", "controlaccess/persname");
            put("corporateBodies", "controlaccess/corpname");
            put("places", "controlaccess/geogname");
            put("unitDates", "did/unitdate");
        }
    };

    // Pattern for EAD nodes that represent a child item
    private Pattern childItemPattern = Pattern.compile("^c(?:\\d+)$");

    // Various date patterns
    private Pattern[] datePatterns = {
            // Yad Vashem, ICA-Atom style: 1924-1-1 - 1947-12-31
            Pattern.compile("^(\\d{4}-\\d{1,2}-\\d{1,2})\\s?-\\s?(\\d{4}-\\d{1,2}-\\d{1,2})$"),
            Pattern.compile("^(\\d{4})\\s?-\\s?(\\d{4})$"),
            Pattern.compile("^(\\d{4})-\\[(\\d{4})\\]$"),
            Pattern.compile("^(\\d{4})-\\[(\\d{4})\\]$"),
            Pattern.compile("^(\\d{4}s)-\\[(\\d{4}s)\\]$"),
            Pattern.compile("^\\[(\\d{4})\\]$"), Pattern.compile("^(\\d{4})$"),
            Pattern.compile("^(\\d{2})th century$") };

    /**
     * Construct an EadImporter object.
     * 
     * @param framedGraph
     * @param repository
     * @param topLevelEad
     */
    public EadImporter(FramedGraph<Neo4jGraph> framedGraph, Agent repository,
            Node topLevelEad) {

        super(framedGraph, repository);
        xpath = XPathFactory.newInstance().newXPath();
        langHelper = new EadLanguageExtractor(xpath, topLevelEad);
    }

    /**
     * Extract a list of entity bundles for DatePeriods from the
     * data, attempting to parse the unitdate attribute.
     * 
     * @param data
     */
    @Override
    public List<EntityBundle<DatePeriod>> extractDates(Node data)
            throws XPathExpressionException, ValidationError {
        List<EntityBundle<DatePeriod>> dates = new LinkedList<EntityBundle<DatePeriod>>();
        for (String date : extractTextList(data, "did/unitdate")) {
            EntityBundle<DatePeriod> dpb = extractDate(date);
            if (dpb != null)
                dates.add(dpb);
        }
        return dates;
    }

    public List<Node> extractChildData(Node data) {
        List<Node> children = new LinkedList<Node>();
        logger.debug("Extracting child data from: " + data);
        for (int i = 0; i < data.getChildNodes().getLength(); i++) {
            Node n = data.getChildNodes().item(i);
            if (childItemPattern.matcher(n.getNodeName()).matches()) {
                children.add(n);
            }
        }
        return children;
    }

    @Override
    protected EntityBundle<DocumentaryUnit> extractDocumentaryUnit(Node data,
            int depth) throws Exception {

        Map<String, Object> dataMap = new HashMap<String, Object>();
        dataMap.put(
                "identifier",
                xpath.compile("did/unitid/text()").evaluate(data,
                        XPathConstants.STRING));
        dataMap.put(
                "name",
                xpath.compile("did/unittitle/text()").evaluate(data,
                        XPathConstants.STRING));

        // Add persname, origination etc
        for (Entry<String, String> entry : eadControlaccessMap.entrySet()) {
            List<String> vals = extractTextList(data, entry.getValue());
            dataMap.put(entry.getKey(), vals.toArray(new String[vals.size()]));
        }

        return new BundleFactory<DocumentaryUnit>().buildBundle(dataMap,
                DocumentaryUnit.class);
    }

    @Override
    protected void importItems(Node data, DocumentaryUnit parent, int depth)
            throws Exception {
        XPathExpression expr = xpath.compile("did/unitid/text()");

        String topLevelId = (String) expr.evaluate(data, XPathConstants.STRING);
        // If we have a unitid at archdesc level, import that
        if (!topLevelId.isEmpty()) {
            logger.info("Extracting single item from archdesc... " + topLevelId);
            importItem(data, parent, depth);
        } else {
            // Otherwise, inspect the children of the archdesc/dsc
            logger.info("Extracting multiple items from archdesc/dsc...");
            NodeList dsc = (NodeList) xpath.compile("dsc").evaluate(data,
                    XPathConstants.NODESET);
            for (int i = 0; i < dsc.getLength(); i++) {
                for (Node d : extractChildData(dsc.item(i))) {
                    importItem(d, parent, depth + 1);
                }
            }
        }
    }

    public List<EntityBundle<DocumentDescription>> extractDocumentDescriptions(
            Node data, int depth) throws Exception {
        List<EntityBundle<DocumentDescription>> descs = new LinkedList<EntityBundle<DocumentDescription>>();

        Map<String, Object> dataMap = new HashMap<String, Object>();

        // For EAD (and most other types) there will only be one description
        // per logical object we create.
        for (Entry<String, String> entry : eadAttributeMap.entrySet()) {
            dataMap.put(entry.getKey(), getElementText(data, entry.getValue()));
        }

        // Set level of description and actual level of nesting
        dataMap.put(LEVEL_ATTR, getLevelOfDescription(data));
        dataMap.put(DEPTH_ATTR, depth);

        // FIXME: TODO: Actually detect correct language of description...
        List<String> langCodes = langHelper.getLanguagesOfDescription(data);
        langCodes.addAll(langHelper.getGlobalLanguagesOfDescription());
        dataMap.put("languageOfDescription", getUniqueArray(langCodes));

        // Set language of materials
        List<String> materialCodes = langHelper.getLanguagesOfMaterial(data);
        materialCodes.addAll(langHelper.getGlobalLanguagesOfMaterial());
        dataMap.put("languageOfMaterial", getUniqueArray(materialCodes));

        descs.add(new BundleFactory<DocumentDescription>().buildBundle(dataMap,
                DocumentDescription.class));
        return descs;
    }

    // Helpers

    /**
     * Make a list unique.
     * 
     * @param list
     * @return
     */
    private String[] getUniqueArray(List<String> list) {
        HashSet<String> hs = new HashSet<String>();
        hs.addAll(list);
        return hs.toArray(new String[list.size()]);
    }

    /**
     * Extract a list of strings from the specified element set.
     * 
     * @param data
     * @param path
     * @return
     * @throws Exception
     */
    private List<String> extractTextList(Node data, String path)
            throws XPathExpressionException {
        List<String> names = new ArrayList<String>();

        NodeList nodes = (NodeList) xpath.compile(path).evaluate(data,
                XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            names.add(nodes.item(i).getTextContent());
        }
        return names;
    }

    private String getLevelOfDescription(Node data) {
        Node attr = data.getAttributes().getNamedItem("level");
        if (attr != null)
            return attr.getNodeValue();
        return "";
    }

    /**
     * Attempt to extract some date periods. This does not currently put the
     * dates into ISO form.
     * 
     * @param date
     * @return
     * @throws ValidationError
     */
    private EntityBundle<DatePeriod> extractDate(String date)
            throws ValidationError {
        Map<String, Object> data = new HashMap<String, Object>();
        boolean ok = false;
        for (Pattern re : datePatterns) {
            Matcher matcher = re.matcher(date);
            if (matcher.matches()) {
                data.put("startDate", matcher.group(1));
                data.put("endDate",
                        matcher.group(matcher.groupCount() > 1 ? 2 : 1));
                ok = true;
                break;
            }
        }

        return ok ? new BundleFactory<DatePeriod>().buildBundle(data,
                DatePeriod.class) : null;
    }    
    
    /**
     * Fetch the element text from (possibly) multiple nodes, and join them with
     * a double break.
     * 
     * @param data
     * @param path
     * @return
     * @throws XPathExpressionException
     */
    private String getElementText(Node data, String path)
            throws XPathExpressionException {
        NodeList nodes = (NodeList) xpath.compile(path).evaluate(data,
                XPathConstants.NODESET);
        String out = "";
        for (int i = 0; i < nodes.getLength(); i++) {
            if (out.isEmpty()) {
                out += nodes.item(i).getTextContent();
            } else {
                out += "\n\n" + nodes.item(i).getTextContent();
            }
        }
        return out;
    }

    // Command line runner
    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addOption(new Option("createrepo", false,
                "Create agent with the given ID"));
        CommandLineParser parser = new PosixParser();
        CommandLine cmdLine = parser.parse(options, args);

        if (cmdLine.getArgList().size() != 3)
            throw new RuntimeException(
                    "Usage: importer [OPTIONS] <agent-id> <ead.xml> <neo4j-graph-dir>");

        // Get the graph and search it for the required agent...
        FramedGraph<Neo4jGraph> graph = new FramedGraph<Neo4jGraph>(
                new Neo4jGraph((String) cmdLine.getArgList().get(2)));

        Agent agent = null;
        if (cmdLine.hasOption("createrepo")) {
            try {
                Map<String, Object> agentData = new HashMap<String, Object>();
                agentData.put("identifier", cmdLine.getArgList().get(0));
                agentData.put("name", cmdLine.getArgList().get(0));
                EntityBundle<Agent> agb = new BundleFactory<Agent>()
                        .buildBundle(agentData, Agent.class);
                agent = new BundleDAO<Agent>(graph).insert(agb);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            agent = graph
                    .getVertices("identifier",
                            (String) cmdLine.getArgList().get(0), Agent.class)
                    .iterator().next();
        }

        // XML parsing boilerplate...
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse((String) cmdLine.getArgList().get(1));

        // Extract the EAD instances from the eadlist.
        XPathFactory xfactory = XPathFactory.newInstance();
        XPath xpath = xfactory.newXPath();
        xpath.compile("//eadlist/ead");

        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            // This handles either multiple EADs within a top-level
            // eadlist node, or single EAD documents.
            EadImporter importer;
            NodeList result = (NodeList) xpath.compile("//eadlist/ead")
                    .evaluate(doc, XPathConstants.NODESET);
            if (result.getLength() > 0) {
                for (int i = 0; i < result.getLength(); i++) {
                    importer = new EadImporter(graph, agent, result.item(i));
                    Node archDesc = (Node) xpath.compile("archdesc").evaluate(
                            result.item(i), XPathConstants.NODE);
                    // Initialize the importer...
                    importer.importItems(archDesc);
                }
            } else {
                Node ead = (Node) xpath.compile("//ead").evaluate(doc,
                        XPathConstants.NODE);
                importer = new EadImporter(graph, agent, ead);
                Node archDesc = (Node) xpath.compile("//ead/archdesc")
                        .evaluate(doc, XPathConstants.NODE);
                importer.importItems(archDesc);
            }
            graph.getBaseGraph().stopTransaction(Conclusion.SUCCESS);
            tx.success();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            graph.getBaseGraph().stopTransaction(Conclusion.FAILURE);
            tx.failure();
        } finally {
            tx.finish();
            graph.shutdown();
        }
    }
}