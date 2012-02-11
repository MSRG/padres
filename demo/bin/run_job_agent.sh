#!/bin/bash

if [ -z "$PADRES_HOME" ]
then
	PADRES_HOME="$(cd $(dirname "$0")/../.. && pwd)"
fi

java -Xms128m -Xmx256m -cp ${PADRES_HOME}/build/ \
     -Djava.ext.dirs=${JAVA_HOME}/jre/lib/ext:${PADRES_HOME}/lib \
     -Djava.security.policy=${PADRES_HOME}/etc/java.policy \
     ca.utoronto.msrg.padres.demo.workflow.agent.AgentRMIClient $1 $2 $3

     
