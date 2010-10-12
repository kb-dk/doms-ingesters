#!/bin/sh

echo "Do this to use the ingester"
echo "java -cp .:lib/* dk.statsbiblioteket.doms.ingesters.radiotv.Ingester"
echo "The preingest files are read from /tmp/radioTVMetadata"
echo "on failure, the file is moved to /tmp/failedFiles"
echo "on succes, the file is moved to /tmp/processedFiles"