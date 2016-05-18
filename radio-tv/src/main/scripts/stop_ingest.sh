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


touch $STOPFOLDER/stoprunning
echo "Stop signal sent"

anywait(){

    for pid in "$@"; do
        while kill -0 "$pid"; do
            sleep 0.5
        done
    done
}

ingesterPids=$(ps ww -C java -o pid,args | grep "\-stopfolder=$STOPFOLDER" | cut -c 1-5)

echo "This process $$ will now wait for the exit of pids $ingesterPids"
anywait $ingesterPids
