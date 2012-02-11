#! /bin/bash

#         B - S
#        /
#   P - B - B - S
#        \
#         B - S


ADDITIONAL_PARAMS="-Xms 128 -Xmx 512"
   
if [ $# -lt 3 ]; then
    echo "Usage: $0 <protocol> <msg_rate(msg/min)> <run_time>";
    exit -1; 
fi
protocol=$1
rate="$2";
exec_time=`expr $3 \* 60`

# Make sure environment variable $PADRES_HOME is set
if [ -z "$PADRES_HOME" ]
then
    PADRES_HOME="$(cd $(dirname "$0")/../.. && pwd)"
    export PADRES_HOME
fi

function st_brk() {
  local uri=$1
  shift;
  $PADRES_HOME/bin/startbroker $ADDITIONAL_PARAMS -uri $uri $*
  sleep 2;
}

function st_clnt() {
    local id=$1
    local bURI=$2
    $PADRES_HOME/demo/bin/stockquote/startSQsubscriber.sh $ADDITIONAL_PARAMS -i $id -b $bURI -s "[class,eq,'STOCK'],[symbol,eq,'STEM']" &
}

function killalljava() {
    # kill all the stockquote clients (subscriber and publisher)
    for pid in `ps aux | grep java | awk '/stockquote/{printf $2" "}'`; 
    do
	echo "Killing process ID $pid"
	kill -9 $pid; 
    done
}

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

# starting brokers
echo -n "Starting broker B0...";
st_brk $protocol://localhost:1100/B0 > $PROT_DIR/broker_log_top3_B0_$rate
echo "Done."
echo -n "Starting broker B1...";
st_brk $protocol://localhost:1101/B1 -n $protocol://localhost:1100/B0 > $PROT_DIR/broker_log_top3_B1_$rate
echo "Done."
echo -n "Starting broker B2...";
st_brk $protocol://localhost:1102/B2 -n $protocol://localhost:1100/B0 > $PROT_DIR/broker_log_top3_B2_$rate
echo "Done."
echo -n "Starting broker B3...";
st_brk $protocol://localhost:1103/B3 -n $protocol://localhost:1100/B0 > $PROT_DIR/broker_log_top3_B3_$rate
echo "Done."

# waiting for brokers to start
WAIT_SEC=5;
echo -n "Waiting for $WAIT_SEC secs"
for i in `seq 1 $WAIT_SEC`; 
do
    echo -n ".$i."; 
    sleep 1; 
done; 
echo "";

# starting subscribers
echo -n "Starting subscriber1..."
st_clnt sub1 $protocol://localhost:1101/B1 > $PROT_DIR/delays_runtopology3_c1_$rate
echo "Done"
echo -n "Starting subscriber2..."
st_clnt sub2 $protocol://localhost:1102/B2 > $PROT_DIR/delays_runtopology3_c2_$rate
echo "Done"
echo -n "Starting subscriber3..."
st_clnt sub3 $protocol://localhost:1103/B3 > $PROT_DIR/delays_runtopology3_c3_$rate
echo "Done"

echo -n "Starting publisher..."
$PADRES_HOME/demo/bin/stockquote/startSQpublisher.sh -i pub -b $protocol://localhost:1100/B0 -s STEM -r $rate > $PROT_DIR/pub_log_top3_$rate &
echo "Done"

# sleep for specific time and terminate
echo -n "Starting @ "
date
echo "Running for $exec_time Seconds"
sleep $exec_time
# kill brokers
echo "Killing broker B0"
$PADRES_HOME/bin/stopbroker B0
echo "Killing broker B1"
$PADRES_HOME/bin/stopbroker B1
echo "Killing broker B2"
$PADRES_HOME/bin/stopbroker B2
echo "Killing broker B3"
$PADRES_HOME/bin/stopbroker B3
killalljava
