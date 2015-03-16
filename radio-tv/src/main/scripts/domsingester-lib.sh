# function library for domsingester scripts
#
# The $BASEDIR variable *must* be defined before sourcing this file
# It should be defined as:
# BASEDIR=$(dirname $(dirname $(readlink -f $0)))
#
# This file should then be in $BASEDIR/bin/

# Init script for the ingester
DOMSINGEST_INIT=$BASEDIR/bin/domsingester_init.sh

# Get folder configuration
source $BASEDIR/bin/ingest_config.sh

#### Helper functions ####
# Return number of items currently waiting in the hotfolder
ingest_queue_len()
{
    echo $(ls -U $HOTFOLDER | wc -l)
}

# Return number of items currently waiting in the export folder
export_queue_len()
{
    echo $(ls -U $EXPORTFOLDER | wc -l)
}

# Is the ingester running? returns 0 (true) or 1 (false)
ingest_running()
{
    local status
    status=$($DOMSINGEST_INIT status)
    case $status in
	Running) return 0;;
	Stopped) return 1;;
    esac
}

# Is the exporter running? returns 0 (true) or 1 (false)
export_running()
{
    local pid
    pid=$(pgrep -u fedora -f "/home/fedora/digitv/conf/template.xml $HOTFOLDER")
    if [ -n "$pid" ]; then
	return 0
    else
	return 1
    fi
}

# Is the object mover running? returns 0 (true) or 1 (false)
objectmover_running()
{
  local pid
  pid=$(pgrep -u fedora -f "$BASEDIR/bin/ingest-object-mover.sh")
  if [ -n "$pid" ]; then
    return 0
  else
    return 1
  fi
}

# Rotate $LOGFILE, keep $NUMLOGS generations
rotate_log()
{
    local x
    for x in $(seq $NUMLOGS -1 1)
    do
        [ -r ${LOGFILE}.$x ] && mv ${LOGFILE}.$x ${LOGFILE}.$((x+1))
    done
    [ -r ${LOGFILE} ] && mv ${LOGFILE} ${LOGFILE}.1
}
#### /Helper functions ####
