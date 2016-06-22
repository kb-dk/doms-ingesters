#!/bin/bash

# Where are we?
BASEDIR=$(dirname $(dirname $(readlink -f $0)))

# Setup and helpers
source $BASEDIR/bin/domsingester-lib.sh

USEFILE=$(ls -1 $BASEDIR/lib/domsClient*.jar 2>&1 | tail -1)
if [ -n "$USEFILE" ]; then
    pid=$(/usr/sbin/lsof -t $USEFILE)
else
    echo "FATAL: Could not find domsclient*.jar"
    exit 1
fi

start()
{
    if [ -z "$ignore_object_mover" ]; then
        if objectmover_running; then
            echo "FATAL: Cannot start, ingest-object-mover is running"
            exit 1
        fi
    fi
    rotate_log
    [ -r $STOPFOLDER/stoprunning ] && rm -f $STOPFOLDER/stoprunning && echo "Removing stopfile $STOPFOLDER/stoprunning"
    echo "Starting ingester"
    cd $BASEDIR/bin
    ./ingest.sh > $LOGFILE 2>&1 <&- &
}

stop()
{
    stoplimit=20
    if [ -n "$pid" ]; then
        if [ "$1" = "kill" ]; then
            echo "Killing ingester"
            [ $(ingest_queue_len) -gt 0 ] && echo "DANGER! Ingest is in progress, cleanup may be necessary!"
            kill $pid
        else
            if [ $(ingest_queue_len) -eq 0 ]; then
                # Queue is empty, just kill it
                echo "Stopping ingester (inputfolder is empty)"
                kill $pid
                exit 0
            fi
            if [ $(ingest_queue_len) -lt $stoplimit ]; then
                echo "Ingester is working but there are too few remaining items for the stopfile to work (requires atleast $stoplimit)"
                echo "To stop it in this state you must use kill. But please wait until there are no more items to ingest to"
                echo "avoid corrupting the input state of the ingester."
            else
                echo "Dropping stopfile in $STOPFOLDER. Ingester will stop within the next $stoplimit items ingested"
                touch $STOPFOLDER/stoprunning
            fi
        fi
    else
        echo "Ingester is not running"
    fi
}

status()
{
    if [ "$pid" ]; then
        echo "Running ($pid)"
    else
        echo "Stopped"
    fi
}

case $1 in
    start)
        start
        ;;
    forcestart)
        # We want ingest-object-mover to be able to start the ingester
        # but of course then we need to skip checking if it is running
        ignore_object_mover=1
        start
        ;;
    stop)
        stop
        ;;
    kill)
        stop kill
        ;;
    status)
        status
        ;;
    *)
        echo "Usage: domsingester { start | stop | kill | status }"
        ;;
esac

# vim: set sts=4 sw=4 et ft=sh : #
