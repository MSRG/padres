#! /bin/bash

#             B - S
#            /
#   S - B - B - P
#            \
#             B - S

if [ $# -lt 3 ]; then
    echo "Usage: $0 <protocol ('rmi'/'socket')> <msg_rate(msg/min)> <run_time (min)>";
    exit -1; 
fi
protocol=$1
rate=$2
exec_time=`expr $3 \* 60`

ADDITIONAL_PARAMS="-Xms256m -Xmx512m"
PATHSEP=':';

# Make sure environment variable $PADRES_HOME is set
if [ -z "$PADRES_HOME" ]
then
    PADRES_HOME="$(cd $(dirname "$0")/../.. && pwd)"
    export PADRES_HOME
fi

# adjust for cygwin
case "`uname`" in
  (CYGWIN*) 
  	PADRES_HOME="$(cygpath -m "$PADRES_HOME")"
  	PATHSEP='\\;'
  	;;
esac

CLASSPATH="${PADRES_HOME}/build/"
for LIB in `ls ${PADRES_HOME}/lib/*.jar`
do
	CLASSPATH="${CLASSPATH}${PATHSEP}${LIB}"
done

function st_brk() {
  local uri=$1
  shift;
  java $ADDITIONAL_PARAMS -cp $CLASSPATH \
  	-Djava.security.policy="${PADRES_HOME}/etc/java.policy" \
  	ca.utoronto.msrg.padres.broker.brokercore.BrokerCore -uri $uri $* &
  sleep 2;
}

function st_clnt() {
    local id=$1
    local bURI=$2
    shift;shift;
    java $ADDITIONAL_PARAMS -cp $CLASSPATH \
	-Djava.security.policy="${PADRES_HOME}/etc/java.policy" \
	ca.utoronto.msrg.padres.demo.stockquote.TimerStockSubscriber $id $bURI &
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
DATA_DIR=$PADRES_HOME/bin/benchmark/datat/
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
echo -n "Starting Broker 0...";
st_brk $protocol://localhost:1100/B0
echo "Done."
echo -n "Starting Broker 1...";
st_brk $protocol://localhost:1101/B1 -n $protocol://localhost:1100/B0
echo "Done."
echo -n "Starting Broker 2...";
st_brk $protocol://localhost:1102/B2 -n $protocol://localhost:1100/B0
echo "Done."
echo -n "Starting Broker 3...";
st_brk $protocol://localhost:1103/B3 -n $protocol://localhost:1100/B0
echo "Done."


# wait for 5sec for everything to start
WAIT_SEC=5;
echo -n "Waiting for $WAIT_SEC secs."
for i in `seq 1 $WAIT_SEC`; do
    echo -n ".$i."; 
    sleep 1; 
done; 
echo "";

# start the subscriber
echo -n "Starting subscriber 1..."
st_clnt sub1 $protocol://localhost:1101/B1 > $PROT_DIR/delays_runtopology3_c1_$rate
echo "Done"
echo -n "Starting subscriber 2..."
st_clnt sub2 $protocol://localhost:1102/B2 > $PROT_DIR/delays_runtopology3_c2_$rate
echo "Done"
echo -n "Starting subscriber 3..."
st_clnt sub3 $protocol://localhost:1103/B3 > $PROT_DIR/delays_runtopology3_c3_$rate
echo "Done"

# start the publisher
echo -n "Starting publisher now.."
java $ADDITIONAL_PARAMS -cp $CLASSPATH \
    -Djava.security.policy="${PADRES_HOME}/etc/java.policy" \
    ca.utoronto.msrg.padres.demo.stockquote.TimerStockPublisher \
    pub1 $protocol://localhost:1100/B0 $rate &
echo "Done"

# sleep for specific time and terminate
echo -n "Starting @ "; date
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

