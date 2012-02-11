#!/bin/sh

# Supported command-line options are:
#
# Client name to display at top of each webpage (Default="Default Client Name")
# -Dclient.name=<string>
#
# Port to serve from (Default="8080")
# -Dhttp.port=<port number>
#
# Relative path to directory containing webpages, images, etc. (Default="etc/web/client")
# -Dweb.dir=<relative path>
#
# Comma separated list of brokers to automatically connect to on startup (Default="")
# -Ddefault.brokers=<IP address 1>,<IP address 2>,..
#
# Default start page (Default="status.html")
# -Ddefault.startpage=<page.html>
#
# Path of property file for RMIUniversalClient (Default="etc/client.properties")
# Path is relative to PADRES_HOME
# -Dclient.properties=<file path>

# Use PADRES_HOME directory
cd $PADRES_HOME
java -cp $PADRES_HOME/build/client \
	 -Djava.ext.dirs=${JAVA_HOME}/lib:${PADRES_HOME}/lib \
	 $* \
	 ajax.client.WebUIClient
