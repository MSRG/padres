#!/bin/bash

#
# to start a stockquote subscriber
#

USAGE="Usage: $0 -r <msg_rate(msg/min)> -i <subscriber_id> -b <broker_uri> -h";

msgRate=6000;
bURI="rmi://localhost:1100/B0"
subID="sub"

while getopts r:i:b:h o
do
    case $o in
        r) msgRate=$OPTARG;;
	i) subID=$OPTARG;;
        b) bURI=$OPTARG;;
        h) echo $USAGE
            exit 0;;
        [?]) echo $USAGE
            exit 1;;
    esac
done
protocol=$(echo $bURI | cut -d: -f1)

PADRES_HOME="/home/master/bmmaran/work/localhost/padres-v1.5/"

# set the data direcotry; create if it does not exist
DATA_DIR=$PADRES_HOME/bin/benchmark/data
if [ ! -d $DATA_DIR ]
then
    echo "Creating data directory $DATA_DIR"
    mkdir $DATA_DIR;
fi
PROT_DIR=$DATA_DIR/$protocol
if [ ! -d $PROT_DIR ]
then
    echo "Creating protocol directory $PROT_DIR"
    mkdir $PROT_DIR;
fi

# start the subscriber
echo -n "Starting Subscriber $subID..."
$PADRES_HOME/demo/bin/stockquote/startSQsubscriber.sh -i $subID -b $bURI -s "[class,eq,'STOCK'],[symbol,eq,'STEM']" > $PROT_DIR/delays_runtopology1_$msgRate &
subPID=$!
echo "Done"

