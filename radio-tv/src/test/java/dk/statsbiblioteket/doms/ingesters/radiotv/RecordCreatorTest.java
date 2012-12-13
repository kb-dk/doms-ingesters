package dk.statsbiblioteket.doms.ingesters.radiotv;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import dk.statsbiblioteket.doms.central.RecordDescription;
import dk.statsbiblioteket.doms.client.DomsWSClient;
import dk.statsbiblioteket.doms.client.DomsWSClientImpl;
import dk.statsbiblioteket.doms.client.NoObjectFound;

import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * Trivial test of ingester
 */
public class RecordCreatorTest {
    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testIngestProgram() throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        Document metadataDocument = documentBuilderFactory.newDocumentBuilder().parse(getClass().getResource("/2012-11-14_23-20-00_dr1.xml").getFile());
        DomsWSClient testDomsClient = new TestDomsWSClient();
        new RecordCreator(testDomsClient).ingestProgram(metadataDocument);
    }

    @Ignore
    @Test
    public void testIngestProgramRealDOMS() throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        Document metadataDocument = documentBuilderFactory.newDocumentBuilder().parse(getClass().getResource("/2012-11-15_09-40-00_dr1.xml").getFile());
        DomsWSClient testDomsClient = new DomsWSClientImpl();
        testDomsClient.login(new URL("http://alhena:7880/centralWebservice-service/central/?wsdl"), "fedoraAdmin", "fedoraAdminPass");
        new RecordCreator(testDomsClient).ingestProgram(metadataDocument);
    }

    private static class TestDomsWSClient implements DomsWSClient {
        @Override
        public void login(URL url, String s, String s1) {
            
        }

        @Override
        public void setCredentials(URL url, String s, String s1) {
            
        }

        @Override
        public String createObjectFromTemplate(String s, String s1)
                throws dk.statsbiblioteket.doms.client.ServerOperationFailed {
            return null;  
        }

        @Override
        public String createObjectFromTemplate(String s, List<String> strings, String s1)
                throws dk.statsbiblioteket.doms.client.ServerOperationFailed {
            return null;  
        }

        @Override
        public String createFileObject(String s, dk.statsbiblioteket.doms.client.FileInfo fileInfo, String s1)
                throws dk.statsbiblioteket.doms.client.ServerOperationFailed {
            return null;  
        }

        @Override
        public void addFileToFileObject(String s, dk.statsbiblioteket.doms.client.FileInfo fileInfo, String s1)
                throws dk.statsbiblioteket.doms.client.ServerOperationFailed {
            
        }

        @Override
        public String getFileObjectPID(URL url) throws dk.statsbiblioteket.doms.client.NoObjectFound,
                dk.statsbiblioteket.doms.client.ServerOperationFailed {
            return null;  
        }

        @Override
        public List<String> getPidFromOldIdentifier(String s) throws dk.statsbiblioteket.doms.client.NoObjectFound,
                dk.statsbiblioteket.doms.client.ServerOperationFailed {
            throw new NoObjectFound();
        }

        @Override
        public Document getDataStream(String s, String s1)
                throws dk.statsbiblioteket.doms.client.ServerOperationFailed {
            return null;  
        }

        @Override
        public void updateDataStream(String s, String s1, Document document, String s2)
                throws dk.statsbiblioteket.doms.client.ServerOperationFailed {
            
        }

        @Override
        public void addObjectRelation(dk.statsbiblioteket.doms.client.Relation relation, String s)
                throws dk.statsbiblioteket.doms.client.ServerOperationFailed {
            
        }

        @Override
        public void removeObjectRelation(dk.statsbiblioteket.doms.client.Relation relation, String s)
                throws dk.statsbiblioteket.doms.client.ServerOperationFailed {
            
        }

        @Override
        public List<dk.statsbiblioteket.doms.client.Relation> listObjectRelations(String s, String s1)
                throws dk.statsbiblioteket.doms.client.ServerOperationFailed {
            return Collections.emptyList();
        }

        @Override
        public void publishObjects(String s, String... strings)
                throws dk.statsbiblioteket.doms.client.ServerOperationFailed {
            
        }

        @Override
        public void unpublishObjects(String s, String... strings)
                throws dk.statsbiblioteket.doms.client.ServerOperationFailed {
            
        }

        @Override
        public void deleteObjects(String s, String... strings)
                throws dk.statsbiblioteket.doms.client.ServerOperationFailed {
            
        }

        @Override
        public long getModificationTime(String s, String s1, String s2)
                throws dk.statsbiblioteket.doms.client.ServerOperationFailed {
            return 0;  
        }

        @Override
        public List<RecordDescription> getModifiedEntryObjects(String s, String s1, long l, String s2, long l1, long l2)
                throws dk.statsbiblioteket.doms.client.ServerOperationFailed {
            return null;  
        }

        @Override
        public String getViewBundle(String s, String s1) throws dk.statsbiblioteket.doms.client.ServerOperationFailed {
            return null;  
        }

        @Override
        public void setObjectLabel(String s, String s1, String s2)
                throws dk.statsbiblioteket.doms.client.ServerOperationFailed {
            
        }
    }
}
