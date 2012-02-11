#!/bin/bash

# check and set the PADRES_HOME environment value
if [ -z "$PADRES_HOME" ]
then
	PADRES_HOME="$(cd $(dirname "$0")/../../.. && pwd)"
fi

ETC_DIR=${PADRES_HOME}/demo/etc/fixedcycles

echo "Starting Broker A"
${PADRES_HOME}/bin/startbroker -uri rmi://localhost:1099/BrokerA  -c ${ETC_DIR}/brokerA_fixedcycles.properties
echo "Starting Broker B"
${PADRES_HOME}/bin/startbroker -uri rmi://localhost:1098/BrokerB -n rmi://localhost:1099/BrokerA -c ${ETC_DIR}/brokerB_fixedcycles.properties
echo "Starting Broker C"
${PADRES_HOME}/bin/startbroker -uri rmi://localhost:1097/BrokerC -n rmi://localhost:1099/BrokerA -c ${ETC_DIR}/brokerC_fixedcycles.properties
echo "Starting Broker D"
${PADRES_HOME}/bin/startbroker -uri rmi://localhost:1096/BrokerD -n rmi://localhost:1098/BrokerB,rmi://localhost:1097/BrokerC -c ${ETC_DIR}/brokerD_fixedcycles.properties

echo "Starting Client 1"
${PADRES_HOME}/demo/bin/run_Job_Deployer.sh ClientA rmi://localhost:1099/BrokerA &
 sleep 4
echo "Starting Client 2"
${PADRES_HOME}/demo/bin/run_Job_Deployer.sh ClientB rmi://localhost:1099/BrokerA &
sleep 4
echo "Starting Client 3"
${PADRES_HOME}/demo/bin/run_Job_Deployer.sh ClientC rmi://localhost:1096/BrokerD &

sleep 4
echo "Starting PADRES Monitor"
${PADRES_HOME}/bin/startmonitor &



