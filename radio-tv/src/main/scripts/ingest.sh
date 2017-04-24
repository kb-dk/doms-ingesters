#!/bin/bash

#
# Set up basic variables
#
SCRIPT_DIR=$(dirname $0)
pushd $SCRIPT_DIR > /dev/null
SCRIPT_DIR=$(pwd)
popd > /dev/null
BASEDIR=$SCRIPT_DIR/..


# Default config param
COLDFOLDER=$BASEDIR/coldfolder
LUKEFOLDER=$BASEDIR/lukewarm
HOTFOLDER=$BASEDIR/hotfolder
STOPFOLDER=$BASEDIR/stopfolder
WSDL=http://alhena:7980/centralWebservice-service/central/?wsdl
USERNAME=fedoraAdmin
PASSWORD=fedoraAdminPass
SCHEMA=$BASEDIR/config/exportedRadioTVProgram.xsd
OVERWRITE=false
VERIFY=false
MAXFAILS=10
EXPORTFOLDER=$BASEDIR/files/export
THREADS=4
WAIT=1000


# Override the config params from ingest_config.sh
source $BASEDIR/config/ingest_config.sh


# Override the config params again from the command line
#
# Parse command line arguments.
# http://www.shelldorado.com/goodcoding/cmdargs.html
#
# ("don't use the getopt command if the arguments may contain whitespace
#  characters")
#
while getopts c:l:h:w:u:p:s:o:n:t:f:v: opt
do
    case "$opt" in
      c)  COLDFOLDER="$OPTARG";;
      l)  LUKEFOLDER="$OPTARG";;
      h)  HOTFOLDER="$OPTARG";;
      w)  WSDL="$OPTARG";;
      u)  USERNAME="$OPTARG";;
      p)  PASSWORD="$OPTARG";;
      s)  SCHEMA="$OPTARG";;
      o)  OVERWRITE="$OPTARG";;
      n)  THREADS="$OPTARG";;
      t)  WAIT="$OPTARG";;
      f)  MAXFAILS="$OPTARG";;
      v)  VERIFY="$OPTARG";;
      \?)		# unknown flag
      	  echo >&2 \
	  "usage: $0 [-c coldfolder] [-l lukefolder] [-h hotfolder] [-w wsdl] \
	  [-u username] [-p password] [-s preingestschema] [-o true|false] \
	  [-n numThreads] [-t threadPollInterval] [-f maxFails] [-v true|false]"
	  exit 1;;
    esac
done
shift `expr $OPTIND - 1`

java -Dlogback.configurationFile=$BASEDIR/config/logback.xml \
     -cp .:$BASEDIR/config/*:$BASEDIR/lib/* \
   dk.statsbiblioteket.doms.ingesters.radiotv.Ingester \
    -hotfolder=$HOTFOLDER \
    -lukefolder=$LUKEFOLDER \
    -coldfolder=$COLDFOLDER \
    -stopfolder=$STOPFOLDER \
    -wsdl=$WSDL \
    -username=$USERNAME \
    -password=$PASSWORD \
    -preingestschema=$SCHEMA \
    -overwrite=$OVERWRITE \
    -numthreads=$THREADS \
    -threadwaittime=$WAIT \
    -maxFails=$MAXFAILS \
    -check=$VERIFY
