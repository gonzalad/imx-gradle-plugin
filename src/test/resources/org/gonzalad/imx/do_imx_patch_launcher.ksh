#!/bin/ksh

IMX_PATCH_HOME=$imxHome
CONF=$IMX_PATCH_HOME/patchs/imx_patch.conf
LOG_FILE=$IMX_PATCH_HOME/patchs/imx_patch.log
LOG_ERROR=false
LOG_INTERRUPTED=false

if (($# >= 2)); then
	if [ "$2" = true ]; then
		LOG_INTERRUPTED=true
	fi
fi
if (($# >= 1)); then
	if [ "$1" = true ]; then
		LOG_ERROR=true
	fi
fi

typeset -i i=0
while (( i < 10 )) 
do 
   
   echo `date` $i >> $LOG_FILE
   sleep 7
   (( i = i + 1 ))
done

cat >> $LOG_FILE << EOM

###################### 14/10/2015 - 09h57 ######################

MODE AUTOMATIQUE
Liste des patchs a installer :
DsPatch20151009_1.tar.Z
DmPatch20151009_2.tar.Z

Heure de debut : 14/10/2015 - 19h28
EOM

if [ "$LOG_INTERRUPTED" = false ]; then
cat >> $LOG_FILE << EOM
Bilan :
DdPatch20150907_1.tar.Z	DdPatch20150907_1	14/10/2015 - 19h29	OK
TmPatch20150907_5.tar.Z	TmPatch20150907_5	14/10/2015 - 19h30	OK
EOM
fi

if [ "$LOG_INTERRUPTED" = false -a "$LOG_ERROR" = true ]; then
	cat >> $LOG_FILE << EOM
DsPatch20150907_6.tar.Z	DsPatch20150907_6	14/10/2015 - 19h32	KO!!!
EOM
fi

rm -f $CONF
exit 1
