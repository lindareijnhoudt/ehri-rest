package eu.ehri.project.importers.properties;

import eu.ehri.project.models.EntityClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class PropertiesCheckerTest {

    PropertiesChecker p;

    @Before
    public void init() {
        NodeProperties pc = new NodeProperties();
        pc.setTitles(NodeProperties.NODE + NodeProperties.SEP + 
                NodeProperties.PROPERTY + NodeProperties.SEP + 
                NodeProperties.HANDLERNAME + NodeProperties.SEP + 
                NodeProperties.REQUIRED + NodeProperties.SEP + 
                NodeProperties.MULTIVALUED);
        pc.addRow("unit,identifier,objectIdentifier,1,");
        pc.addRow("description,identifier,descriptionIdentifier,1,");
        pc.addRow("description,languageCode,,1,");
        pc.addRow("description,name,,1,");
        pc.addRow("repositoryDescription,typeOfEntity,,,");
        pc.addRow("repositoryDescription,otherFormsOfName,,,1");
        pc.addRow("repositoryDescription,parallelFormsOfName,,,");
        pc.addRow("repositoryDescription,history,,,");
        pc.addRow("repositoryDescription,generalContext,,,");
        p = new PropertiesChecker(pc);
    }

    public PropertiesCheckerTest() {
    }

    @Test
    public void testCheck() {
        assertTrue(p.check(new XmlImportProperties("eag.properties"), EntityClass.REPOSITORY_DESCRIPTION));
    }
}
