#!/bin/sh
#
# Basic Parameters
#
# Normal queue
COLDFOLDER=$BASEDIR/coldfolder
LUKEFOLDER=$BASEDIR/lukewarm
HOTFOLDER=$BASEDIR/hotfolder

# Overwrite queue
COLDFOLDER=$BASEDIR/forced/coldfolder
LUKEFOLDER=$BASEDIR/forced/lukewarm
HOTFOLDER=$BASEDIR/forced/hotfolder


STOPFOLDER=$BASEDIR/stopfolder
WSDL=http://alhena:7880/centralWebservice-service/central/?wsdl
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
