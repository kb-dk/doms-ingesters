#!/bin/sh
#
# Basic Parameters
#
COLDFOLDER=$BASEDIR/coldfolder
LUKEFOLDER=$BASEDIR/lukewarm
HOTFOLDER=$BASEDIR/hotfolder
STOPFOLDER=$BASEDIR/stopfolder
WSDL=http://alhena:7980/centralWebservice-service/central/?wsdl
USERNAME=fedoraAdmin
PASSWORD=fedoraAdminPass
SCHEMA=$BASEDIR/config/exportedRadioTVProgram.xsd

# Should the ingester overwrite existing objects?
OVERWRITE=false

# Should the ingester attempt to do semantic matching against existing objects in DOMS?
VERIFY=false

# The maximum number of errors tolerated before the ingester stops
MAXFAILS=10

# This setting is used only by ingest-object-mover, it should match the folder
# used when exporting objects for ingest
EXPORTFOLDER=$BASEDIR/files/export

# Use 4 threads, and poll status (and for stop folder) every 1000 ms
THREADS=4
WAIT=1000