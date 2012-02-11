#!/bin/sh

VERSION_NO_FILE="version.txt"
USAGE="Usage: $(basename $0) [-h] [-v version_no]"

# get the current version number from the file
VERSION_NO=`cat ${VERSION_NO_FILE}`

# process the command line options
while [ $# -gt 0 ]; do
	case "$1" in
		( -h )
			echo $USAGE
			exit 0 ;;
		( -v )
			VERSION_NO="$2"
			shift 2 ;;
		( * )
			echo "invalid option $1"
			echo $USAGE
			exit 1 ;;
    esac
done

# write the version number to the file
echo $VERSION_NO > $VERSION_NO_FILE

# replace version number and date in the README and INSTALL files
DATE=`date +%d-%m-%Y`
sed -r -e s/[0-9]+-[0-9]+-[0-9]+/$DATE/ \
       -e s/v\([0-9]+\.\)+[0-9]+/v$VERSION_NO/ < README > tmp.tmp
mv tmp.tmp README
sed -r -e s/[0-9]+-[0-9]+-[0-9]+/$DATE/ \
       -e s/v\([0-9]+\.\)+[0-9]+/v$VERSION_NO/ < INSTALL > tmp.tmp
mv tmp.tmp INSTALL

# create a package directory and copy files there
PACKAGE_DIR="padres-v${VERSION_NO}"
mkdir $PACKAGE_DIR

# function to remove all the CVS-created sub directories
rem_cvs_dir()
{
CVS_DIR="${1}/*"
for i in 1 2 3 4 5
do
    rm -rf ${CVS_DIR}/CVS
    CVS_DIR="${CVS_DIR}/*"
done
}

# prepare a package directory
echo "preparing for packaging..."
cp -r INSTALL README LICENSE install*.sh bin/ build/ demo/ etc/ lib/ $PACKAGE_DIR/
# exclude CVS directories
find $PACKAGE_DIR -name "CVS" > exclude_src.tmp

## produce tar files
# just the binary release
echo "producing padres-v${VERSION_NO}.tgz..."
cat exclude_src.tmp > exclude.tmp
ls "${PACKAGE_DIR}/install_lib.sh" >> exclude.tmp
ls "${PACKAGE_DIR}/install_all.sh" >> exclude.tmp
ls -d "${PACKAGE_DIR}/lib" >> exclude.tmp
tar czfX "padres-v${VERSION_NO}.tgz" exclude.tmp $PACKAGE_DIR
# just the library release
echo "producing padres-lib-v${VERSION_NO}.tgz..."
cat exclude_src.tmp > exclude.tmp
ls "${PACKAGE_DIR}/install.sh" >> exclude.tmp
ls "${PACKAGE_DIR}/install_all.sh" >> exclude.tmp
ls -d "${PACKAGE_DIR}/bin" >> exclude.tmp
ls -d "${PACKAGE_DIR}/build" >> exclude.tmp
ls -d "${PACKAGE_DIR}/etc" >> exclude.tmp
tar czfX "padres-lib-v${VERSION_NO}.tgz" exclude.tmp $PACKAGE_DIR
# bin + lib release
echo "producing padres-all-v${VERSION_NO}.tgz..."
cat exclude_src.tmp > exclude.tmp
ls "${PACKAGE_DIR}/install_lib.sh" >> exclude.tmp
ls "${PACKAGE_DIR}/install.sh" >> exclude.tmp
tar czfX "padres-all-v${VERSION_NO}.tgz" exclude.tmp $PACKAGE_DIR

# clean the package directory and temporary files
echo "cleaning temp files/directories..."
rm exclude.tmp
rm exclude_src.tmp
rm -rf $PACKAGE_DIR
echo "packaging success"

exit 0

