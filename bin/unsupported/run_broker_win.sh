#!/bin/sh
# If you get an error like ClassDefNotFound, then replace the ":" to ";" or vice versa in the classpath.
# Use it in cygwin only

PADRES_ROOT=`pwd | sed 's/\/cygdrive\/\(.\)/\/\1:/g' | sed -e 's-/[^/]*$--'`
echo java -Djava.class.path="main;lib/jess.jar;lib/log4j-1.2.13.jar;lib/openjms-0.7.5.jar;lib/j2ee.jar;lib/mysql-connector.jar;lib/exolabcore-0.3.5.jar;lib/pg73jdbc3.jar" -Djava.security.policy=etc/java.policy -Djava.rmi.server.codebase="file://$PADRES_ROOT/build/padres-0.2.jar  file://$PADRES_ROOT/build/lib/openjms-0.7.5.jar file://$PADRES_ROOT/build/lib/j2ee.jar file://$PADRES_ROOT/build/lib/pg73jdbc3.jar file://$PADRES_ROOT/build/lib/log4j-1.2.13.jar" -Dpadres.build="$PADRES_ROOT/build" brokercore.BrokerCore $*
java -Djava.class.path="main;lib/jess.jar;lib/log4j-1.2.13.jar;lib/openjms-0.7.5.jar;lib/j2ee.jar;lib/mysql-connector.jar;lib/exolabcore-0.3.5.jar;lib/pg73jdbc3.jar" -Djava.security.policy=etc/java.policy -Djava.rmi.server.codebase="file://$PADRES_ROOT/build/padres-0.2.jar  file://$PADRES_ROOT/build/lib/openjms-0.7.5.jar file://$PADRES_ROOT/build/lib/j2ee.jar file://$PADRES_ROOT/build/lib/pg73jdbc3.jar file://$PADRES_ROOT/build/lib/log4j-1.2.13.jar" -Dpadres.build="$PADRES_ROOT/build" brokercore.BrokerCore $*


