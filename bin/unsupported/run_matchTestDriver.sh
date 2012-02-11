#!/bin/bash

PADRES_ROOT=`pwd | sed -e 's-/[^/]*$--'`
CLASSPATH=test:main:lib/jess.jar:lib/openjms-0.7.5.jar:lib/j2ee.jar:lib/mysql-connector.jar:lib/exolabcore-0.3.5.jar:lib/pg73jdbc3.jar
LD_LIBRARY_PATH=$LD_LIBRARY_PATH:./lib

MEMORY=1512m

export CLASSPATH
export LD_LIBRARY_PATH
export MEMORY

java -Xms$MEMORY -Xmx$MEMORY matching.MatchTestDriver ./test/matching/padresWorkloadAdvertisements.log ./test/matching/padresWorkloadSubscriptions.log ./test/matching/padresWorkloadPublications.log
