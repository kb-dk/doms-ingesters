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

