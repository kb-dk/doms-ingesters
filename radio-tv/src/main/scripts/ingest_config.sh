#!/bin/sh
#
# Basic Parameters
#
COLDFOLDER=$BASEDIR/files/coldfolder
LUKEFOLDER=$BASEDIR/files/lukewarm
HOTFOLDER=$BASEDIR/files/hotfolder
STOPFOLDER=$BASEDIR/files/stopfolder
WSDL=http://localhost:7980/centralWebservice-service/central/?wsdl
USERNAME=fedoraAdmin
PASSWORD=fedoraAdminPass
SCHEMA=$BASEDIR/resources/exportedRadioTVProgram.xsd

# Ingester logs here
LOGFILE=$BASEDIR/logs/ingester.log
# Keep 8 numbered logs
NUMLOGS=7

# This setting is used only by ingest-object-mover, it should match the folder
# used when exporting objects for ingest
EXPORTFOLDER=$BASEDIR/files/export
