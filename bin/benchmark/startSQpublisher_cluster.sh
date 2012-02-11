#!/bin/bash

#
# to start a stockquote publisher
#

USAGE="Usage: $0 -r <msg_rate(msg/min)> -i <publisher_id> -b <broker_uri> -h";

msgRate=6000;
bURI="rmi://localhost:1100/B0"
pubID="pub"

while getopts r:i:b:h o
do
    case $o in
        r) msgRate=$OPTARG;;
	i) pubID=$OPTARG;;
        b) bURI=$OPTARG;;
        h) echo $USAGE
            exit 0;;
        [?]) echo $USAGE
            exit 1;;
    esac
done

PADRES_HOME="/home/master/bmmaran/work/localhost/padres-v1.5/"
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
echo -n "Starting Publisher $pubID..."
$PADRES_HOME/demo/bin/stockquote/startSQpublisher.sh -i $pubID -b $bURI -s "STEM" -r $msgRate > $PROT_DIR/pub_log_top1_$msgRate &
pubPID=$!
echo "Done"
