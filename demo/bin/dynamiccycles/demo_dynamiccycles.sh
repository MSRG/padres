#!/bin/bash

# check and set the PADRES_HOME environment value
if [ -z "$PADRES_HOME" ]
then
	PADRES_HOME="$(cd $(dirname "$0")/../../.. && pwd)"
fi

ETC_DIR=${PADRES_HOME}/demo/etc/dynamiccycles

echo "Starting Broker A"
${PADRES_HOME}/bin/startbroker  -uri rmi://localhost:1099/BrokerA A -c ${ETC_DIR}/brokerA_dynamiccycles.properties
echo "Starting Broker B"
${PADRES_HOME}/bin/startbroker  -uri rmi://localhost:1098/BrokerB -n rmi://localhost:1099/BrokerA -c ${ETC_DIR}/brokerB_dynamiccycles.properties
echo "Starting Broker C"
${PADRES_HOME}/bin/startbroker  -uri rmi://localhost:1097/BrokerC -n rmi://localhost:1099/BrokerA -c ${ETC_DIR}/brokerC_dynamiccycles.properties
echo "Starting Broker D"
${PADRES_HOME}/bin/startbroker  -uri rmi://localhost:1096/BrokerD -n rmi://localhost:1098/BrokerB,rmi://localhost:1097/BrokerC -c ${ETC_DIR}/brokerD_dynamiccycles.properties
sleep 12
echo "Starting Client A"
${PADRES_HOME}/demo/bin/run_Job_Deployer.sh ClientA rmi://localhost:1099/BrokerA &
sleep 4
echo "Starting Client B"
${PADRES_HOME}/demo/bin/run_Job_Deployer.sh ClientB rmi://localhost:1096/BrokerD &
sleep 4
echo "Starting the Monitor"
${PADRES_HOME}/bin/startmonitor
