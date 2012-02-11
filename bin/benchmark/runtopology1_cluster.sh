#! /bin/bash

#
# Distributed operation in MSRG cluster
#   P - B - S
#
# Notes:
# - It has to be launched from master.msrg.utoronto.ca 
# - Previous to that, PADRES distribution has to be distributed to all
#   the required machines' ~/work/localhost/
#

USAGE="Usage: $0 -r <msg_rate(msg/min)> -t <run_time> -b <broker_uri> -p <publisher_ip> -s <subscriber_ip>";

rate=6000;
execTime=1
bURI="rmi://localhost:1100/B0"
pubAddr="localhost"
subAddr="localhost"

while getopts r:t:b:p:s:h o
do
    case $o in
        r) rate=$OPTARG;;
        t) execTime=$OPTARG;;
        b) bURI=$OPTARG;;
        p) pubAddr=$OPTARG;;
        s) subAddr=$OPTARG;;
        h) echo $USAGE
            exit 0;;
        [?]) echo $USAGE
            exit 1;;
    esac
done
exec_time=`expr $execTime \* 60`
bAddr=$(echo $bURI | cut -d: -f2 | cut -d/ -f3)
bID=$(echo $bURI | cut -d/ -f4)

PADRES_HOME="/home/master/bmmaran/work/localhost/padres-v1.5/"
DATA_DIR=$PADRES_HOME/bin/benchmark/data

# start the broker
echo -n "Starting broker $bID @$bAddr..."
ssh $bAddr "$PADRES_HOME/bin/startbroker -uri $bURI > $DATA_DIR/broker_log_top1_$rate" &
echo "Done"

# wait for 5sec for everything to start 
WAIT_SEC=5;
echo -n "Waiting for $WAIT_SEC secs "
for i in `seq 1 $WAIT_SEC`; 
do
    echo -n ".$i."; 
    sleep 1; 
done; 
echo "";

# star the client and dump the output in the data directory
echo "Starting subscriber @$subAddr..."
ssh -f $subAddr "$PADRES_HOME/bin/benchmark/startSQsubscriber_cluster.sh -r $rate -i sub -b $bURI"

# start the publisher
echo "Starting publisher @$pubAddr..."
ssh -f $pubAddr "$PADRES_HOME/bin/benchmark/startSQpublisher_cluster.sh -r $rate -i pub -b $bURI"

# sleep for specific time and terminate
echo -n "Starting @ "
date
echo "Running for $exec_time Seconds"
sleep $exec_time

# kill the processes
echo "Killing Publisher @$pubAddr... "
ssh $pubAddr $PADRES_HOME/bin/benchmark/killall_stockquote.sh
sleep 3
echo "Killing Subscriber @$subAddr... "
ssh $subAddr $PADRES_HOME/bin/benchmark/killall_stockquote.sh
sleep 3
echo -n "Killing broker B0"
ssh $bAddr $PADRES_HOME/bin/stopbroker $bID
echo "DONE"
