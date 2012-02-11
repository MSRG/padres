#!/bin/sh

USAGE="Usage: $(basename $0) [-h] [-d padres_dir]"

INSTALL_DIR="/usr/local/"
DOWNLOAD_DIR="$(cd $(dirname $0) && pwd)"

# process the command line options
while [ $# -gt 0 ]; do
	case "$1" in
		( -h )
			echo $USAGE
			exit 0 ;;
		( -d )
			INSTALL_DIR="$2"
			shift 2 ;;
		( * )
			echo "invalid option $1"
			echo $USAGE
			exit 1 ;;
    esac
done

# remove the '/' at the end, if any
INSTALL_DIR=`echo $INSTALL_DIR | sed s:/$::`

# create a 'padres' directory inside the given directory
INSTALL_DIR="${INSTALL_DIR}/padres"
# create the installation directory if it does not exist
# exit, if permission denied
if [ ! -d $INSTALL_DIR ]; then
	echo "creating the instation directory <$INSTALL_DIR>..."
	mkdir $INSTALL_DIR
	# check the exit status of the mkdir; exit, if mkdir is failed
	if [ $? != "0" ]; then
	    exit 1
	fi
fi

# install binaries, scripts, and config files
# basically copy them to the INSTALL_DIR
echo "copying files to the installation directory..."
cp -r $DOWNLOAD_DIR/build $INSTALL_DIR
cp -r $DOWNLOAD_DIR/lib $INSTALL_DIR
cp -r $DOWNLOAD_DIR/bin $INSTALL_DIR
cp -r $DOWNLOAD_DIR/demo $INSTALL_DIR
cp -r $DOWNLOAD_DIR/etc $INSTALL_DIR

#give execution permissions to the scripts
chmod 755 $INSTALL_DIR/bin/*.sh

echo "PADRES installation complete"
echo "\nPlease set/update environment variables using following commands:"
echo ">>export PADRES_HOME=\"$INSTALL_DIR\""
echo ">>export PATH=\"\$PATH:\$PADRES_HOME/bin\""
exit 0

