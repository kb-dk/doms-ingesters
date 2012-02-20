#!/bin/sh
#
# Parameters for injecting technical metadata via ffprobe
#
DOMSWSDL=http://alhena:7880/centralWebservice-service/central/?wsdl
DOMSUSER=fedoraAdmin
DOMSPASSWORD=fedoraAdminPass

BARTURLPREFIX=http://bitfinder.statsbiblioteket.dk/bart/

FFPROBEOPTIONS=ffprobe -show_format -show_streams -print_format xml -i file:
