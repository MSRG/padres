#!/bin/sh

# check and set the PADRES_HOME environment value
if [ -z "$PADRES_HOME" ]
then
	PADRES_HOME="$(cd $(dirname "$0")/.. && pwd)"
	export PADRES_HOME
fi

PATHSEP=':'

# adjust for cygwin
case "`uname`" in
  (CYGWIN*) 
  	PADRES_HOME="$(cygpath -m "$PADRES_HOME")"
  	PATHSEP='\\;'
  	;;
esac

java -cp ${PADRES_HOME}/build/ -Djava.ext.dirs=${JAVA_HOME}/jre/lib/ext${PATHSEP}${PADRES_HOME}/lib \ 
	 ca.utoronto.msrg.padres.tools.panda.TopologyGenerator $*
