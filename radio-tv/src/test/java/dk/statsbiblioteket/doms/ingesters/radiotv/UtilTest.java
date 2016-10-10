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


    @Test
    public void doms2Comments() throws Exception {
        String comment = Util.domsCommenter("testfileName", "test action with arg {0} and {1}", "testArg", " testarg2 ");
        assertEquals("RadioTV Digitv Ingester (null) test action with arg testArg and  testarg2  as part of ingest of testfileName",comment);
    }

    @Test
    public void weirdFilename() throws Exception {
        String comment = Util.domsCommenter("testfileName{0}", "test action with arg {0}", "testArg");
        assertEquals("RadioTV Digitv Ingester (null) test action with arg testArg as part of ingest of testfileName{0}",comment);
    }

    @Test
    public void repeatedComments() throws Exception {
        String comment = Util.domsCommenter("testfileName", "test action with arg {0} and {0}", "testArg");
        assertEquals("RadioTV Digitv Ingester (null) test action with arg testArg and testArg as part of ingest of testfileName",comment);
    }

    @Test
    public void unusedComments() throws Exception {
        String comment = Util.domsCommenter("testfileName", "test action with arg {0}", "testArg", " testArg2 ");
        assertEquals("RadioTV Digitv Ingester (null) test action with arg testArg as part of ingest of testfileName",comment);
    }

    @Test
    public void indexOutOfBounds() throws Exception {
        String comment = Util.domsCommenter("testfileName", "test action with arg {5}", "testArg");
        assertEquals("RadioTV Digitv Ingester (null) test action with arg {5} as part of ingest of testfileName",comment);
    }

    @Test
    public void nestedArgs() throws Exception {
        String comment = Util.domsCommenter("testfileName", "test action with arg {0}", "testArg{0}");
        assertEquals("RadioTV Digitv Ingester (null) test action with arg testArg{0} as part of ingest of testfileName",comment);
    }

    @Test
    public void nestedArgs2() throws Exception {
        String comment = Util.domsCommenter("testfileName", "test action with arg {0}", "testArg{1}", "arg2");
        assertEquals("RadioTV Digitv Ingester (null) test action with arg testArg{1} as part of ingest of testfileName",comment);
    }

    @Test
    public void newLines() throws Exception {
        String comment = Util.domsCommenter("testfile\nName\n", "test action with \narg {0}", "\ntestArg\n");
        assertEquals("RadioTV Digitv Ingester (null) test action with \narg \ntestArg\n as part of ingest of testfile\nName\n",comment);
    }


}