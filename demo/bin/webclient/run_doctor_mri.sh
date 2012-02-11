#!/bin/bash

## run it from PADRES project root directory
./bin/startbroker -i Broker1
./bin/startwebclient -i Doctor -port 8080 -ipage hospital_demo.html &
./bin/startwebclient -i MRI -port 8081 -ipage hospital_demo.html &

echo "Enter to quit the demo"
read input

./bin/stopbroker Broker1
killall java
