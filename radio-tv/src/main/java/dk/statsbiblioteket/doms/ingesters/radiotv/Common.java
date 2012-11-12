package dk.statsbiblioteket.doms.ingesters.radiotv;

import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XPathSelector;

/**
 * Created with IntelliJ IDEA.
 * User: abr
 * Date: 11/12/12
 * Time: 5:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class Common {
    static final String RECORDING_PBCORE_DESCRIPTION_DOCUMENT_ELEMENT
                    = "//program/pbcore/pbc:PBCoreDescriptionDocument";
    static final String COMMENT = "Ingest of Radio/TV data";
    static final String FAILED_COMMENT = COMMENT + ": Something failed, rolling back";
    static final XPathSelector XPATH_SELECTOR = DOM
            .createXPathSelector("pbc", "http://www.pbcore.org/PBCore/PBCoreNamespace.html");
    static final String RITZAU_ORIGINALS_ELEMENT = "//program/originals/ritzau_original";
    static final String GALLUP_ORIGINALS_ELEMENT = "//program/originals/gallup_original";
    static final String RECORDING_FILES_FILE_ELEMENT = "//program/program_recording_files/file";
    static final String FILE_URL_ELEMENT = "file_url";
    static final String HAS_FILE_RELATION_TYPE
                    = "http://doms.statsbiblioteket.dk/relations/default/0/1/#hasFile";
    static final String PROGRAM_TEMPLATE_PID = "doms:Template_Program";
    static final String PROGRAM_PBCORE_DS_ID = "PBCORE";
    static final String RITZAU_ORIGINAL_DS_ID = "RITZAU_ORIGINAL";
    static final String GALLUP_ORIGINAL_DS_ID = "GALLUP_ORIGINAL";
    static final String PROGRAM_BROADCAST_DS_ID = "PROGRAM_BROADCAST";
    private static final String RADIO_TV_FILE_TEMPLATE_PID = "doms:Template_RadioTVFile";
    static final int MAX_FAIL_COUNT = 10;
}
