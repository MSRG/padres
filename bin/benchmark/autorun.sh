#!/bin/bash

MIN_RUN=10
if [ $# -lt 3 ]; then
    echo "Missing arguments:"
    echo -e "\tUsage: $0 <topology> <protocol> <run_time_per_test>"
    echo -e "\tAvailable protocols: 'rmi' or 'socket'"
    exit 1
fi
topology=$1
protocol=$2
run_time=$3

if [ -z "$PADRES_HOME" ]
then
    PADRES_HOME="$(cd $(dirname "$0")/../.. && pwd)"
    export PADRES_HOME
fi

function runTop1() {
    min_rate="$1"
    echo "Running top 1 with $min_rate";
    $PADRES_HOME/bin/benchmark/runtopology1.sh $protocol $min_rate $run_time
}

function runTop3() {
    min_rate="$1"
    echo "Running top 3 with $min_rate";
    $PADRES_HOME/bin/benchmark/runtopology3.sh $protocol $min_rate $run_time
}

for sec_rate in 250 500 750 1000; do
    min_rate=`expr $sec_rate \* 60`;
    if [ $topology == '1' ]; then
	runTop1 $min_rate
    else
	runTop3 $min_rate
    fi
    sleep 5
done
