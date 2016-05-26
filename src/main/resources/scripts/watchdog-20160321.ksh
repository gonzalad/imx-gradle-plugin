#!/bin/ksh

#usage watchdog.ksh [--nolaunch] [--timelimit <timelimitseconds>] <watched-process>
#by default timelimitseconds = 60 seconds
#use -1 to wait indefinitely 

#
# Checks that watched-process execution time doesn't exceeds timelimit.
# called only if timelimit > 0
#
watchdog()
{
    sleep 1; # wait for the worker to start
    while [ $timelimit -gt 0 ]
    do
        # pgrep is available since 5.8, else use ps -ef | grep -v grep | grep -v watchdog.ksh | grep $worker_name
        #jobid=`pgrep $worker_name`
        jobid=`ps -ef | grep -v grep | grep -v watchdog.ksh | grep "$worker_name" | awk '{print $2}'`

        if [ $? -eq 1 ]; then
            break
        else
            sleep 1
        fi
        ((timelimit-=1))
    done
    if [ $timelimit -eq 0 ]
    then
        # kill worker + child processes
        ptree $jobid | awk '$1=='$jobid'{start=1}start==1{print $1}' | while read pid
            do
                kill -TERM "$pid" > /dev/null 2>&1
            done
    fi
}

printUsage()
{
    echo "usage watchdog.ksh [--nolaunch] [--timelimit <timelimitseconds>] <watched-process>"
		echo "    This script waits for watched-process to end."
		echo "    It kills the process if its execution time exceeeds timelimitseconds."
		echo "    Optionnaly, the script can also start the process."
		echo "    watched-process: process to watch (its execution will be watched with 'ps' executable)"
		echo "    nolaunch: watched-process isn't executed by watchdog"
		echo "    timelimitseconds: by default timelimitseconds = 60 seconds"
		echo "                      use -1 to wait indefinitely" 
}

if (($# == 0))
then
    echo "watchdog.ksh - Error : argument required"
		printUsage()
    rc=1
fi

#export PATH=/usr/bin:/usr/sbin:/bin

#
# default time limit is 60 seconds
#
timelimit=60
#
# by default process is launched
#
nolaunch=false

#handle command-line parameters see http://stackoverflow.com/questions/192249/how-do-i-parse-command-line-arguments-in-bash
while (( $# >= 1 ))
do
key="$1"

case $key in
    -t|--timelimit)
    timelimit="$2"
    shift # past argument
    ;;
    -nl|--nolaunch)
    nolaunch=true
    ;;
    *)
    worker="$1" # watched-process
    ;;
esac
shift # past argument or value
done

echo "worker=$worker, nolaunch=$nolaunch, timelimit=$timelimit"

if [ -z "$worker" ]; then
    echo "Error first argument mandatory (worker)"
    exit 3
fi

#worker="${0%/*}/${1}"
worker_name=${worker##*/}
#worker_name=${worker_name%.*}
if [ "$nolaunch" = false ]
then
	if [ ! -f $worker ]
    then
	    echo "Error. \"$worker\" cannot be found"
	    exit 1
	fi
	if [ ! -x $worker ]
    then
	    echo "Error. \"$worker\" is not executable"
	    exit 2
	fi
fi

#
# start the watchdog before the worker
#
if (( timelimit >= 0 ))
then
    watchdog &
fi


if [ "$nolaunch" = false ]
then
    tmpfile="/tmp/.$work_name.$$"
    $worker > $tmpfile 2>&1 &
    worker_id=$!
else
    worker_id=`ps -ef | grep -v grep | grep -v watchdog.ksh | grep "$worker_name" | awk '{print $2}'`
fi

echo "waiting $worker_id - $worker_name"

if [ -z "$worker_id" ]
then
    echo "cannot detect process $worker_id - $worker_name, it didn't started ?"
    rc=1
else
    #wait ne fonctionne pas si le process est lance par at (msg erreur : bash: wait: pid 8554 is not a child of this shell)
    #wait $worker_id > /dev/null 2>&1

    while [ -e /proc/$worker_id ]; do sleep 5; done
    rc=$?
fi

#just to be sure to have last logs (tail -f)
sleep 1


if [ $rc -ne 0 ]
then
    # replace this line to do whatever you want, send email, sms, logger....
    #
    # echo .... | mailx someone@somewhere.com
    
    if [ "$nolaunch" = false ]
    then
        details=`cat $tmpfile 2>/dev/null`
        echo "Exit status=$rc. Error waiting for $worker_id - $worker_name - $details"
        rm -f $tmpfile
    else
        echo "Exit status=$rc. Error waiting for $worker_id - $worker_name"
    fi
fi

exit $rc
