#!/bin/bash
#Taken from https://stackoverflow.com/questions/192249/how-do-i-parse-command-line-arguments-in-bash

pushd . > /dev/null
cd "$(dirname "$0")"

case $1 in

	nightly)
	  $0 experiment2 electronics gaming security travel cooking pets
    $0 experiment1 electronics gaming security travel cooking
    ;;

    extract)
    java -classpath "./target/MasterThesisKSA.jar" -Xmx4G -XX:+UseParallelGC -XX:-UseGCOverheadLimit -DentityExpansionLimit=0 -DtotalEntitySizeLimit=0 -Djdk.xml.totalEntitySizeLimit=0 net.auberson.scherer.masterthesis.Extract
    ;;

    learningcurve)
    shift
    java -classpath "./target/MasterThesisKSA.jar" net.auberson.scherer.masterthesis.ComputeLearningCurve $@
    ;;

    experiment1)
    shift
    java -classpath "./target/MasterThesisKSA.jar" net.auberson.scherer.masterthesis.Experiment1 $@
    ;;

    experiment2)
    shift
    java -classpath "./target/MasterThesisKSA.jar" net.auberson.scherer.masterthesis.Experiment2 $@
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
    echo "    E.g. $0 experiment1 electronics gaming security travel cooking"
    echo "    Runs the first experiment, storing the data files in "
    echo "    ./data/experiment1 and the reports in ./reports/experiment1 "
    echo "- Run second experiment: $0 experiment2 <categories>"
    echo "    E.g. $0 experiment2 electronics gaming security travel cooking pets"
    echo "    Runs the second experiment, where the last class will not be used for
    echo "    initial training, storing the data files in ./data/experiment2 and the
    echo "    reports in ./reports/experiment2 "
    ;;
esac

popd > /dev/null
