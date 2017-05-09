#Hvordan vi tester den...


## Deploy on digiv-devel

```bash
version=1.9-SNAPSHOT
scp target/radio-tv-$version-ingester.tar.gz  digitv@digitv-devel:.

#Deploy the ingester
ssh digitv@digitv-devel <<EOS
    rm -rf ingester radio-tv-$version
    tar -xvzf radio-tv-$version-ingester.tar.gz
    ln -s radio-tv-$version ingester
    cd ingester
    mkdir hotfolder coldfolder lukewarm stopfolder
    echo -e "\nVERIFY=true" >> config/ingest_config.sh
EOS

#Test files
scp develro@digitv-stage:/ingest/lukewarm/*.xml digitv@digitv-devel:~/ingester/hotfolder/

```


Vi har vel nogen export filer i stage (dvs. input filer til ingesteren).

    TGC, v1.8, 2/5/2017:
    Jeg har et fuldt sæt af export filer fra min test af parallel export.
    
    Dette er de valgte 3 test filer:
    -rw-rw-r--. 1 digitv digitv 8669 Apr  7 09:48 2017-03-16_22-00-00_dr3.xml
    -rw-rw-r--. 1 digitv digitv 6376 Apr  7 11:33 2017-03-16_22-00-00_tv2d.xml
    -rw-rw-r--. 1 digitv digitv 6643 Apr  7 09:46 2017-03-16_22-30-00_dr2.xml 

Hvis du kun har export filer der allerede er blevet ingestet, kan vi slette dem igen i stage doms. Det gøres med en curl kommando ala

    curl -X DELETE -u fedoraAdmin:fedoraAdminPass "http://stage-doms:7880/fedora/objects/$UUID"
hvor $UUID er uuid'en på posten


I step 2 til 5, må test filerne ikke forekomme i coldfolder.


## 1. Almindelig ingest
Since v1.8

Lav 1+ export fra digitv, af objekter som ikke pt. er i stage doms.

Brug ingesteren til at ingeste disse.

    TGC, v1.8, 2/5/2017:
    PASS
    
### 1.1 Test match af input objekter mod coldfolder binært identitiske
Since v1.8 (courtesy of TGC)

Tag filerne fra step 1. og kopier dem tilbage til hotfolder.
Ingesteren skulle gerne se at filerne er identiske med dem i coldfolder og ikke forsøge ingest.

    TGC, v1.8, 2/5/2017:
    PASS

### 1.2 Test match af input objekter mod coldfolder semantisk identiske
Since v1.9

1. Tag filerne fra step 1.
2. Kør dem gennem xmllint, eller lignende procedure, så de ikke længere har samme checksum. Det kan også klares ved at tilføje ekstra linieskift til slut i filen.
3. Kopier dem tilbage til hotfolder.

Ingesteren skulle gerne se at filerne er identiske med dem i coldfolder og ikke forsøge ingest.


## 2. Test af fejl, stop

Tag 2+ exports som allerede er blevet ingested i stage doms
1. Sæt MAXFAILS til 1.
2. Sæt OVERWRITE til False
3. Sæt Check til False
4. Sæt Threads til 1 (bare for at den tager objekterne sekventielt i stedet for samtidigt)

Prøv at ingeste de 2+ filer. Den bør fejle efter det første objekt, og stoppe fordi den kun accepterer 1 fejl

    TGC, v1.8, 2/5/2017:
    To filer efterladt i hotfolder og en i lukewarm.
    PASS 


## 3. Test af fejl, fortsæt

Tag 2+ exports som allerede er blevet ingested i stage doms

1. Sæt MAXFAILS til 0.
2. Sæt OVERWRITE til False
3. Sæt Check til False

Prøv at ingeste de 2+ filer. Den vil fejle i at ingeste objekterne, men den bør IKKE stoppe, da MAXFAILS 0 betyder at den accepterer ubegrænset mange fejl.

    TGC, v1.8, 2/5/2017:
    De 3 filer ender i lukewarm, hotfolder er tom, og ingesteren er ikke stoppet.

    PASS 

Check at du kan stoppe den med de almindelige stop scripts (dvs. at MAXFAILS 0 ikke forhindrer normal stop)

    PASS 



## 4. Test af Overwrite

Tag 2+ exports som allerede er blevet ingested i stage doms

1. Sæt OVERWRITE til True
2. Sæt Check til False

Prøv at ingeste de 2+ filer. Da overwrite er true, vil den overskrive posterne i doms. Check log filen for at være sikker på at den hver overskrevet posterne.

    TGC, v1.8, 2/5/2017:
    PASS 


## 5. Test af Check

Tag 2+ exports som allerede er blevet ingested i stage doms

1. Sæt OVERWRITE til False
2. Sæt Check til True

Prøv at ingeste de 2+ filer. Da objekterne allerede er ingestet (se forrige test) bør den rapportere at success, uden at have overskrevet posterne.

Det kan ses fra logfilerne om den har forsøgt at overskrive posterne eller bare accepteret at de er ens.

    TGC, v1.8, 2/5/2017:
    FAIL
 
    Den kan ikke se at objekterne er ens og forsøger ingest, alle 3 filer ender i lukewarm.
