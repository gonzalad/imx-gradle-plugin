#!/bin/ksh

IMX_LOG_FILE=$imxHome/patchs/imx_patch.log

if [ ! -f $IMX_LOG_FILE ]
then
    touch $IMX_LOG_FILE
fi
tail -f -n0 $IMX_LOG_FILE &
log_id=$!

#wait start of install (launched with at now + 1 minute)
sleep 61

#wait until end of install
./watchdog.ksh --nolaunch --timelimit -1 "$watchedProcess"
rc=$?

#stop tailing log file
kill $log_id

if ((rc != 0))
then
    exit $rc
fi

#analyze log files to set exit code
#  tac : reverses lines in file, see http://www.theunixschool.com/2012/06/5-ways-to-reverse-order-of-file-content.html
tail -1000 $IMX_LOG_FILE | tac | awk '{ 
    if ($0 == "Bilan :") {
        # no error has been detected install was successfull
        exit 0
    }
    if ($6 == "KO!!!") {
        # error during patch install detected
        exit 166
    }
    if ($1 == "######################" && $5 == "######################") {
        # patch installation interruption detected
        exit 167
    }
}'
rc=$?
if ((rc == 0))
then
    echo "*** INSTALLATION PATCHS OK ***"
else
    echo "*** INSTALLATION PATCHS EN ERREUR ***"
fi
exit $rc
