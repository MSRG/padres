PADRES v2.1
===========

PADRES is a distributed content-based publish/subscribe system 
middleware. This middleware can be used to create a pub/sub broker 
overlay to route messages between publishers and subscribers. The
following components included in this package are:

1. broker: to create the distributed routing backbone.
2. client: to connect to a broker and acts as a publisher or subscriber. 
3. monitor: to monitor the deployed PADRES system.


Files and Directories:
---------------------
1. README: this file.
2. INSTALL: installation instructions.
2. ISSUES: open issues.
3. COPYRIGHT: copyright notice.
4. install.sh: script to install PADRES in the system.
5. bin/: scripts to run PADRES components. 
6. build/: the PADRES component binaries. 
7. etc/: PADRES configuration files.
8. demo/: various demo scripts instructions.
9. doc/: Generated Javadocs.


Installation:
-------------
Check the instructions in the INSTALL file.


Development setup:
------------------
- Install Java 8
- Install [Maven](http://maven.apache.org/install.html)
- Run script: install-maven-dependencies.sh
- Open maven project in IDE


Documentation
-------------
The PADRES user guide is available at
http://www.msrg.org/projects/padres/docs/user_guide

The PADRES developer guide is available at 
http://www.msrg.org/projects/padres/docs/dev_guide


--------------------------------------------------------------------
Middleware Systems Research  (msrg.org)
University of Toronto
2012-02-11
