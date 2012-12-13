package dk.statsbiblioteket.doms.ingesters.radiotv;

import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XPathSelector;

/** Constants used while building document. */
public class Common {
    static final String COMMENT = "Ingest of Radio/TV data";
    static final String FAILED_COMMENT = COMMENT + ": Something failed, rolling back";
    static final int MAX_FAIL_COUNT = 10;

    static final String PBCORE_NAMESPACE = "http://www.pbcore.org/PBCore/PBCoreNamespace.html";
    static final String DC_NAMESPACE = "http://purl.org/dc/elements/1.1/";
    static final String RITZAU_NAMESPACE = "http://doms.statsbiblioteket.dk/types/ritzau_original/0/1/#";
    static final String GALLUP_NAMESPACE = "http://doms.statsbiblioteket.dk/types/gallup_original/0/1/#";
    static final String PROGRAM_BROADCAST_NAMESPACE = "http://doms.statsbiblioteket.dk/types/program_broadcast/0/1/#";
    static final XPathSelector XPATH_SELECTOR = DOM
            .createXPathSelector("pbc", PBCORE_NAMESPACE,
                                 "dc", DC_NAMESPACE,
                                 "ritzau", RITZAU_NAMESPACE,
                                 "gallup", GALLUP_NAMESPACE,
                                 "pb", PROGRAM_BROADCAST_NAMESPACE);
    static final String PBCORE_DESCRIPTION_ELEMENT
                    = "//program/pbcore/pbc:PBCoreDescriptionDocument";
    static final String DC_IDENTIFIER_ELEMENT = "//dc:identifier";
    static final String PBCORE_TITLE_ELEMENT = "//pbc:pbcoreTitle[pbc:titleType=\"titel\"]/pbc:title";
    static final String PBCORE_RITZAU_IDENTIFIER_ELEMENT
            = "pbc:pbcoreIdentifier[pbc:identifierSource=\"id\"]/pbc:identifier";
    static final String PBCORE_GALLUP_IDENTIFIER_ELEMENT
                    = "pbc:pbcoreIdentifier[pbc:identifierSource=\"tvmeter\"]/pbc:identifier";
    static final String RITZAU_ORIGINALS_ELEMENT = "//program/originals/ritzau:ritzau_original";
    static final String GALLUP_ORIGINALS_ELEMENT = "//program/originals/gallup:gallup_original|//program/originals/gallup:tvmeterProgram";
    static final String PROGRAM_BROADCAST_ELEMENT = "//program/pb:programBroadcast";
    static final String RECORDING_FILES_URLS = "//program/fileUrls/fileUrl";

    static final String PROGRAM_TEMPLATE_PID = "doms:Template_Program";
    static final String PROGRAM_PBCORE_DS_ID = "PBCORE";
    static final String RITZAU_ORIGINAL_DS_ID = "RITZAU_ORIGINAL";
    static final String GALLUP_ORIGINAL_DS_ID = "GALLUP_ORIGINAL";
    static final String PROGRAM_BROADCAST_DS_ID = "PROGRAM_BROADCAST";
    static final String DC_DS_ID = "DC";
    static final String HAS_FILE_RELATION_TYPE
            = "http://doms.statsbiblioteket.dk/relations/default/0/1/#hasFile";
}
