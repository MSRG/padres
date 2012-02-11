#! /bin/bash

#
# Script to kill all the running scripts from .../stockquote/ directory
#

# kill all the stockquote clients (subscriber and publisher)
for pid in `ps aux | grep java | awk '/stockquote/{printf $2" "}'`; 
do
    echo "Killing process ID $pid"
    kill -9 $pid; 
done

