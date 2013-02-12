#! /bin/bash

#=============================================================
# Script which takes the output of "ut-backup" and
# restores it to a system.  
#
# 1 - Not a valid gzip file
# 2 - Not a tar file
# 3 - Missing content from file
# 4 - Error from restore file
# 5 - Restore file too old
#
#==============================================================

IN_FILE=INVALID
VERBOSE=false
NOHUPPED=false

function debug() {
  if [ "true" == $VERBOSE ]; then
    echo $*
  fi
}

function err() {
  echo $* >> /dev/stderr
}

function doHelp() {
  echo "$0 -i (input bundle file) -h (help) -v (verbose)"
}

INST_OPTS=" -o DPkg::Options::=--force-confnew --yes --force-yes --fix-broken "
UPGD_OPTS=" -o DPkg::Options::=--force-confnew --yes --force-yes --fix-broken "

function restore_db()
{
    echo "restore_db()"
    infile=$1

    dropdb -U postgres uvm >/dev/null 2>&1
    createuser -U postgres -dSR untangle 2>/dev/null
    createdb -O postgres -U postgres uvm >/dev/null 2>&1

    ## If the database is open, just drop all of the schemas inside of it.
    psql -U postgres uvm -c "DROP SCHEMA settings CASCADE" >/dev/null 2>&1
    psql -U postgres uvm -c "DROP SCHEMA events CASCADE" >/dev/null 2>&1
    psql -U postgres uvm -c "DROP SCHEMA reports CASCADE" >/dev/null 2>&1

    # if confirm "Restore settings?"; then
    zcat $infile | psql -X -U postgres uvm
    
    ## Reset the events schema, it is no longer valid.
    psql -U postgres -c "UPDATE settings.split_schema_ver SET events_version = NULL;" uvm  >/dev/null 2>&1
    psql -U postgres -c "DROP SCHEMA IF EXISTS events CASCADE" uvm >/dev/null 2>&1 
    true
    
    # fi
}

function clean_dirs()
{
    rm -rf /usr/share/untangle/settings/*
    rm -rf /usr/share/untangle/conf/openvpn
    rm -rf /etc/openvpn
    rm -f  /usr/share/untangle/conf/dirbackup.ldif
}

function restore_packages()
{
    echo "restore_packages()"
    instfile=$1

    apt-get update 2>&1 
    apt-get dist-upgrade $UPGD_OPTS 2>&1 

    /etc/init.d/untangle-vm stop
    if [ -x /etc/init.d/untangle-reports ]; then
        /etc/init.d/untangle-reports stop
    fi

    # ignore kaspersky files *-kav*
    cat $instfile | grep -v "\-kav" | awk '{print $1}' | xargs apt-get install $INST_OPTS 2>&1 
}

function restore_files() {
    echo "restore_files()"

    dumpfile=$1
    tarfile=$2
    instfile=$3

    # stop the UVM, depending on circumstances may already be stopped
    /etc/init.d/untangle-vm stop
    if [ -x /etc/init.d/untangle-reports ]; then
        /etc/init.d/untangle-reports stop
    fi

    if [ -x /etc/init.d/apache2 ]; then
        /etc/init.d/apache2 stop
    fi

    # restore the database
    restore_db $dumpfile

    # clean out stuff that restore would otherwise append to
    clean_dirs

    # restore the files, both system and the /usr/share/untangle important stuf
    tar zxf $tarfile -C /

    # update the things also kept in kernel (bug 5533)
    hostname `cat /etc/hostname`

    # try to download needed files, this may fail because the network settings may not be correct
    restore_packages $instfile

    ## Qualified and unqualified hostname (hostname may not be set yet)
    host_string=`awk '{ hostname = $0 ; sub ( /\..*/, "", hostname ) ; print $0 " " hostname }' /etc/hostname`

    # try again just in case these network settings are "better" than the previous
    restore_packages $instfile

    # Restart apache
    /etc/init.d/apache2 restart

    # start the UVM, depending on circumstances (menu driven restore) may need to be restopped
    /etc/init.d/untangle-vm start > /dev/null 2>&1

    if [ -x /etc/init.d/untangle-reports ]; then
        /etc/init.d/untangle-reports start
    fi

    # Inform the UVM that the DB settings have changed, give it a chance to write out config files.
    /usr/share/untangle/bin/ut-pycli -c "uvmContext.syncConfigFiles()"

    return 0
}

function doRestore() {
    if [ "INVALID" == $IN_FILE ]; then
        err "Please provide an input file";
        return 1;
    fi

    debug "Restoring from file -" $IN_FILE

    # Create a working directory
    WORKING_DIR=`mktemp -d -t ut-restore.XXXXXXXXXX`
    debug "Working in directory $WORKING_DIR"

    # Copy our file to the working directory
    cp $IN_FILE $WORKING_DIR/x.tar.gz

    # Unzip
    gzip -t $WORKING_DIR/x.tar.gz
    EXIT_VAL=$?

    if [ $EXIT_VAL != 0 ]; then
        err "$IN_FILE Does not seem to be a valid gzip file"
        rm -rf $WORKING_DIR
        return 1
    fi

    debug "Gunzip"
    gunzip $WORKING_DIR/x.tar.gz

    # Now, untar
    pushd $WORKING_DIR > /dev/null 2>&1
    debug "Untar"
    tar -xvf x.tar  > /dev/null 2>&1
    EXIT_VAL=$?
    popd  > /dev/null 2>&1

    if [ $EXIT_VAL != 0 ]; then
        err "$IN_FILE Does not seem to be a valid gzip tar file"
        rm -rf $WORKING_DIR
        return 2
    fi

    # Find the specfic files
    pushd $WORKING_DIR > /dev/null 2>&1

    DB_FILE=`ls | grep uvmdb*.gz`
    FILES_FILE=`ls | grep files*.tar.gz`
    INSTALLED_FILE=`ls | grep installed*`
    OLD_DB_FILE=`ls | grep mvvmdb*.gz`

    debug "DB file $DB_FILE"
    debug "Files file $FILES_FILE"
    debug "Installed file $INSTALLED_FILE"

    popd  > /dev/null 2>&1

    # Check version
    if [ -n "$OLD_DB_FILE" ]; then
        err "Restore file too old"
        return 5
    fi

    # Verify files
    if [ -z "$INSTALLED_FILE" ]; then
        err "Unable to find installed packages file"
        rm -rf $WORKING_DIR
        return 3
    fi

    if [ -z "$FILES_FILE" ]; then
        err "Unable to find system files file"
        rm -rf $WORKING_DIR
        return 3
    fi

    if [ -z "$DB_FILE" ]; then
        err "Unable to find database file"
        rm -rf $WORKING_DIR
        return 3
    fi

    # Invoke restore_files ("Usage: $0 dumpfile tarfile instfile")
    restore_files $WORKING_DIR/$DB_FILE $WORKING_DIR/$FILES_FILE $WORKING_DIR/$INSTALLED_FILE

    EXIT_VAL=$?

    rm -rf $WORKING_DIR

    if [ $EXIT_VAL != 0 ]; then
        err "Error $EXIT_VAL returned from untangle-restore"
        return 4
    fi

    debug "Completed.  Success"
}

####################################
# "Main" logic starts here

while getopts "hi:vQ" opt; do
  case $opt in
    h) doHelp;exit 0;;
    i) IN_FILE=$OPTARG;;
    v) VERBOSE=true;;
    Q) NOHUPPED=true;;
  esac
done

## Execute these functions in a separate detached process, this way
## when uvm gets killed this process doesn't exit.
if [ $NOHUPPED != "true" ]; then
    ## Just append any arguments, they don't matter
    nohup bash @UVM_HOME@/bin/ut-restore.sh "$@" -Q > @PREFIX@/var/log/uvm/restore.log 2>&1 &
else
    doRestore
fi

