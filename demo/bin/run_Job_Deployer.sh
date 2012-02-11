#!/bin/bash

if [ -z "$PADRES_HOME" ]
then
	PADRES_HOME="$(cd $(dirname "$0")/../.. && pwd)"
fi

PATHSEP=':'

# adjust for cygwin
case "`uname`" in
  (CYGWIN*) 
  	PADRES_HOME="$(cygpath -m "$PADRES_HOME")"
  	PATHSEP='\\;'
  	;;
esac
   
java -Xms128m -Xmx256m -cp ${PADRES_HOME}/build/ \
     -Djava.ext.dirs=${JAVA_HOME}/jre/lib/ext${PATHSEP}${PADRES_HOME}/lib \
     -Djava.security.policy=${PADRES_HOME}/etc/java.policy \
     ca.utoronto.msrg.padres.tools.guiclient.SwingRMIDeployer $1 $2
     