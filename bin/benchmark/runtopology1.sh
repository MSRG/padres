#! /bin/bash

#
#   P - B - S
#

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
  shift; shift;
  echo -n "Starting broker $uri...";
  $PADRES_HOME/bin/startbroker $ADDITIONAL_PARAMS -uri $uri $*
  echo "Done."
}

function st_clnt() {
    $PADRES_HOME/demo/bin/stockquote/startSQsubscriber.sh $ADDITIONAL_PARAMS -i sub -b $1 -s "[class,eq,'STOCK'],[symbol,eq,'STEM']" &
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

# start the broker
st_brk $protocol://localhost:1100/B0 > $PROT_DIR/broker_log_top1_$rate

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
echo -n "Starting subscriber..."
st_clnt $protocol://localhost:1100/B0 > $PROT_DIR/delays_runtopology1_$rate
echo "Done"

# start the publisher
echo -n "Starting publisher..."
$PADRES_HOME/demo/bin/stockquote/startSQpublisher.sh $ADDITIONAL_PARAMS -i pub -b $protocol://localhost:1100/B0 -s STEM -r $rate > $PROT_DIR/pub_log_top1_$rate &
echo "Done"

# sleep for specific time and terminate
echo -n "Starting @ "
date
echo "Running for $exec_time Seconds"
sleep $exec_time

# kill the brokers and clients
killalljava
sleep 3
echo "Killing broker B0"
$PADRES_HOME/bin/stopbroker B0
