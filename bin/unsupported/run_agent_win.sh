#!/bin/sh
PADRES_ROOT=`pwd | sed 's/\/cygdrive\/\(.\)/\/\1:/g' | sed -e 's-/[^/]*$--'`



#PADRES_ROOT=`pwd`
#CLASSPATH="main:padres-0.2.jar:lib/jess.jar"
echo java -Djava.class.path="padres-0.2.jar;lib/log4j-1.2.13.jar;lib/jess.jar;lib/j2ee.jar;lib/openjms-0.7.5.jar" -Djava.security.policy=etc/java.policy -Djava.rmi.server.codebase=file://$PADRES_ROOT/build/padres-0.2.jar -Dpadres.build="$PADRES_ROOT/build" agent.AgentRMIClient $1 $2 $3
java -Djava.class.path="padres-0.2.jar;lib/log4j-1.2.13.jar;lib/jess.jar;lib/j2ee.jar;lib/openjms-0.7.5.jar" -Djava.security.policy=etc/java.policy -Djava.rmi.server.codebase=file://$PADRES_ROOT/build/padres-0.2.jar -Dpadres.build="$PADRES_ROOT/build" agent.AgentRMIClient $1 $2 $3
