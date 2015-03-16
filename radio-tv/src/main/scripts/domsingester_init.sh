#!/bin/bash

# Where are we?
BASEDIR=$(dirname $(dirname $(readlink -f $0)))

cd $BASEDIR/bin
source ingest_config.sh

# Keep 8 numbered logs by default
NUMLOGS=7
logfile=$BASEDIR/logs/ingester.log

USEFILE=$(ls -1 $BASEDIR/lib/domsclient*.jar 2>&1 | tail -1)
if [ -n "$USEFILE" ]; then
    pid=$(/usr/sbin/lsof -t $USEFILE)
else
    echo "FATAL: Could not find domsclient*.jar"
    exit 1
fi

rotate_log()
{
    for x in $(seq $NUMLOGS -1 1)
    do
        [ -r ${logfile}.$x ] && mv ${logfile}.$x ${logfile}.$((x+1))
    done
    [ -r ${logfile} ] && mv ${logfile} ${logfile}.1
}

start()
{
    rotate_log
    [ -r $STOPFOLDER/stoprunning ] && rm -f $STOPFOLDER/stoprunning && echo "Removing stopfile $STOPFOLDER/stoprunning"
    echo "Starting ingester"
    ./ingest.sh > $logfile 2>&1 <&- &
}

stop()
{
    input=$(ls $HOTFOLDER 2>/dev/null|wc -l)
    stoplimit=20

    if [ -n "$pid" ]; then
        if [ "$1" = "kill" ]; then
            echo "Killing ingester"
            [ $input -gt 0 ] && echo "DANGER! Ingest is in progress, cleanup may be necessary!"
            kill $pid
        else
            if [ $input -eq 0 ]; then
                # Queue is empty, just kill it
                echo "Stopping ingester (inputfolder is empty)"
                kill $pid
                exit 0
            fi
            if [ $input -lt $stoplimit ]; then
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
