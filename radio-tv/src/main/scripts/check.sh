#!/bin/bash
#
# Check if ingester is running and optionally start it again if the
# queue is not empty
#

# Start if stopped?
autostart=1

# Where are we?
BASEDIR=$(dirname $(dirname $(readlink -f $0)))

# Setup and helpers
source $BASEDIR/bin/domsingester-lib.sh

# Main
status=$($DOMSINGEST_INIT status)
case $status in
    Running) exit 0;;
    Stopped)
	if objectmover_running; then
	    exit 0
	else
	    if [ $autostart -eq 1 -a $(ingest_queue_len) -gt 0 ]; then
		$DOMSINGEST_INIT start > /dev/null 2>&1
	    else
		exit 0
	    fi
	fi
	;;
esac
