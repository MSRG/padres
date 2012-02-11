#!/bin/bash

# check and set the PADRES_HOME environment value
if [ -z "$PADRES_HOME" ]
then
	PADRES_HOME="$(cd $(dirname "$0")/../../.. && pwd)"
fi

${PADRES_HOME}/bin/stopbroker B1
${PADRES_HOME}/bin/stopbroker B2
${PADRES_HOME}/bin/stopbroker B3
${PADRES_HOME}/bin/killall java
