#!/bin/sh
# If you get an error like ClassDefNotFound, then replace the ":" to ";" or vice versa in the classpath.
# Use it in cygwin only

PADRES_ROOT=`pwd | sed 's/\/cygdrive\/\(.\)/\/\1:/g' | sed -e 's-/[^/]*$--'`
echo java -cp "padres-0.2.jar;lib/jgraph.jar;lib/jgrapht-0.5.1.jar" -Djava.security.policy=etc/java.policy -Djava.rmi.server.codebase="file://$PADRES_ROOT/build/padres-0.2.jar" -Dpadres.build="$PADRES_ROOT/padres/build" monitor.MonitorFrame
java -cp "padres-0.2.jar;lib/log4j-1.2.13.jar;lib/jgraph.jar;lib/jgrapht-0.5.1.jar" -Djava.security.policy=etc/java.policy -Djava.rmi.server.codebase="file://$PADRES_ROOT/build/padres-0.2.jar" -Dpadres.build="$PADRES_ROOT/padres/build" monitor.MonitorFrame