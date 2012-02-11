#!/bin/sh
for host in Administrator@10.0.0.97 Administrator@10.0.0.97 Administrator@10.0.0.97 Administrator@10.0.0.98 Administrator@10.0.0.98 Administrator@10.0.0.97 Administrator@10.0.0.97 Administrator@10.0.0.97 Administrator@10.0.0.97 Administrator@10.0.0.98 Administrator@10.0.0.98 Administrator@10.0.0.98
do
ssh $host 'killall java; killall rmiregistry'
done

