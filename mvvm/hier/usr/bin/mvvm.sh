#! /bin/sh
# $Id$

# get a bunch of default values
source @PREFIX@/etc/default/mvvm

MVVM_CONSOLE_LOG=${MVVM_CONSOLE_LOG:-"@PREFIX@/var/log/mvvm/console.log"}
MVVM_MVVM_LOG=${MVVM_MVVM_LOG:-"@PREFIX@/var/log/mvvm/mvvm.log"}
MVVM_GC_LOG=${MVVM_GC_LOG:-"@PREFIX@/var/log/mvvm/gc.log"}
MVVM_WRAPPER_LOG=${MVVM_WRAPPER_LOG:-"@PREFIX@/var/log/mvvm/wrapper.log"}
MVVM_LAUNCH=${MVVM_LAUNCH:-"@PREFIX@/usr/bin/bunnicula"}

PGDATA=${POSTGRES_DATA:-/var/lib/postgres/data}

# Short enough to restart mvvm promptly
SLEEP_TIME=5

# Used to kill a child with extreme prejudice
nukeIt() {
    echo "mvvm.sh: Killing -9 all bunnicula \"$pid\" (`date`)" >> $MVVM_WRAPPER_LOG
    kill -3 $pid
    kill -9 $pid
    kill -9 `ps awwx | grep java | grep bunnicula | awk '{print $1}'` 2>/dev/null
}

reapChildHardest() {
    nukeIt
    flushIptables ; exit
}

reapChildHarder() {
    echo "mvvm.sh: Killing -15 all bunnicula \"$pid\" (`date`)" >> $MVVM_WRAPPER_LOG
    kill $pid
    sleep 1
    if [ ! -z "`ps awwx | grep java | grep bunnicula | awk '{print $1}'`" ] ; then
        echo "mvvm.sh: Killing -15 all bunnicula \"$pid\" (`date`)" >> $MVVM_WRAPPER_LOG
        for i in `seq 1 5` ; do
            if [ -z "`ps awwx | grep java | grep bunnicula | awk '{print $1}'`" ] ; then
                flushIptables ; exit
            fi
            sleep 1
        done
        if [ ! -z "`ps awwx | grep java | grep bunnicula | awk '{print $1}'`" ] ; then
            reapChildHardest
        fi
    fi
    flushIptables ; exit
}

reapChild() {
    echo "mvvm.sh: shutting down bunnicula " >> $MVVM_WRAPPER_LOG
    kill -3 $pid
    @PREFIX@/usr/bin/mcli -t 20000 shutdown &> /dev/null
    sleep 1
    kill -INT $pid
    if [ ! -z "`ps awwx | grep java | grep bunnicula | awk '{print $1}'`" ] ; then
        echo "mvvm.sh: Killing -INT all bunnicula \"$pid\" (`date`)" >> $MVVM_WRAPPER_LOG
        for i in `seq 1 5` ; do
            if [ -z "`ps awwx | grep java | grep bunnicula | awk '{print $1}'`" ] ; then
                flushIptables ; exit
            fi
            sleep 1
        done
        if [ ! -z "`ps awwx | grep java | grep bunnicula | awk '{print $1}'`" ] ; then
            reapChildHarder
        fi
    fi
    flushIptables ; exit
}


flushIptables() {
    /sbin/iptables -t nat -F
    /sbin/iptables -t mangle -F
    /sbin/iptables -t filter -F
    /sbin/iptables -t raw -F
}

raiseFdLimit() {
    ulimit -n 2000
    ulimit -n 4000
    ulimit -n 8000
    ulimit -n 16000
    ulimit -n 32000
    ulimit -n 64000
    ulimit -n 128000
    ulimit -n 256000
    ulimit -n 512000
    ulimit -n 1024000
}

isServiceRunning() {
  ps aux -w -w | grep -v "grep $1" | grep -q "$1"
}

restartServiceIfNeeded() {
  serviceName=$1

  needToRun=no

  case $serviceName in
    postgresql)
      pidFile=$PGDATE/postmaster.pid
      isServiceRunning postmaster && return
      needToRun=yes # always has to run
      ;;
    slapd)
      pidFile=/var/run/slapd/slapd.pid
      isServiceRunning slapd && return
      needToRun=yes # always has to run
      ;;
    spamassassin)
      pidFile=/var/run/spamd.pid
      isServiceRunning spamd && return
      confFile=/etc/default/spamassassin
      [ -f $confFile ] && grep -q ENABLED=1 $confFile && needToRun=yes
      ;;
    clamav-daemon)
      pidFile="/var/run/clamd/clamd.pid"
      isServiceRunning clamd && return
      dpkg -l clamav-daemon | grep -q -E '^ii' && needToRun=yes
      ;;
    clamav-freshclam)
      pidFile="/var/run/clamd/freshclam.pid"
      isServiceRunning freshclam && return
      dpkg -l clamav-freshclam | grep -q -E '^ii' && needToRun=yes
      ;;
  esac

  if [ $needToRun == "yes" ] ; then
    echo "*** restarting missing $serviceName on `date` ***" >> $MVVM_WRAPPER_LOG
    rm -f $pidFile
#    /etc/init.d/$serviceName stop
    /etc/init.d/$serviceName start
  fi
}

# Return true (0) when we need to reap and restart the mvvm.
needToRestart() {
    cheaphigh=`head -3 /proc/$pid/maps | tail -1 | awk '{ high=split($1, arr, "-"); print arr[2]; }'`
    if [ -z $cheaphigh ]; then
        # not fatal, process has probably just died, which we'll catch soon.
        echo "*** no heap size ($cheaphigh) on `date` in `pwd` ***" >> $MVVM_WRAPPER_LOG
    else
        bignibble=${cheaphigh:0:1}
        case $bignibble in
            0 | 1)
                # less than 384Meg native heap
                ;;
            2)
                # 384Meg < native heap < 640Meg
            if [ $MEM -lt 1000000 ] || [ `date +%H` -eq 1 ] ; then
                    echo "*** bunnicula heap soft limit on `date` in `pwd` ***" >> $MVVM_WRAPPER_LOG
                    return 0;
        fi
                ;;
            3 | 4 | 5 | 6 | 7 | 8 | 9)
                # native heap > 640Meg
                echo "*** bunnicula heap hard limit ($bignibble) on `date` in `pwd` ***" >> $MVVM_WRAPPER_LOG
                return 0;
                ;;
            *)
                echo "*** unexpected heap size ($bignibble) on `date` in `pwd` ***" >> $MVVM_WRAPPER_LOG
                ;;
        esac
    fi

    # gc failure (persistent heap full)
    cmfcount=`tail -50 $MVVM_GC_LOG | grep -ci "concurrent mode failure"`
    if [ $cmfcount -gt 2 ]; then
        echo "*** java heap cmf on `date` in `pwd` ***" >> $MVVM_WRAPPER_LOG
        return 0;
    fi

    # extra nightime checks
    if [ `date +%H` -eq 1 ]; then
        # VSZ greater than 1.1 gigs reboot
        VIRT="`cat /proc/$pid/status | grep VmSize | awk '{print $2}'`"
        if [ $VIRT -gt $MAX_VIRTUAL_SIZE ] ; then
            echo "*** Virt Size too high ($VIRT) on `date` in `pwd` ***" >> $MVVM_WRAPPER_LOG
            return 0;
        fi
    fi

    return 1;
}


trap reapChildHardest 6
trap reapChildHarder 15
trap reapChild 2

while true; do
    echo > $MVVM_CONSOLE_LOG
    echo "============================" >> $MVVM_CONSOLE_LOG
    echo $MVVM_LAUNCH >> $MVVM_CONSOLE_LOG
    echo "============================" >> $MVVM_CONSOLE_LOG

    echo >> $MVVM_WRAPPER_LOG
    echo "============================" >> $MVVM_WRAPPER_LOG
    echo $MVVM_LAUNCH >> $MVVM_WRAPPER_LOG
    echo "============================" >> $MVVM_WRAPPER_LOG

    raiseFdLimit
    flushIptables

    $MVVM_LAUNCH $* >>$MVVM_CONSOLE_LOG 2>&1 &

    pid=$!
    echo "Bunnicula launched. (pid:$pid) (`date`)" >> $MVVM_WRAPPER_LOG

# Instead of waiting, we now monitor.
    while true; do
        sleep $SLEEP_TIME
        if [ ! -d /proc/$pid ] ; then
            echo "*** restarting missing bunnicula $? on `date` ***" >> $MVVM_WRAPPER_LOG
            break
        fi
        if needToRestart; then
            echo "*** need to restart bunnicula $? on `date` ***" >> $MVVM_WRAPPER_LOG
            nukeIt
            break
        fi
	restartServiceIfNeeded postgresql
	restartServiceIfNeeded clamav-freshclam
	restartServiceIfNeeded clamav-daemon
	restartServiceIfNeeded spamassassin
	restartServiceIfNeeded slapd
    done

# Clean up the zombie.  Risky? XXX
#    wait $pid

# Crash/Kill
    flushIptables
    echo "*** bunnicula exited on `date` in `pwd` ***" >> $MVVM_WRAPPER_LOG
    echo "*** copied $MVVM_CONSOLE_LOG to $MVVM_CONSOLE_LOG.crash ***" >> $MVVM_WRAPPER_LOG
    echo "*** copied $MVVM_MVVM_LOG to $MVVM_MVVM_LOG.crash ***" >> $MVVM_WRAPPER_LOG
    echo "*** copied $MVVM_GC_LOG to $MVVM_GC_LOG.crash ***" >> $MVVM_WRAPPER_LOG
    cp -f $MVVM_CONSOLE_LOG.crash.1 $MVVM_CONSOLE_LOG.crash.2
    cp -f $MVVM_CONSOLE_LOG.crash $MVVM_CONSOLE_LOG.crash.1
    cp -f $MVVM_CONSOLE_LOG $MVVM_CONSOLE_LOG.crash
    cp -f $MVVM_MVVM_LOG.crash.1 $MVVM_MVVM_LOG.crash.2
    cp -f $MVVM_MVVM_LOG.crash $MVVM_MVVM_LOG.crash.1
    cp -f $MVVM_MVVM_LOG $MVVM_MVVM_LOG.crash
    cp -f $MVVM_GC_LOG.crash.1 $MVVM_GC_LOG.crash.2
    cp -f $MVVM_GC_LOG.crash $MVVM_GC_LOG.crash.1
    cp -f $MVVM_GC_LOG $MVVM_GC_LOG.crash

    sleep 2
    echo "*** restarting on `date` ***" >> $MVVM_WRAPPER_LOG
done
