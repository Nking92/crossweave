package edu.vu.isis.crossweave;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class CrossweaveMojoTest extends AbstractMojoTestCase {

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        // required
        super.setUp();
    }
    
    /** {@inheritDoc} */
    @Override
    protected void tearDown() throws Exception {
        // required
        super.tearDown();
    }
    
    public void testRun() throws Exception {
        File testPom = new File(getBasedir(), "src/test/resources/test-run/pom.xml");
        assertNotNull(testPom);
        assertTrue(testPom.exists());
        
        CrossWeaveMojo mojo = (CrossWeaveMojo) lookupMojo("analyze", testPom);
        assertNotNull(mojo);
        mojo.execute();
        
        File outputFile = new File(getBasedir(), "src/test/resources/output/PatternStructure");
        assertNotNull(outputFile);
        assertTrue(outputFile.exists());
        
    }
}
