#!/bin/sh
#
# Basic Parameters
#
COLDFOLDER=$BASEDIR/coldfolder
LUKEFOLDER=$BASEDIR/lukewarm
HOTFOLDER=$BASEDIR/hotfolder
STOPFOLDER=$BASEDIR/stopfolder
WSDL=http://alhena:7880/centralWebservice-service/central/?wsdl
USERNAME=fedoraAdmin
PASSWORD=fedoraAdminPass
SCHEMA=$BASEDIR/resources/exportedRadioTVProgram.xsd

# Should the ingester overwrite existing objects?
OVERWRITE=false

# Ingester logs here
LOGFILE=$BASEDIR/logs/ingester.log
# Keep 8 numbered logs
NUMLOGS=7

#Number of threads to use when punishing DOMS
NUMTHREADS=5

# This setting is used only by ingest-object-mover, it should match the folder
# used when exporting objects for ingest
EXPORTFOLDER=$BASEDIR/files/export
