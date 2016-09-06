package dk.statsbiblioteket.doms.ingesters.radiotv;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.w3c.dom.Document;

import dk.statsbiblioteket.doms.client.DomsWSClient;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Trivial test of ingester
 */
public class RecordCreatorTest {
    private SimpleDateFormat origDateFormat;

    @Before
    public void setUp() throws Exception {
        origDateFormat = new SimpleDateFormat("yyyy-mm-dd HH:MM:SS.s");
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testIngestProgram() throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        String filename = "2012-11-14_23-20-00_dr1.xml";




        String programPid = "uuid:"+UUID.randomUUID().toString();

        URL fileURL1 = new URL(
                "http://bitfinder.statsbiblioteket.dk/bart/mux1.1352930400-2012-11-14-23.00.00_1352934000-2012-11-15-00.00.00_dvb1-2.ts");
        String filePid1 = "uuid:"+UUID.randomUUID().toString();

        URL fileURL2 = new URL(
                "http://bitfinder.statsbiblioteket.dk/bart/mux1.1352934000-2012-11-15-00.00.00_1352937600-2012-11-15-01.00.00_dvb1-2.ts");
        String filePid2 = "uuid:"+UUID.randomUUID().toString();

        String ritzauOrigID = "5444487";
        String tvMeterOrigID = "00011211142323021211150004242000310044002000410010001800310031003000400020090000Damages                                                     Damages                                                     Damages                                                        33FREM      010000000000000000000Stereo    16:9      172000000000000211636800          000001";
        String tvMeterOldID = tvMeterOrigID + "TvmeterProgram";

        String ritzauOldID = ritzauOrigID + "RitzauProgram";

        String programObjectCreationComment = Util.domsCommenter(filename, "creating Program Object");

        String programTitle = "Damages";



        String setObjectLabelComment = Util.domsCommenter(filename, "added program title '" + programTitle + "'object label");
        String updatedDatastreamComment = Util.domsCommenter(filename, "updated datastream");


        String pbCoreString = getPBCore(ritzauOldID, tvMeterOldID, programTitle);

        Date approxStart = origDateFormat.parse("2012-11-14 23:20:00.0");
        Date approxEnd = origDateFormat.parse("2012-11-15 00:00:00.0");
        Date preciseStart = origDateFormat.parse("2012-11-14 23:23:02.0");
        Date preciseEnd = origDateFormat.parse("2012-11-15 00:04:24.0");

        String ritzauOrig = getRitzau(ritzauOldID, approxStart, approxEnd, programTitle);
        String tvmeterOrig = getTvMeter(tvMeterOldID, preciseStart, preciseEnd);
        String programBroadcast = getProgramBroadcast(preciseStart, preciseEnd);

        String fileContents = getExportedObject(pbCoreString, fileURL1.toString(), fileURL2.toString(), ritzauOrig,
                                                tvmeterOrig, programBroadcast);


        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document metadataDocument = documentBuilder.parse(stream(fileContents), filename);

        DomsWSClient testDomsClient = mock(DomsWSClient.class);

        //Return the two filepids when searching for their URLs
        when(testDomsClient.getFileObjectPID(fileURL1)).thenReturn(filePid1);
        when(testDomsClient.getFileObjectPID(fileURL2)).thenReturn(filePid2);

        //Program Not found in doms already, so return empty lists
        when(testDomsClient.getPidFromOldIdentifier(ritzauOldID)).thenReturn(Arrays.asList());
        when(testDomsClient.getPidFromOldIdentifier(tvMeterOldID)).thenReturn(Arrays.asList());

        //Return the programPid when new object is made
        when(testDomsClient.createObjectFromTemplate(Common.PROGRAM_TEMPLATE_PID, Arrays.asList(ritzauOldID, tvMeterOldID),
                                                     programObjectCreationComment)).thenReturn(programPid);



        new RecordCreator(testDomsClient,true).ingestProgram(metadataDocument, filename);


        InOrder ordered = inOrder(testDomsClient);

        //First the file objects are found from their URLs
        ordered.verify(testDomsClient).getFileObjectPID(fileURL1);
        ordered.verify(testDomsClient).getFileObjectPID(fileURL2);

        //Then we search for the program pid from the ritzau/gallup identifiers
        ordered.verify(testDomsClient).getPidFromOldIdentifier(ritzauOldID);
        ordered.verify(testDomsClient).getPidFromOldIdentifier(tvMeterOldID);

        //It was not found, so we then create a new object
        ordered.verify(testDomsClient).createObjectFromTemplate(Common.PROGRAM_TEMPLATE_PID,Arrays.asList(ritzauOldID, tvMeterOldID), programObjectCreationComment);

        //And we set the label
        ordered.verify(testDomsClient).setObjectLabel(programPid, programTitle, setObjectLabelComment);

        //Then we add the four datastreamsw
        //TODO verify actual ds content...
        verify(testDomsClient).updateDataStream(eq(programPid), eq(Common.PROGRAM_PBCORE_DS_ID), any(Document.class), eq(updatedDatastreamComment));
        ordered.verify(testDomsClient).updateDataStream(eq(programPid), eq(Common.RITZAU_ORIGINAL_DS_ID), any(Document.class), eq(updatedDatastreamComment));
        ordered.verify(testDomsClient).updateDataStream(eq(programPid), eq(Common.GALLUP_ORIGINAL_DS_ID), any(Document.class), eq(updatedDatastreamComment));
        ordered.verify(testDomsClient).updateDataStream(eq(programPid), eq(Common.PROGRAM_BROADCAST_DS_ID), any(Document.class), eq(updatedDatastreamComment));

        //We check (unnessesarily) if our newly made object is already linked to the previously found file pids
        ordered.verify(testDomsClient).listObjectRelations(programPid, Common.HAS_FILE_RELATION_TYPE);

        //As it is not linked, add two relations
        ordered.verify(testDomsClient).addObjectRelation(programPid, Common.HAS_FILE_RELATION_TYPE, filePid1, Util.domsCommenter(filename, "added relation '" + Common.HAS_FILE_RELATION_TYPE + "' to '" + filePid1 + "'") );
        ordered.verify(testDomsClient).addObjectRelation(programPid, Common.HAS_FILE_RELATION_TYPE, filePid2, Util.domsCommenter(filename, "added relation '" + Common.HAS_FILE_RELATION_TYPE + "' to '" + filePid2 + "'") );

        //That's all, folks
        ordered.verifyNoMoreInteractions();
    }

    private ByteArrayInputStream stream(String pbCoreString) {
        return new ByteArrayInputStream(pbCoreString.getBytes());
    }


    public String getPBCore(String ritzauID, String tvmeterId, String title){
        return
                "        <ns2:PBCoreDescriptionDocument xmlns:ns2=\"http://www.pbcore.org/PBCore/PBCoreNamespace.html\">\n" +
        "            <ns2:pbcoreIdentifier>\n" +
        "                <ns2:identifier>"+ritzauID+"</ns2:identifier>\n" +
        "                <ns2:identifierSource>id</ns2:identifierSource>\n" +
        "            </ns2:pbcoreIdentifier>\n" +
        "            <ns2:pbcoreIdentifier>\n" +
        "                <ns2:identifier>"+tvmeterId+"</ns2:identifier>\n" +
        "                <ns2:identifierSource>tvmeter</ns2:identifierSource>\n" +
        "            </ns2:pbcoreIdentifier>\n" +
        "            <ns2:pbcoreTitle>\n" +
        "                <ns2:title>"+title+"</ns2:title>\n" +
        "                <ns2:titleType>titel</ns2:titleType>\n" +
        "            </ns2:pbcoreTitle>\n" +
        "            <ns2:pbcoreDescription>\n" +
        "                <ns2:description>Amerikansk dramaserie fra 2010.</ns2:description>\n" +
        "                <ns2:descriptionType>kortomtale</ns2:descriptionType>\n" +
        "            </ns2:pbcoreDescription>\n" +
        "            <ns2:pbcoreDescription>\n" +
        "                <ns2:description>Patty Hewes har mistanke om, at Tessa Marchetti bliver brugt til at smugle Louis Tobins penge ud af USA. Så Tom Shayes sendes til Antigua i Caribien for at efterforske sagen og lokke oplysninger ud af Tessa. Samtidig erfarer Joe Tobin for første gang, at hans far havde en datter uden for ægteskab.</ns2:description>\n" +
        "                <ns2:descriptionType>langomtale1</ns2:descriptionType>\n" +
        "            </ns2:pbcoreDescription>\n" +
        "            <ns2:pbcoreGenre>\n" +
        "                <ns2:genre>hovedgenre: Serier</ns2:genre>\n" +
        "            </ns2:pbcoreGenre>\n" +
        "            <ns2:pbcoreGenre>\n" +
        "                <ns2:genre>undergenre: Dramaserie</ns2:genre>\n" +
        "            </ns2:pbcoreGenre>\n" +
        "            <ns2:pbcoreGenre>\n" +
        "                <ns2:genre>indhold_emne: Fiktion</ns2:genre>\n" +
        "            </ns2:pbcoreGenre>\n" +
        "            <ns2:pbcoreContributor>\n" +
        "                <ns2:contributor>Tom Shayes: Tate Donovan</ns2:contributor>\n" +
        "                <ns2:contributorRole>medvirkende</ns2:contributorRole>\n" +
        "            </ns2:pbcoreContributor>\n" +
        "            <ns2:pbcoreContributor>\n" +
        "                <ns2:contributor>Ellen Parsons: Rose Byrne</ns2:contributor>\n" +
        "                <ns2:contributorRole>medvirkende</ns2:contributorRole>\n" +
        "            </ns2:pbcoreContributor>\n" +
        "            <ns2:pbcoreContributor>\n" +
        "                <ns2:contributor>Patty Hewes: Glenn Close</ns2:contributor>\n" +
        "                <ns2:contributorRole>medvirkende</ns2:contributorRole>\n" +
        "            </ns2:pbcoreContributor>\n" +
        "            <ns2:pbcoreContributor>\n" +
        "                <ns2:contributor>Tom Shayes: Tate Donovan</ns2:contributor>\n" +
        "                <ns2:contributorRole>instruktion</ns2:contributorRole>\n" +
        "            </ns2:pbcoreContributor>\n" +
        "            <ns2:pbcoreContributor>\n" +
        "                <ns2:contributor>Ellen Parsons: Rose Byrne</ns2:contributor>\n" +
        "                <ns2:contributorRole>instruktion</ns2:contributorRole>\n" +
        "            </ns2:pbcoreContributor>\n" +
        "            <ns2:pbcoreContributor>\n" +
        "                <ns2:contributor>Patty Hewes: Glenn Close</ns2:contributor>\n" +
        "                <ns2:contributorRole>instruktion</ns2:contributorRole>\n" +
        "            </ns2:pbcoreContributor>\n" +
        "            <ns2:pbcorePublisher>\n" +
        "                <ns2:publisher>dr1</ns2:publisher>\n" +
        "                <ns2:publisherRole>channel_name</ns2:publisherRole>\n" +
        "            </ns2:pbcorePublisher>\n" +
        "            <ns2:pbcorePublisher>\n" +
        "                <ns2:publisher>DR1</ns2:publisher>\n" +
        "                <ns2:publisherRole>kanalnavn</ns2:publisherRole>\n" +
        "            </ns2:pbcorePublisher>\n" +
        "            <ns2:pbcoreInstantiation>\n" +
        "                <ns2:dateCreated>2009</ns2:dateCreated>\n" +
        "                <ns2:formatLocation>Statsbiblioteket; Radio/TV-samlingen</ns2:formatLocation>\n" +
        "                <ns2:formatStandard>ikke hd</ns2:formatStandard>\n" +
        "                <ns2:formatDuration>2482000</ns2:formatDuration>\n" +
        "                <ns2:formatAspectRatio>16:9</ns2:formatAspectRatio>\n" +
        "                <ns2:formatColors>farve</ns2:formatColors>\n" +
        "                <ns2:formatChannelConfiguration>ikke surround</ns2:formatChannelConfiguration>\n" +
        "                <ns2:pbcoreDateAvailable>\n" +
        "                    <ns2:dateAvailableStart>2012-11-14T23:20:00+0100</ns2:dateAvailableStart>\n" +
        "                    <ns2:dateAvailableEnd>2012-11-15T00:00:00+0100</ns2:dateAvailableEnd>\n" +
        "                </ns2:pbcoreDateAvailable>\n" +
        "                <ns2:pbcoreFormatID>\n" +
        "                    <ns2:formatIdentifier>5444487RitzauProgram</ns2:formatIdentifier>\n" +
        "                    <ns2:formatIdentifierSource>id</ns2:formatIdentifierSource>\n" +
        "                </ns2:pbcoreFormatID>\n" +
        "            </ns2:pbcoreInstantiation>\n" +
        "            <ns2:pbcoreExtension>\n" +
        "                <ns2:extension>antalepisoder:39</ns2:extension>\n" +
        "            </ns2:pbcoreExtension>\n" +
        "            <ns2:pbcoreExtension>\n" +
        "                <ns2:extension>episodenr:33</ns2:extension>\n" +
        "            </ns2:pbcoreExtension>\n" +
        "            <ns2:pbcoreExtension>\n" +
        "                <ns2:extension>premiere:ikke premiere</ns2:extension>\n" +
        "            </ns2:pbcoreExtension>\n" +
        "            <ns2:pbcoreExtension>\n" +
        "                <ns2:extension>genudsendelse:ikke genudsendelse</ns2:extension>\n" +
        "            </ns2:pbcoreExtension>\n" +
        "            <ns2:pbcoreExtension>\n" +
        "                <ns2:extension>hovedgenre_id:4</ns2:extension>\n" +
        "            </ns2:pbcoreExtension>\n" +
        "            <ns2:pbcoreExtension>\n" +
        "                <ns2:extension>kanalid:3</ns2:extension>\n" +
        "            </ns2:pbcoreExtension>\n" +
        "            <ns2:pbcoreExtension>\n" +
        "                <ns2:extension>live:ikke live</ns2:extension>\n" +
        "            </ns2:pbcoreExtension>\n" +
        "            <ns2:pbcoreExtension>\n" +
        "                <ns2:extension>produktionsland_id:0</ns2:extension>\n" +
        "            </ns2:pbcoreExtension>\n" +
        "            <ns2:pbcoreExtension>\n" +
        "                <ns2:extension>program_id:29121458</ns2:extension>\n" +
        "            </ns2:pbcoreExtension>\n" +
        "            <ns2:pbcoreExtension>\n" +
        "                <ns2:extension>program_ophold:ikke program ophold</ns2:extension>\n" +
        "            </ns2:pbcoreExtension>\n" +
        "            <ns2:pbcoreExtension>\n" +
        "                <ns2:extension>undergenre_id:684</ns2:extension>\n" +
        "            </ns2:pbcoreExtension>\n" +
        "            <ns2:pbcoreExtension>\n" +
        "                <ns2:extension>afsnit_id:59202</ns2:extension>\n" +
        "            </ns2:pbcoreExtension>\n" +
        "            <ns2:pbcoreExtension>\n" +
        "                <ns2:extension>saeson_id:2382</ns2:extension>\n" +
        "            </ns2:pbcoreExtension>\n" +
        "            <ns2:pbcoreExtension>\n" +
        "                <ns2:extension>serie_id:1009</ns2:extension>\n" +
        "            </ns2:pbcoreExtension>\n" +
        "            <ns2:pbcoreExtension>\n" +
        "                <ns2:extension>tekstet:ikke tekstet</ns2:extension>\n" +
        "            </ns2:pbcoreExtension>\n" +
        "            <ns2:pbcoreExtension>\n" +
        "                <ns2:extension>th:ikke tekstet for hørehæmmede</ns2:extension>\n" +
        "            </ns2:pbcoreExtension>\n" +
        "            <ns2:pbcoreExtension>\n" +
        "                <ns2:extension>ttv:ikke tekst-tv</ns2:extension>\n" +
        "            </ns2:pbcoreExtension>\n" +
        "            <ns2:pbcoreExtension>\n" +
        "                <ns2:extension>showviewcode:355397</ns2:extension>\n" +
        "            </ns2:pbcoreExtension>\n" +
        "        </ns2:PBCoreDescriptionDocument>\n";
    }


    public String getExportedObject(String pbcore, String fileUrl1, String fileUrl2, String ritzauOrig, String tvmeterOrig, String programBroadcast){


        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
               "<program xmlns:ns2=\"http://www.pbcore.org/PBCore/PBCoreNamespace.html\" xmlns:ns3=\"http://doms.statsbiblioteket.dk/types/ritzau_original/0/1/#\" xmlns:ns4=\"http://doms.statsbiblioteket.dk/types/gallup_original/0/1/#\" xmlns:ns5=\"http://doms.statsbiblioteket.dk/types/program_broadcast/0/1/#\">\n" +
               "    <pbcore>\n" +
               pbcore +
               "    </pbcore>\n" +
               "    <originals>\n" +
               ritzauOrig +
               tvmeterOrig +
               "    </originals>\n" +
               programBroadcast +
               "    <fileUrls>\n" +
               "        <fileUrl>" + fileUrl1 + "</fileUrl>\n" +
               "        <fileUrl>" + fileUrl2 + "</fileUrl>\n" +
               "    </fileUrls>\n" +
               "</program>\n";
    }

    private String getProgramBroadcast(final Date startDate, final Date endDate) {
        SimpleDateFormat pbcdf = new SimpleDateFormat("yyyy-mm-dd'T'HH:MM:SS.sss+ZZZZ");
        return "    <ns5:programBroadcast>\n" +
               "        <ns5:timeStart>" + pbcdf.format(startDate) + "</ns5:timeStart>\n" +
               "        <ns5:timeStop>" + pbcdf.format(endDate) + "</ns5:timeStop>\n" +
                                      "        <ns5:channelId>dr1</ns5:channelId>\n" +
                                      "    </ns5:programBroadcast>\n";
    }

    private String getTvMeter(final String id, final Date startDate, final Date endDate) {
        return "        <ns4:tvmeterProgram>\n" +
               "            <ns4:originalEntry>" + id + "</ns4:originalEntry>\n" +
                                 "            <ns4:sourceFileName>de121114.std</ns4:sourceFileName>\n" +
                                 "            <ns4:logFormat>FORMAT_2</ns4:logFormat>\n" +
                                 "            <ns4:stationID>DR1</ns4:stationID>\n" +
               "            <ns4:startDate>" + origDateFormat.format(startDate) + "</ns4:startDate>\n" +
               "            <ns4:endDate>" + origDateFormat.format(endDate) + "</ns4:endDate>\n" +
                                 "            <ns4:parsedProgramClassification>\n" +
                                 "                <ns4:targetGroup>Voksne</ns4:targetGroup>\n" +
                                 "                <ns4:contentsItem>Fiktion</ns4:contentsItem>\n" +
                                 "                <ns4:form>Serie</ns4:form>\n" +
                                 "                <ns4:frequency>Serie</ns4:frequency>\n" +
                                 "                <ns4:origin>En el. flere TV-stationers egenproduktion</ns4:origin>\n" +
                                 "                <ns4:sendstatus>Førstegangsudsendelse</ns4:sendstatus>\n" +
                                 "                <ns4:productionDepartment>TV-INTERNATIONAL</ns4:productionDepartment>\n" +
                                 "                <ns4:itemCountry>USA</ns4:itemCountry>\n" +
                                 "                <ns4:productionCountry>USA</ns4:productionCountry>\n" +
                                 "                <ns4:intent>.. er at underholde</ns4:intent>\n" +
                                 "                <ns4:productionTimeAndPlace>Redigeret udsendelse udefra/ENG/EFP</ns4:productionTimeAndPlace>\n" +
                                 "                <ns4:targetGroupProductionYear>2009</ns4:targetGroupProductionYear>\n" +
                                 "                <ns4:targetGroupProposedPlacement>0000</ns4:targetGroupProposedPlacement>\n" +
                                 "            </ns4:parsedProgramClassification>\n" +
                                 "            <ns4:mainTitle>Damages</ns4:mainTitle>\n" +
                                 "            <ns4:subTitle>Damages</ns4:subTitle>\n" +
                                 "            <ns4:originalTitle>Damages</ns4:originalTitle>\n" +
                                 "            <ns4:episodeNumber>33</ns4:episodeNumber>\n" +
                                 "            <ns4:broadcastType>FREM</ns4:broadcastType>\n" +
                                 "            <ns4:overflowFlag>0</ns4:overflowFlag>\n" +
                                 "            <ns4:regionFlags>10000000000000000000</ns4:regionFlags>\n" +
                                 "            <ns4:expectedGRP>Stereo</ns4:expectedGRP>\n" +
                                 "            <ns4:additionDeductionOnPrice>16:9</ns4:additionDeductionOnPrice>\n" +
                                 "            <ns4:commonCode>17. (7b) Udenlandsk Fiktion.</ns4:commonCode>\n" +
                                 "            <ns4:price>0000000000</ns4:price>\n" +
                                 "            <ns4:internalIDCode>0211636800</ns4:internalIDCode>\n" +
                                 "            <ns4:bid>00000</ns4:bid>\n" +
                                 "            <ns4:emmisionsLevel>1</ns4:emmisionsLevel>\n" +
                                 "        </ns4:tvmeterProgram>\n";
    }

    private String getRitzau(final String id, final Date startDate, final Date endDate, final String title) {
        return "        <ns3:ritzau_original>RitzauProgram{Id=" + id +
               ", channel_name='dr1', kanalId=3, starttid=" + origDateFormat.format(startDate) +
               ", sluttid=" + origDateFormat.format(endDate) +
               ", annotation='null', originaltitel='null', kanalnavn='DR1', titel='" + title +
               "', hovedgenre='Serier', undergenre='Dramaserie', kortomtale='Amerikansk dramaserie fra 2010.', langomtale1='Patty Hewes har mistanke om, at Tessa Marchetti bliver brugt til at smugle Louis Tobins penge ud af USA. Så Tom Shayes sendes til Antigua i Caribien for at efterforske sagen og lokke oplysninger ud af Tessa. Samtidig erfarer Joe Tobin for første gang, at hans far havde en datter uden for ægteskab.', langomtale2='null', urllink='null', lydlink='null', episodetitel='null', instruktion='null', forfatter='null', produktionsland='null', medvirkende='Patty Hewes: Glenn Close, Ellen Parsons: Rose Byrne og Tom Shayes: Tate Donovan.', programlaengde=0, produktionsaar=2009, episodenr=33, antalepisoder=39, showviewcode=355397, surround=false, genudsendelse=false, ttv=false, sh=false, tekstet=false, sekstenni=true, bredformat=false, premiere=false, th=false, program_id=29121458, hovedgenre_id=4, undergenre_id=684, program_ophold=false, produktionsland_id=0, live=false, hd=false, afsnit_id=59202, saeson_id=2382, serie_id=1009}</ns3:ritzau_original>\n";
    }

}
