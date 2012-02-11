rmiregistry 1099 &
rmiregistry 1098 &
rmiregistry 1097 &
rmiregistry 1096 &
rmiregistry 1095 &
sleep 4
../bin/run_broker_win.sh ../demo/brokerA_cycles.properties > brokerA.log &
sleep 12 
../bin/run_broker_win.sh ../demo/brokerB_cycles.properties > brokerB.log &
sleep 12
../bin/run_broker_win.sh ../demo/brokerC_cycles.properties > brokerC.log &
sleep 12
../bin/run_broker_win.sh ../demo/brokerD_cycles.properties > brokerD.log &
sleep 12
../bin/run_broker_win.sh ../demo/brokerE_cycles.properties > brokerE.log &
sleep 12
../bin/run_monitor_win.sh  > monitor.log
