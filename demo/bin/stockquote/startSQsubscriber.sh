#!/bin/bash

# Make sure environment variable $PADRES_HOME is set
if [ -z "$PADRES_HOME" ]
then
    PADRES_HOME="$(cd $(dirname "$0")/../../.. && pwd)"
    export PADRES_HOME
fi

ARGS=$*
MEMORY_MIN=256  # default
MEMORY_MAX=512  # default
# ADDR=$(nslookup `hostname` | grep "Address: " | awk '{print $2}')   
# default
USAGE="Usage: $(basename $0) [-Xms <mem_min>] [-Xmx <mem_max>] [-hostname <hostname>] ..."

# Parse for JVM specific flag params and RMI port
CLIENT_OPT=()
while [ $# -gt 0 ]; do
    case "$1" in
	( -Xms )        
		MEMORY_MIN="$2"
		shift 2 ;;
	( -Xmx )
        MEMORY_MAX="$2"
		shift 2 ;;
	( -hostname )   
		ADDR="$2" 
		shift 2 ;;
	( -h )  
        echo $USAGE; 
		exit ;;
	( * )
		CLIENT_OPT=( ${CLIENT_OPT[@]} $1 )
		shift 1 ;;
    esac
done

java -Xms${MEMORY_MIN}m -Xmx${MEMORY_MAX}m \
	-cp ${PADRES_HOME}/build \
	-Djava.ext.dirs=${JAVA_HOME}/lib:${PADRES_HOME}/lib \
	-Djava.security.policy=${PADRES_HOME}/etc/java.policy \
	ca.utoronto.msrg.padres.demo.stockquote.StockSubscriber ${CLIENT_OPT[@]}
