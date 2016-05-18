#!/bin/bash

#
# Set up basic variables
#
SCRIPT_DIR=$(dirname $0)
pushd $SCRIPT_DIR > /dev/null
SCRIPT_DIR=$(pwd)
popd > /dev/null
BASEDIR=$SCRIPT_DIR/..

source $SCRIPT_DIR/ingest_config.sh

#
# Parse command line arguments.
# http://www.shelldorado.com/goodcoding/cmdargs.html
#
# ("don't use the getopt command if the arguments may contain whitespace
#  characters")
#

while getopts c:l:h:w:u:p:s:fc:fl:fh opt
do
    case "$opt" in
      c)  COLDFOLDER="$OPTARG";;
      l)  LUKEFOLDER="$OPTARG";;
      h)  HOTFOLDER="$OPTARG";;
      fc) FORCED_COLDFOLDER="$OPTARG";;
      fl) FORCED_LUKEFOLDER="$OPTARG";;
      fh) FORCED_HOTFOLDER="$OPTARG";;
      w)  WSDL="$OPTARG";;
      u)  USERNAME="$OPTARG";;
      p)  PASSWORD="$OPTARG";;
      s)  SCHEMA="$OPTARG";;
      \?)		# unknown flag
      	  echo >&2 \
	  "usage: $0 [-c coldfolder] [-l lukefolder] [-h hotfolder] [-fc forced_coldfolder] [-fl forced_lukefolder] \
	  [-fh forced_hotfolder] [-w wsdl] [-u username] [-p password] [-s preingestschema]"
	  exit 1;;
    esac
done
shift `expr $OPTIND - 1`

#Clear the stopfolder
mkdir -p "$STOPFOLDER"
rm -f "$STOPFOLDER/*"

java -cp .:$BASEDIR/lib/* dk.statsbiblioteket.doms.ingesters.radiotv.Ingester \
   -hotfolder=$HOTFOLDER -lukefolder=$LUKEFOLDER -coldfolder=$COLDFOLDER \
   -stopfolder=$STOPFOLDER -wsdl=$WSDL -username=$USERNAME -password=$PASSWORD \
   -preingestschema=$SCHEMA -overwrite=false &
pid_normal=$!

java -cp .:$BASEDIR/lib/* dk.statsbiblioteket.doms.ingesters.radiotv.Ingester \
   -hotfolder=$FORCED_HOTFOLDER -lukefolder=$FORCED_LUKEFOLDER -coldfolder=$FORCED_COLDFOLDER \
   -stopfolder=$STOPFOLDER -wsdl=$WSDL -username=$USERNAME -password=$PASSWORD \
   -preingestschema=$SCHEMA -overwrite=true  &
pid_forced=$!

wait $pid_normal
wait $pid_forced

