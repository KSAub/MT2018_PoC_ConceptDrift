#!/bin/bash
#Taken from https://stackoverflow.com/questions/192249/how-do-i-parse-command-line-arguments-in-bash

pushd . > /dev/null
cd "$(dirname "$0")"

case $1 in
    extract)
    java -classpath "./target/*" -Xmx4G -XX:+UseParallelGC -XX:-UseGCOverheadLimit -DentityExpansionLimit=0 -DtotalEntitySizeLimit=0 -Djdk.xml.totalEntitySizeLimit=0 net.auberson.scherer.masterthesis.Extract
    ;;
    *)    # unknown command
    echo "USAGE:                                                                         "
    echo "- Extract Dataset: $0 extract"
    echo "    Expects the unpacked StackOverflow archive in ./data/raw (i.e. a "
    echo "    subdirectory named 'stackoverflow' containing a number of 7z files). "
    echo "    Generates many dataset CSVs in ./data/intermediate (one CSV per class,"
    echo "    and a CSV containing the dataset sizes)."
    ;;
esac

popd > /dev/null