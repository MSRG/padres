#!/bin/bash

# check and set the PADRES_HOME environment value
if [ -z "$PADRES_HOME" ]
then
	PADRES_HOME="$(cd $(dirname "$0")/../../.. && pwd)"
fi

${PADRES_HOME}/bin/stopbroker A
${PADRES_HOME}/bin/stopbroker B
${PADRES_HOME}/bin/stopbroker C
${PADRES_HOME}/bin/stopbroker D
