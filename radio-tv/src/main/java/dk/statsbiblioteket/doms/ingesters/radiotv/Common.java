package dk.statsbiblioteket.doms.ingesters.radiotv;

import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XPathSelector;

/** Constants used while building document. */
public class Common {

    public static final String COMMENT = "Ingest of Radio/TV data";
    public static final String FAILED_COMMENT = COMMENT + ": Something failed, rolling back";
    public static final int MAX_FAIL_COUNT = 10;

    static final String PBCORE_NAMESPACE = "http://www.pbcore.org/PBCore/PBCoreNamespace.html";
    public static final String DC_NAMESPACE = "http://purl.org/dc/elements/1.1/";
    static final String RITZAU_NAMESPACE = "http://doms.statsbiblioteket.dk/types/ritzau_original/0/1/#";
    static final String GALLUP_NAMESPACE = "http://doms.statsbiblioteket.dk/types/gallup_original/0/1/#";
    static final String PROGRAM_BROADCAST_NAMESPACE = "http://doms.statsbiblioteket.dk/types/program_broadcast/0/1/#";
    public static final XPathSelector XPATH_SELECTOR = DOM
            .createXPathSelector("pbc", PBCORE_NAMESPACE,
                                 "dc", DC_NAMESPACE,
                                 "ritzau", RITZAU_NAMESPACE,
                                 "gallup", GALLUP_NAMESPACE,
                                 "pb", PROGRAM_BROADCAST_NAMESPACE);

    public static final String PBCORE_DESCRIPTION_ELEMENT
                    = "//program/pbcore/pbc:PBCoreDescriptionDocument";
    public static final String PBCORE_TITLE_ELEMENT = "//pbc:pbcoreTitle[pbc:titleType=\"titel\"]/pbc:title";
    public static final String PBCORE_RITZAU_IDENTIFIER_ELEMENT
            = "pbc:pbcoreIdentifier[pbc:identifierSource=\"id\"]/pbc:identifier";
    public static final String PBCORE_GALLUP_IDENTIFIER_ELEMENT
                    = "pbc:pbcoreIdentifier[pbc:identifierSource=\"tvmeter\"]/pbc:identifier";
    public static final String RITZAU_ORIGINALS_ELEMENT = "//program/originals/ritzau:ritzau_original";
    public static final String GALLUP_ORIGINALS_ELEMENT = "//program/originals/gallup:gallup_original|//program/originals/gallup:tvmeterProgram";
    public static final String PROGRAM_BROADCAST_ELEMENT = "//program/pb:programBroadcast";

    public static final String DC_IDENTIFIER_ELEMENT = "//dc:identifier";

    public static final String RECORDING_FILES_URLS = "//program/fileUrls/fileUrl";

    public static final String PROGRAM_TEMPLATE_PID = "doms:Template_Program";
    public static final String PROGRAM_PBCORE_DS_ID = "PBCORE";
    public static final String RITZAU_ORIGINAL_DS_ID = "RITZAU_ORIGINAL";
    public static final String GALLUP_ORIGINAL_DS_ID = "GALLUP_ORIGINAL";
    public static final String PROGRAM_BROADCAST_DS_ID = "PROGRAM_BROADCAST";
    public static final String DC_DS_ID = "DC";
    public static final String HAS_FILE_RELATION_TYPE
            = "http://doms.statsbiblioteket.dk/relations/default/0/1/#hasFile";
}
