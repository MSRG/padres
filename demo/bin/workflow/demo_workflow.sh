#!/bin/bash

# check and set the PADRES_HOME environment value
if [ -z "$PADRES_HOME" ]
then
	PADRES_HOME="$(cd $(dirname "$0")/../../.. && pwd)"
fi

ETC_DIR=${PADRES_HOME}/demo/etc/fixedcycles

echo "Starting Broker B1"
${PADRES_HOME}/bin/startbroker -i B1 -p 1100 &
sleep 12 
echo "Starting Broker B2"
${PADRES_HOME}/bin/startbroker -i B2 -p 1101 -n localhost:1100 &
sleep 12
echo "Starting Broker B3"
${PADRES_HOME}/bin/startbroker -i B3 -p 1102 -n localhost:1100  &
sleep 12

echo "Starting Agent A"
${PADRES_HOME}/demo/bin/run_job_agent.sh localhost 1100 agentA &
sleep 4
echo "Starting Agent B"
${PADRES_HOME}/demo/bin/run_job_agent.sh localhost 1101 agentB &
sleep 4
echo "Starting Agent C"
${PADRES_HOME}/demo/bin/run_job_agent.sh localhost 1101 agentC &
sleep 4
echo "Starting Agent D"
${PADRES_HOME}/demo/bin/run_job_agent.sh localhost 1102 agentD &
sleep 4
echo "Starting Agent E"
${PADRES_HOME}/demo/bin/run_job_agent.sh localhost 1102 agentE &
sleep 4

echo "Starting Job Deployer"
${PADRES_HOME}/demo/bin/run_Job_Deployer.sh localhost 1100 &
 
sleep 4
echo "Starting PADRES Monitor"
${PADRES_HOME}/bin/startmonitor &



