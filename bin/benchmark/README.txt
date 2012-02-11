The bash scripts in this directory are to run tests to profile the
performance of PADRES.

Publishers send messages at a set rate. We vary these rates in order
to see the maximum rates that can be accommodated by various systems.

There are two sets of testing scripts. 

The script autorunt.sh and the scripts in the form runtopology?t.sh
use TimerStockPublisher.sh, and TimerStockSubscriber.sh as the
publishers and subscribers. These files use a Timer for generating
publications at a fixed rate. They also work for both RMI and socket
protocols.

The script autorun.sh and other scripts in the form of runtopology?.sh
use StockPublisher.sh, and StockSubscriber.sh as the publishers and
subscribers.  These files use Thread.sleep() for generating
publications at a given rate (precise). These set of tests are overall
inferior. However, they are included for legacy.

If you are running a *t.sh file, the data will be saved in
/bin/benchmark/datat

If you are running any other *.sh file, the data will be saved in
/bin/benchmark/data

All data files are in the following format:

(Publication Number)  (Timestamp at generation [ms unixtime])	(Timestamp on receipt [ms unixtime])	(Difference between generation time and reception time (ms))

The naming of the files is in the following format:

delays_runtopology(1 or 3)_(rate in [pub/min])

--------------
FILES
--------------

- runtopology1.sh
USAGE:
./runtopology1.sh <'rmi' or 'socket'> <rate (pub/min)> <run time (in min.)> 

DESCRIPTION: 
Uses StockPublisher as the Publisher, and StockBroker as the
Subscriber.  Runs the following topology with the given publication
rate:
   
   P - B - S

It uses the communication layer with the given ('rmi' or 'socket')
protocol and runs the experiment for the given ('run time') amount of
time.

- runtopology3.sh
USAGE:
./runtopology3.sh <'rmi' or 'socket'> <rate (pub/min)> <run time (in min.)> 

DESCRIPTION: 
Uses StockPublisher as the Publisher, and StockBroker as the
Subscriber.  Runs the following topology with the given publication
rate:
   
       B - S
     /
   P - B - S
     \
       B - S
      
It uses the communication layer with the given ('rmi' or 'socket')
protocol and runs the experiment for the given ('run time') amount of
time.

- autorun.sh
USAGE:
./autorun.sh

DESCRIPTION:
Runs runtopology1.sh and runtopology3.sh for various rates (which must
be changed within the shell file itself) using RMI protocol, using
runtopology1.sh and runtopology3.sh


- runtopology1t.sh
USAGE:
./runtopology1.sh [rate (pub/min)] [protocol ("socket" or "rmi")]

DESCRIPTION: 
Uses TimerStockPublisher as the Publisher, and TimerStockSubscriber as the Subscriber.
Works for socket or RMI. Runs the following topology for a constant rate:
   
   P - B - S

      
- autorunt.sh
USAGE:
./autorun.sh [protocol ("socket" or "rmi")

DESCRIPTION:
Runs runtopology1t.sh for various rates (which must
be changed within the shell file itself) for either RMI or socket protocols.


--------------
How to profile
--------------

1. To profile, choose which autorun script to use. "autorunt.sh" or "autorun.sh"
2. Make sure the appropriate data folders exist: /bin/benchmark/datat or /bin/benchmark/data , respectively
3. Edit the autorun script to test for the desired rates.
4. Run the autorun script. i.e.
./autorunt.sh socket
5. Once the tests are complete, run the test checking script i.e.
./checkrun.sh datat
This will generate deliv.rate and gener.rate files in the datat folder. These files contain the amount of generated
messages and the amount of delivered messages for each run.
6. Look through each of these files. The maximum rate that can be accomodated by the system
is the maximum rate for which the deliv.rate and gener.rate files contain the correct generation 
and delivery rates for each second of the test.


--------------
General Details
--------------
For runtopology1.sh, runtopology1t.sh, runtopology3.sh
- The subscribers send out a single subscription (2 predicates), at the start of the test. This subscription will be a match with each publication.
- Publications are 8 predicates long. (105 characters plus or minus a couple of characters)
