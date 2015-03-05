#!/bin/bash
#
# Manage what exported objects should go to domsingester hotfolder
# and which should go directly to the coldfolder
#

# Where are we?
BASEDIR=$(dirname $(dirname $(readlink -f $0)))

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
#### /Helper functions ####

# Exit values
# 0: objects moved, ingester started
# 1: export is running
# 2: export folder is empty
# 3: ingester is running and hotfolder is not empty
# 4: hotfolder is empty but we failed to stop the ingester
# 5: we found the ingester stopped and the hotfolder non-empty
main()
{
    local stopcounter=0 hotobjectcounter=0 coldobjectcounter=0 f
    if export_running; then
	echo "FATAL: exporter is running"
	exit 1
    else
	if [ $(export_queue_len) -eq 0 ]; then
	    echo "FATAL: Nothing to do, export folder is empty"
	    exit 2
	fi
    fi
    if ingest_running; then
	if [ $(ingest_queue_len) -gt 0 ]; then
	    echo "FATAL: ingester is running and hotfolder is not empty"
	    exit 3
	else
	    # queue is empty, shutdown ingester
	    $DOMSINGEST_INIT stop
	    while ingest_running
	    do
		if [ $stopcounter -gt 20 ]; then
		    # More than one minute has passed, we give up
		    echo "FATAL: could not stop ingester"
		    exit 4
		fi
		sleep 3
		stopcounter=$((stopcounter+1))
	    done
	fi
    else
	if [ $(ingest_queue_len) -gt 0 ]; then
	    echo "FATAL: ingester is stopped but hotfolder is not empty"
	    exit 5
	fi
    fi
    # Start moving objects
    for f in $(find . -name '*.xml')
    do
	if cmp $f $COLDFOLDER/$f 2>/dev/null; then
	    # Found matching cold file, update it and skip ingest
	    mv $f $COLDFOLDER
	    coldobjectcounter=$((coldobjectcounter+1))
	else
	    # No cold file or file did not match, ingest
	    mv $f $HOTFOLDER
	    hotobjectcounter=$((hotobjectcounter+1))
	fi
    done
    # Summary
    echo "Hot objects added:    $hotobjectcounter"
    echo "Cold objects updated: $coldobjectcounter"
    # Start the ingester
    $DOMSINGEST_INIT start
}

# Go to export folder
set -e
cd $EXPORTFOLDER
set +e

# Enter main program
main
