#!/bin/sh

# Supported command-line options are:
#
#	Client name to display at top of each webpage (Default="Default Client Name")
# -Dclient.name=<string>
#
# Port to serve from (Default="8080")
# -Dhttp.port=<port number>
#
# Relative path to directory containing webpages, images, etc. (Default="src/client/ajax/web")
# -Dweb.dir=<relative path>
#
# Comma separated list of brokers to automatically connect to on startup (Default="")
# -Ddefault.brokers=<IP address 1>,<IP address 2>,..
#
# Default start page (Default="status.html")
# -Ddefault.startpage=<page.html>
#
# Relative path to directory containing property file for RMIUniversalClient (Default="build/etc")
# -Dclientproperties.dir=<relative path>
#
# Filename for RMIUniversalClient property file (Default="client.properties")
# -Dclientproperties.file=<filename>

PADRES_ROOT=`pwd | sed -e 's-/[^/]*$--'`

CLIENT_CLASSES="$PADRES_ROOT/build/client"
PADRES_LIB="$PADRES_ROOT/build/padres-0.2.jar"
SIMPLE_LIB="$PADRES_ROOT/lib/simple-3.1.3.jar"
LOG4J_LIB="$PADRES_ROOT/lib/log4j-1.2.13.jar"

CLIENT_PROPS_DIR="build/etc/"
WEB_DIR="build/etc/web/client"

# Use build directory before JAR
cd $PADRES_ROOT
java -cp "$CLIENT_CLASSES:$PADRES_LIB:$LOG4J_LIB:$SIMPLE_LIB" \
	-Dweb.dir=$WEB_DIR \
	-Dclientproperties.dir=$CLIENT_PROPS_DIR \
	$* \
	ajax.client.WebUIClient
