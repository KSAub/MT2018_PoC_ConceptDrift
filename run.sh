#!/bin/bash
#Taken from https://stackoverflow.com/questions/192249/how-do-i-parse-command-line-arguments-in-bash

pushd . > /dev/null
cd "$(dirname "$0")"

case $1 in
	
    extract)
    java -classpath "./target/*" -Xmx4G -XX:+UseParallelGC -XX:-UseGCOverheadLimit -DentityExpansionLimit=0 -DtotalEntitySizeLimit=0 -Djdk.xml.totalEntitySizeLimit=0 net.auberson.scherer.masterthesis.Extract
    ;;
    
    learningcurve)
    java -classpath "./target/*" net.auberson.scherer.masterthesis.ComputeLearningCurve
    ;;
    
    experiment1)
    java -classpath "./target/*" net.auberson.scherer.masterthesis.Experiment1
    ;;
    
    
    *)    # unknown command
    echo "USAGE:                                                                         "
    echo "- Extract Dataset: $0 extract"
    echo "    Expects the unpacked StackOverflow archive in ./data/raw (i.e. a "
    echo "    subdirectory named 'stackoverflow' containing a number of 7z files). "
    echo "    Generates many dataset CSVs in ./data/intermediate (one CSV per class,"
    echo "    and a CSV containing the dataset sizes)."
    echo "- Compute Learning Curve: $0 learningcurve <categories>"
    echo "    E.g. $0 learningcurve electronics gaming security travel cooking"
    echo "    Trains a classifier for the given categories (the corresponding CSV in "
    echo "    ./data/intermediate must exist) with varying training set sizes, and "
    echo "    outputs the Accuracy for each size to a CSV in ./reports/learning-curve"     
    echo "- Run first experiment: $0 experiment1 <categories>"
    echo "    E.g. $0 learningcurve experiment1 gaming security travel cooking"
    ;;
esac

popd > /dev/null