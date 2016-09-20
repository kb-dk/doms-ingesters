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

# This setting is used only by ingest-object-mover, it should match the folder
# used when exporting objects for ingest
EXPORTFOLDER=$BASEDIR/files/export

# Use 4 threads, and poll status (and for stop folder) every 1000 ms
THREADS=4
WAIT=1000