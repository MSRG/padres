#!/bin/sh
PADRES_ROOT=`pwd | sed -e 's-/[^/]*$--'`

java -cp padres-0.2.jar:lib/jgraph.jar:lib/jgrapht-0.5.1.jar -Djava.security.policy=etc/java.policy -Djava.rmi.server.codebase=file://$PADRES_ROOT/build/padres-0.2.jar -Dpadres.build="$PADRES_ROOT/build" monitor.MonitorFrame
