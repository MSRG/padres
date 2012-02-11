rmiregistry 1099 &
rmiregistry 1098 &
rmiregistry 1097 &
rmiregistry 1096 &
rmiregistry 1095 &
sleep 4
../bin/run_broker_win.sh ../demo/brokerA.properties > brokerA.log &
sleep 12 
../bin/run_broker_win.sh ../demo/brokerB.properties > brokerB.log &
sleep 12
../bin/run_broker_win.sh ../demo/brokerC.properties > brokerC.log &
sleep 12
../bin/run_broker_win.sh ../demo/brokerD.properties > brokerD.log &
sleep 12
../bin/run_broker_win.sh ../demo/brokerE.properties > brokerE.log &
sleep 12
../bin/run_agent_win.sh localhost 1099 agentA > agentA.log &
sleep 4
../bin/run_agent_win.sh localhost 1098 agentB > agentB.log &
sleep 4
../bin/run_agent_win.sh localhost 1097 agentC > agentC.log &
sleep 4
../bin/run_agent_win.sh localhost 1096 agentD > agentD.log &
sleep 4
../bin/run_agent_win.sh localhost 1095 agentE > agentE.log &
sleep 4
../bin/run_Job_Deployer.sh localhost 1099 > deployer.log &
sleep 4
../bin/run_monitor_win.sh  > monitor.log
