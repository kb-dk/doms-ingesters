package dk.statsbiblioteket.doms.ingesters.radiotv;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by abr on 21-09-16.
 */
public class UtilTest {
    @Test
    public void domsCommenter() throws Exception {
        String comment = Util.domsCommenter("testfileName", "test action with arg {0}", "testArg");
        assertEquals("RadioTV Digitv Ingester (null) test action with arg testArg as part of ingest of testfileName",comment);
    }


    @Test
    public void domsCommenterPings() throws Exception {
        String comment = Util.domsCommenter("testfileName", "test action with arg '{0}'", "testArg");
        assertEquals("RadioTV Digitv Ingester (null) test action with arg 'testArg' as part of ingest of testfileName",comment);
    }
}